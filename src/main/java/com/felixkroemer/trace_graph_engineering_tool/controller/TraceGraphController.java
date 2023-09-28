package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.display_manager.Trace;
import com.felixkroemer.trace_graph_engineering_tool.display_manager.TracesDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.table.CyTableViewManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class TraceGraphController {

    private final Logger logger;

    private final CyServiceRegistrar registrar;
    private final TraceGraph traceGraph;
    private final RenderingController renderingController;
    private final TraceDetailsController traceDetailsController;

    public TraceGraphController(CyServiceRegistrar registrar, TraceGraph traceGraph) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.registrar = registrar;
        this.traceGraph = traceGraph;
        this.renderingController = new RenderingController(registrar, traceGraph);
        this.traceDetailsController = new TraceDetailsController(registrar);
        registrar.registerService(this.renderingController, SelectedNodesAndEdgesListener.class);
    }

    public void registerNetwork() {
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(traceGraph.getNetwork());
        var networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(renderingController.getView());
        this.traceGraph.getPDM().forEach(p -> p.addObserver(this.renderingController));
        this.hideUnneededColumns();
        renderingController.applyWorkingLayout();
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

    public void setMode(RenderingMode mode) {
        renderingController.setMode(mode);
    }

    public TraceGraph getTraceGraph() {
        return this.traceGraph;
    }

    public void unregister() {
        registrar.unregisterService(renderingController, SelectedNodesAndEdgesListener.class);
    }

    public void showTraceDetails(CyIdentifiable identifiable) {
        boolean isEdge = identifiable instanceof CyEdge;
        Set<Trace> traces = TracesDisplayController.getTraces(identifiable, this.traceGraph, 2, isEdge);
        this.traceDetailsController.showTraces(traces);
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

    public NetworkType getNetworkType(CyNetwork network) {
        if (network == this.traceGraph.getNetwork()) {
            return NetworkType.DEFAULT;
        } else if (network == this.traceDetailsController.getNetwork()) {
            return NetworkType.TRACE_DETAILS;
        } else {
            throw new IllegalArgumentException("Network does not belong to this trace graph");
        }
    }

    public void destroy() {
        //network destroyed handler will delete it from this.traceGraphs
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.destroyNetwork(this.traceGraph.getNetwork());
        var traceDetailsNetwork = this.traceDetailsController.getNetwork();
        if (traceDetailsNetwork != null) {
            networkManager.destroyNetwork(traceDetailsNetwork);
        }
    }

    public void highlightTrace(List<CyNode> sequence) {

    }
}
