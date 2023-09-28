package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;

public class DefaultDisplayController extends AbstractDisplayController {

    public DefaultDisplayController(CyNetworkView view, TraceGraph traceGraph) {
        super(view, traceGraph);
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
    }

    @Override
    public void enable() {
        this.deselectAll();
        this.showALlEdges();
    }
}
