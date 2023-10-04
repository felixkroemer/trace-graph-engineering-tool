package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.Trace;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class ShortestTraceDisplayController extends AbstractDisplayController {

    private CyNode previousNode;

    public ShortestTraceDisplayController(CyNetworkView view, TraceGraph traceGraph) {
        super(view, traceGraph);
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
        if (event.getSelectedEdges().size() == 1) {
            CyEdge edge = event.getSelectedEdges().iterator().next();
            traceGraph.getNetwork().getRow(edge).set(CyNetwork.SELECTED, false);

            var fartherNode = this.findFartherNode(edge.getSource(), edge.getTarget());
            var x = this.networkView.getNodeView(fartherNode).getVisualProperty(NODE_X_LOCATION);
            var y = this.networkView.getNodeView(fartherNode).getVisualProperty(NODE_Y_LOCATION);

            networkView.setVisualProperty(NETWORK_CENTER_X_LOCATION, x);
            networkView.setVisualProperty(NETWORK_CENTER_Y_LOCATION, y);
        }
    }

    @Override
    public void enable() {
        this.hideAllEdges();
    }

    public void showTrace(Trace trace) {
        this.hideAllEdges();
    }

}
