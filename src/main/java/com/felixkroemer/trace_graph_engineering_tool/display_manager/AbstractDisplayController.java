package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_VISIBLE;

public abstract class AbstractDisplayController {

    protected CyNetworkView networkView;
    protected TraceGraph traceGraph;

    // assumes the network has the default style applied (besides EDGE_VISIBLE / NODE_VISIBLE)
    public AbstractDisplayController(CyNetworkView view, TraceGraph traceGraph) {
        this.networkView = view;
        this.traceGraph = traceGraph;
        this.enable();
    }

    public abstract void handleNodesSelected(SelectedNodesAndEdgesEvent event);

    public abstract void enable();

    protected void showALlEdges() {
        for (var edgeView : networkView.getEdgeViews()) {
            edgeView.setVisualProperty(EDGE_VISIBLE, true);
        }
    }

    protected void hideAllEdges() {
        for (var edgeView : networkView.getEdgeViews()) {
            edgeView.setVisualProperty(EDGE_VISIBLE, false);
        }
    }

    public CyNetworkView getNetworkView() {
        return this.networkView;
    }

}
