package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMapping;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import com.felixkroemer.trace_graph_engineering_tool.util.TaskMonitorStub;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.*;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class TraceGraphController implements NetworkAboutToBeDestroyedListener {

    private final Logger logger;

    private List<TraceGraph> traceGraphs;
    private CyServiceRegistrar registrar;
    private CyNetworkManager networkManager;
    private CyNetworkViewFactory networkViewFactory;
    private CyNetworkViewManager networkViewManager;
    private CyLayoutAlgorithmManager layoutManager;
    private TaskManager taskManager;
    private CyApplicationManager applicationManager;
    private VisualMappingManager visualMappingManager;
    private VisualStyleFactory visualStyleFactory;
    private VisualMappingFunctionFactory visualMappingFunctionFactory;

    private TraceGraphPanel panel;

    public TraceGraphController(CyServiceRegistrar registrar, TraceGraphPanel panel) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.panel = panel;

        this.traceGraphs = new LinkedList<>();
        this.registrar = registrar;
        this.networkManager = registrar.getService(CyNetworkManager.class);
        this.networkViewFactory = registrar.getService(CyNetworkViewFactory.class);
        this.networkViewManager = registrar.getService(CyNetworkViewManager.class);
        this.layoutManager = registrar.getService(CyLayoutAlgorithmManager.class);
        this.taskManager = registrar.getService(TaskManager.class);
        this.applicationManager = registrar.getService(CyApplicationManager.class);
        this.visualMappingManager = registrar.getService(VisualMappingManager.class);
        this.visualStyleFactory = registrar.getService(VisualStyleFactory.class);
        // Ensure we get org.cytoscape.view.vizmap.internal.mappings.PassthroughMappingFactory, then cast to
        // PassthroughMapping
        this.visualMappingFunctionFactory = registrar.getService(VisualMappingFunctionFactory.class, "(mapping" +
                ".type=continuous)");
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
        for (CyLayoutAlgorithm l : this.layoutManager.getAllLayouts()) {
            logger.info(l.getName());
        }

        VisualStyle style = visualStyleFactory.createVisualStyle("default");

        VisualMappingFunction<Integer, Double> sizeMapping = Mappings.createSizeMapping(1, 2000,
                visualMappingFunctionFactory);
        VisualMappingFunction<Integer, Paint> colorMapping = Mappings.createColorMapping(1, 1600,
                visualMappingFunctionFactory);
        TooltipMapping tooltipMapping = new TooltipMapping(tg.getPDM());

        style.addVisualMappingFunction(sizeMapping);
        style.addVisualMappingFunction(colorMapping);

        // TODO: find problem when creating non-tg network and switching back and forth
        //style.addVisualMappingFunction(tooltipMapping);
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
        int index = -1;
        for (int i = 0; i < traceGraphs.size(); i++) {
            if (e.getNetwork() == traceGraphs.get(i).getNetwork()) ;
            index = i;
            break;
        }
        if (index != -1) {
            this.traceGraphs.remove(index);
        }
        if (this.traceGraphs.isEmpty()) {
            this.hidePanel();
        }
    }

}
