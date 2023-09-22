package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;

import java.util.ArrayList;
import java.util.List;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class SelectedDisplayManager extends AbstractDisplayManager {

    private CyNode previousNode;

    public SelectedDisplayManager(CyNetworkView view, TraceGraph traceGraph) {
        super(view, traceGraph);
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.getSelectedEdges().size() == 1 && this.previousNode != null) {
            CyEdge edge = event.getSelectedEdges().iterator().next();
            CyNode neighbor = edge.getTarget() == previousNode ? edge.getSource() : edge.getTarget();
            traceGraph.getNetwork().getRow(edge).set(CyNetwork.SELECTED, false);
            traceGraph.getNetwork().getRow(neighbor).set(CyNetwork.SELECTED, true);
            networkView.setVisualProperty(NETWORK_CENTER_X_LOCATION,
                    networkView.getNodeView(neighbor).getVisualProperty(NODE_X_LOCATION));
            networkView.setVisualProperty(NETWORK_CENTER_Y_LOCATION,
                    networkView.getNodeView(neighbor).getVisualProperty(NODE_Y_LOCATION));
        } else {
            if (event.getSelectedNodes().size() == 1) {
                this.previousNode = event.getSelectedNodes().iterator().next();
            }
            showEdgesOfHighlightedNodes();
        }
    }

    @Override
    public void enable() {
        for (var nodeView : networkView.getNodeViews()) {
            networkView.getModel().getRow(nodeView.getModel()).set(CyNetwork.SELECTED, false);
        }
        showEdgesOfHighlightedNodes();
    }

    private void showEdgesOfHighlightedNodes() {
        this.hideAllEdges();
        CyNetwork network = networkView.getModel();
        List<CyEdge> adjacentEdges = new ArrayList<>();
        CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true).forEach(n -> adjacentEdges.addAll(network.getAdjacentEdgeList(n, CyEdge.Type.DIRECTED)));
        adjacentEdges.forEach(e -> {
            //TODO: if a node is selected, the adjacent edges will still be drawn at 1.0 width
            networkView.getEdgeView(e).setVisualProperty(EDGE_WIDTH, 5.0);
            networkView.getEdgeView(e).setVisualProperty(EDGE_VISIBLE, true);
        });
    }
}
