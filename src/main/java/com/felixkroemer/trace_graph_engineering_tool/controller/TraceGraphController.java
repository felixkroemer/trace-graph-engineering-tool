package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.UIState;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.table.CyTableViewManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class TraceGraphController {

    public static final String NETWORK_TYPE_DEFAULT = "NETWORK_TYPE_DEFAULT";
    public static final String NETWORK_TYPE_TRACE_DETAILS = "NETWORK_TYPE_TRACE_DETAILS";

    private final Logger logger;

    private final CyServiceRegistrar registrar;
    private final TraceGraph traceGraph;
    private final UIState uiState;
    private final RenderingController renderingController;
    private final TraceDetailsController traceDetailsController;

    public TraceGraphController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.registrar = registrar;
        this.traceGraph = traceGraph;
        this.uiState = new UIState(traceGraph);
        this.renderingController = new RenderingController(registrar, traceGraph, uiState);
        this.traceDetailsController = new TraceDetailsController(registrar, this.traceGraph, this.uiState);
        registrar.registerService(this.renderingController, SelectedNodesAndEdgesListener.class);
    }

    public void registerNetwork() {
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(traceGraph.getNetwork());
        var networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(renderingController.getView());
        this.hideUnneededColumns();
        renderingController.applyWorkingLayout();
    }

    public void initNetwork() {
        this.traceGraph.initNetwork();
        CyEventHelper helper = registrar.getService(CyEventHelper.class);
        helper.flushPayloadEvents();
        this.renderingController.applyDefaultStyle();
        this.renderingController.applyWorkingLayout();
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

    public UIState getUiState() {
        return this.uiState;
    }

    public void unregister() {
        registrar.unregisterService(renderingController, SelectedNodesAndEdgesListener.class);
    }

    public void showTraceDetails() {
        var manager = this.registrar.getService(CyApplicationManager.class);
        manager.setCurrentNetwork(this.traceDetailsController.getNetwork());
        this.traceDetailsController.update();
    }

    public void showDefaultView(CyNode node) {
        var manager = this.registrar.getService(CyApplicationManager.class);
        manager.setCurrentNetwork(this.traceGraph.getNetwork());
        if (node != null) {
            var defaultNetworkNode = this.traceDetailsController.findCorrespondingNode(node);
            this.renderingController.focusNode(defaultNetworkNode);
        }
    }

    public boolean containsNetwork(CyNetwork network) {
        return this.traceGraph.getNetwork() == network || this.traceDetailsController.getNetwork() == network;
    }

    public String getNetworkType(CyNetwork network) {
        if (network == this.traceGraph.getNetwork()) {
            return NETWORK_TYPE_DEFAULT;
        } else if (network == this.traceDetailsController.getNetwork()) {
            return NETWORK_TYPE_TRACE_DETAILS;
        } else {
            throw new IllegalArgumentException("Network does not belong to this trace graph");
        }
    }

    public void destroy() {
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.destroyNetwork(this.traceGraph.getNetwork());
        var traceDetailsNetwork = this.traceDetailsController.getNetwork();
        if (traceDetailsNetwork != null) {
            networkManager.destroyNetwork(traceDetailsNetwork);
        }
    }

    public SelectBinsController createSelectBinsController(Parameter parameter) {
        return new SelectBinsController(parameter, this.uiState, this.traceGraph.getSourceTable());
    }
}
