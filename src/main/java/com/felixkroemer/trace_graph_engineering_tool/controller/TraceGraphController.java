package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentTraceGraphControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.table.CyTableViewManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class TraceGraphController extends NetworkController implements SetCurrentNetworkListener {

    public static final String NETWORK_TYPE_DEFAULT = "NETWORK_TYPE_DEFAULT";
    public static final String NETWORK_TYPE_TRACE_DETAILS = "NETWORK_TYPE_TRACE_DETAILS";

    private final TraceGraph traceGraph;
    private final RenderingController renderingController;

    public TraceGraphController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        super(registrar, traceGraph.getNetwork());
        this.traceGraph = traceGraph;
        this.renderingController = new RenderingController(registrar, traceGraph);

        this.registrar.registerService(this, SetCurrentNetworkListener.class, new Properties());
    }

    @Override
    public CyNetworkView getView() {
        return this.renderingController.getView();
    }

    @Override
    public VisualStyle getVisualStyle() {
        return this.renderingController.getVisualStyle();
    }

    @Override
    public void updateNetwork(Parameter changedParameter) {
        var iterator = new TaskIterator();
        iterator.append(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                traceGraph.reinit(changedParameter);
                CyEventHelper helper = registrar.getService(CyEventHelper.class);
                helper.flushPayloadEvents();
            }
        });
        var taskManager = this.registrar.getService(SynchronousTaskManager.class);
        taskManager.execute(iterator);
    }

    @Override
    public Map<String, String> getNodeInfo(CyNode node) {
        return this.traceGraph.getNodeInfo(node);
    }

    private void hideUnneededColumns() {
        var tableViewManager = registrar.getService(CyTableViewManager.class);
        var nodeTableView = tableViewManager.getTableView(this.traceGraph.getNetwork().getDefaultNodeTable());
        var columnViews = nodeTableView.getColumnViews();
        Set<String> parameterNames = new HashSet<>();
        this.traceGraph.getPDM().forEach(p -> parameterNames.add(p.getName()));
        for (View<CyColumn> columnView : columnViews) {
            if (!parameterNames.contains(columnView.getModel().getName())) {
                columnView.setVisualProperty(COLUMN_VISIBLE, false);
            }
        }
    }

    public void setMode(String mode) {
        renderingController.setMode(mode);
    }

    public TraceGraph getTraceGraph() {
        return this.traceGraph;
    }

    @Override
    public void destroy() {
        this.renderingController.destroy();
        this.registrar.unregisterService(this, SetCurrentNetworkListener.class);
    }

    public void focusNode(CyNode node) {
        this.renderingController.focusNode(node);
    }

    public SelectBinsController createSelectBinsController(Parameter parameter) {
        //TODO: support multiple traces per tg
        return new SelectBinsController(parameter, this.traceGraph.getSourceTables().iterator().next());
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() == this.getNetwork()) {
            var eventHelper = this.registrar.getService(CyEventHelper.class);
            eventHelper.fireEvent(new SetCurrentTraceGraphControllerEvent(this, this));
        }
    }
}
