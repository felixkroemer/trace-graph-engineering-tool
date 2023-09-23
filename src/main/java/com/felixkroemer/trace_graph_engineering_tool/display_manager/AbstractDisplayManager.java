package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_VISIBLE;

public abstract class AbstractDisplayManager {

    protected CyNetworkView networkView;
    protected TraceGraph traceGraph;

    // assumes the network has the default style applied (besides EDGE_VISIBLE / NODE_VISIBLE)
    public AbstractDisplayManager(CyNetworkView view, TraceGraph traceGraph) {
        this.networkView = view;
        this.traceGraph = traceGraph;
        this.enable();
    }

    public abstract void handleNodesSelected(SelectedNodesAndEdgesEvent event);

    public abstract void enable();

    protected void hideAllEdges() {
        for (var edgeView : networkView.getEdgeViews()) {
            networkView.getModel().getRow(edgeView.getModel()).set(CyNetwork.SELECTED, false);
            edgeView.setVisualProperty(EDGE_VISIBLE, false);
        }
    }

    public CyNetworkView getNetworkView() {
        return this.networkView;
    }

}
