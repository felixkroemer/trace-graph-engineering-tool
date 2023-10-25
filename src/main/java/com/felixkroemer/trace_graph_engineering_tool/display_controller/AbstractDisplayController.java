package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public abstract class AbstractDisplayController {

    protected CyServiceRegistrar registrar;
    protected CyNetworkView networkView;
    protected TraceGraph traceGraph;

    // assumes the network has the default style applied (besides EDGE_VISIBLE / NODE_VISIBLE)
    public AbstractDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph) {
        this.registrar = registrar;
        this.networkView = view;
        this.traceGraph = traceGraph;
    }

    public abstract void handleNodesSelected(SelectedNodesAndEdgesEvent event);

    public abstract void init();

    /*
     * clear potential value locks, unregister listeners, etc.
     */
    public abstract void disable();

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

    protected CyNode findFartherNode(CyNode source, CyNode target) {
        var sourceX = networkView.getNodeView(source).getVisualProperty(NODE_X_LOCATION);
        var sourceY = networkView.getNodeView(source).getVisualProperty(NODE_Y_LOCATION);
        var targetX = networkView.getNodeView(target).getVisualProperty(NODE_X_LOCATION);
        var targetY = networkView.getNodeView(target).getVisualProperty(NODE_Y_LOCATION);
        var centerX = networkView.getVisualProperty(NETWORK_CENTER_X_LOCATION);
        var centerY = networkView.getVisualProperty(NETWORK_CENTER_Y_LOCATION);

        var distSource = Math.sqrt(Math.pow(sourceX - centerX, 2) + Math.pow(sourceY - centerY, 2));
        var distTarget = Math.sqrt(Math.pow(targetX - centerX, 2) + Math.pow(targetY - centerY, 2));

        return distSource < distTarget ? target : source;
    }

    public CyNetworkView getNetworkView() {
        return this.networkView;
    }

    public abstract String getID();

}
