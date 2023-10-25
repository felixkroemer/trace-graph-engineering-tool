package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;

public class DefaultDisplayController extends AbstractDisplayController {

    public static final String RENDERING_MODE_FULL = "RENDERING_MODE_FULL";

    public DefaultDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph) {
        super(registrar, view, traceGraph);
        this.showALlEdges();
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
    }

    @Override
    public void init() {
    }

    @Override
    public void disable() {
    }

    @Override
    public String getID() {
        return RENDERING_MODE_FULL;
    }
}
