package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import com.felixkroemer.trace_graph_engineering_tool.util.TaskMonitorStub;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.work.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

public class TraceGraphController implements NetworkAboutToBeDestroyedListener, SetCurrentNetworkListener,
        PropertyChangeListener {

    private final Logger logger;

    private List<TraceGraph> traceGraphs;
    private TraceGraph currentNetwork;
    private CyServiceRegistrar registrar;

    private CyNetworkManager networkManager;
    private CyNetworkFactory networkFactory;
    private CyNetworkViewFactory networkViewFactory;
    private CyNetworkViewManager networkViewManager;
    private CyLayoutAlgorithmManager layoutManager;
    private CyApplicationManager applicationManager;
    private VisualMappingManager visualMappingManager;
    private VisualStyleFactory visualStyleFactory;


    private TraceGraphPanel panel;

    public TraceGraphController(CyServiceRegistrar registrar, TraceGraphPanel panel) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.panel = panel;
        this.traceGraphs = new LinkedList<>();
        this.registrar = registrar;

        this.networkManager = registrar.getService(CyNetworkManager.class);
        this.networkFactory = registrar.getService(CyNetworkFactory.class);
        this.networkViewFactory = registrar.getService(CyNetworkViewFactory.class);
        this.networkViewManager = registrar.getService(CyNetworkViewManager.class);
        this.layoutManager = registrar.getService(CyLayoutAlgorithmManager.class);
        this.applicationManager = registrar.getService(CyApplicationManager.class);
        this.visualMappingManager = registrar.getService(VisualMappingManager.class);
        this.visualStyleFactory = registrar.getService(VisualStyleFactory.class);

        this.showPanel();
    }

    //TODO split up
    public void registerTraceGraph(TraceGraph tg) {
        traceGraphs.add(tg);
        this.showPanel();

        CyNetworkView view = networkViewFactory.createNetworkView(tg.getNetwork());

        CyLayoutAlgorithm layoutFactory = layoutManager.getLayout("grid");
        Object context = layoutFactory.getDefaultLayoutContext();
        TaskIterator iterator = layoutFactory.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
        Task task = null;
        // do not use taskManager.execute to hide network until initial layout is applied
        try {
            while (iterator.hasNext()) {
                iterator.next().run(new TaskMonitorStub());
            }
        } catch (Exception e) {
            logger.error("Error applying layout");
        }

        networkManager.addNetwork(tg.getNetwork());
        networkViewManager.addNetworkView(view);

        VisualStyle style = visualStyleFactory.createVisualStyle("default");

        // Ensure we get org.cytoscape.view.vizmap.internal.mappings.PassthroughMappingFactory, then cast to
        // PassthroughMapping
        VisualMappingFunctionFactory visualMappingFunctionFactory =
                registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
        VisualMappingFunctionFactory tooltipMappingFunctionFactory =
                registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=tooltip)");

        VisualMappingFunction<Integer, Double> sizeMapping = Mappings.createSizeMapping(1, 2000,
                visualMappingFunctionFactory);
        VisualMappingFunction<Integer, Paint> colorMapping = Mappings.createColorMapping(1, 1600,
                visualMappingFunctionFactory);
        VisualMappingFunction<CyRow, String> tooltipMapping =
                Mappings.createTooltipMapping(tooltipMappingFunctionFactory);

        style.addVisualMappingFunction(sizeMapping);
        style.addVisualMappingFunction(colorMapping);
        style.addVisualMappingFunction(tooltipMapping);

        this.visualMappingManager.setCurrentVisualStyle(style);

        style.apply(view);
    }

    private void showPanel() {
        CySwingApplication swingApplication = registrar.getService(CySwingApplication.class);
        if (swingApplication.getCytoPanel(CytoPanelName.WEST).indexOfComponent("TraceGraphPanel") < 0) {
            this.registrar.registerService(this.panel, CytoPanelComponent.class);
        }
    }

    private void hidePanel() {
        this.registrar.unregisterService(this.panel, CytoPanelComponent.class);
    }

    @Override
    public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
        // TODO: find way to refer from CyNetwork to TraceGraph
        TraceGraph tr = findTraceGraphForNetwork(e.getNetwork());
        if (tr != null) {
            this.traceGraphs.remove(tr);
        }
        if (this.traceGraphs.isEmpty()) {
            this.hidePanel();
        }
    }

    private TraceGraph findTraceGraphForNetwork(CyNetwork network) {
        int index = -1;
        for (int i = 0; i < traceGraphs.size(); i++) {
            if (network == traceGraphs.get(i).getNetwork()) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            return traceGraphs.get(index);
        } else {
            return null;
        }
    }

    public TraceGraph getActiveTraceGraph() {
        return findTraceGraphForNetwork(applicationManager.getCurrentNetwork());
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (this.currentNetwork != null) {
            this.currentNetwork.getPDM().forEach(Parameter::clearObservers);
        }
        if (e.getNetwork() != null && Util.isTraceGraphNetwork(e.getNetwork())) {
            this.currentNetwork = this.findTraceGraphForNetwork(e.getNetwork());
            this.currentNetwork.getPDM().forEach(p -> p.addObserver(this));
            this.panel.registerCallbacks(this.currentNetwork);
        } else {
            this.currentNetwork = null;
            this.panel.clear();
        }
    }

    public void applyWorkingLayout() {
        CyNetworkView view = applicationManager.getCurrentNetworkView();
        CyLayoutAlgorithm layoutFactory = layoutManager.getLayout("grid");
        Object context = layoutFactory.getDefaultLayoutContext();
        var taskIterator = layoutFactory.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
        TaskManager<?, ?> manager = registrar.getService(TaskManager.class);
        manager.execute(taskIterator);
    }

    public void onBinsChanged(Parameter param, List<Double> bins) {
        param.setBins(bins);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "bins":
            case "enabled":
                TaskIterator iterator = new TaskIterator(new AbstractTask() {
                    @Override
                    public void run(TaskMonitor taskMonitor) throws Exception {
                        currentNetwork.clearNetwork();
                        currentNetwork.reinitNetwork();
                    }
                });
                // throws weird error if run asynchronously
                // runs on awt event thread
                // TODO: check if running async is possible
                var taskManager = registrar.getService(SynchronousTaskManager.class);
                taskManager.execute(iterator);
                applyWorkingLayout();
                break;
        }
    }

    public void clearTraceGraphs() {
        for (var tg : this.traceGraphs) {
            //network destroyed handler will delete it from this.traceGraphs
            networkManager.destroyNetwork(tg.getNetwork());
        }
    }
}
