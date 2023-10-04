package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.LabelBackgroundShapeVisualProperty;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class FollowDisplayController extends AbstractDisplayController {

    private CyNode previousNode;

    public FollowDisplayController(CyNetworkView view, TraceGraph traceGraph) {
        super(view, traceGraph);
        showEdgesOfHighlightedNodes();
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
    public void disable() {
    }

    private void showEdgesOfHighlightedNodes() {
        this.hideAllEdges();
        CyNetwork network = networkView.getModel();
        var selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        for (var node : selectedNodes) {
            var adjacentEdges = network.getAdjacentEdgeList(node, CyEdge.Type.DIRECTED);
            for (var edge : adjacentEdges) {
                StringBuilder sb = new StringBuilder();
                var nodeTable = this.networkView.getModel().getDefaultNodeTable();
                for (int i = 0; i < this.traceGraph.getPDM().getParameterCount(); i++) {
                    Parameter param = this.traceGraph.getPDM().getParameters().get(i);
                    var sourceValue = nodeTable.getRow(edge.getSource().getSUID()).get(param.getName(), Integer.class);
                    var targetValue = nodeTable.getRow(edge.getTarget().getSUID()).get(param.getName(), Integer.class);
                    if (sourceValue != targetValue) {
                        sb.append(param.getName()).append(" : ").append(sourceValue - targetValue).append(", ");
                    }
                }
                /*                sb.deleteCharAt(sb.length() - 1);*/
                var edgeView = networkView.getEdgeView(edge);
                edgeView.setVisualProperty(EDGE_WIDTH, 5.0);
                edgeView.setVisualProperty(EDGE_VISIBLE, true);
                edgeView.setVisualProperty(EDGE_LABEL, sb.toString());
                edgeView.setVisualProperty(EDGE_LABEL_BACKGROUND_SHAPE,
                        LabelBackgroundShapeVisualProperty.ROUND_RECTANGLE);
                edgeView.setVisualProperty(EDGE_LABEL_BACKGROUND_TRANSPARENCY, 127);
                edgeView.setVisualProperty(EDGE_LABEL_AUTOROTATE, true);
                edgeView.setVisualProperty(EDGE_LABEL_FONT_SIZE, 5.0);
            }
        }
    }
}