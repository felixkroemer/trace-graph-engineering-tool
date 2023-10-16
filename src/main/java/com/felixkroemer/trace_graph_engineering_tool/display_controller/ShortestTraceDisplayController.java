package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEventListener;
import com.felixkroemer.trace_graph_engineering_tool.model.Trace;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;

import java.awt.*;
import java.util.List;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class ShortestTraceDisplayController extends AbstractDisplayController implements ShowTraceEventListener {

    public ShortestTraceDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph,
                                          List<CyNode> nodes) {
        super(registrar, view, traceGraph);

        Trace trace = this.traceGraph.findTrace(nodes);
        if (trace != null) {
            this.showTrace(trace);
        }

        this.registrar.registerService(this, ShowTraceEventListener.class);
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
    public void disable() {
        this.registrar.unregisterService(this, ShowTraceEventListener.class);
    }

    public void showTrace(Trace trace) {
        this.hideAllEdges();
        for (int i = 0; i < trace.getSequence().size() - 1; i++) {
            CyEdge edge;
            // is null if the edge is a self edge
            if ((edge = this.traceGraph.getEdge(trace.getSequence().get(i).getValue0(),
                    trace.getSequence().get(i + 1).getValue0())) != null) {
                this.networkView.getEdgeView(edge).batch(v -> {
                    v.setVisualProperty(EDGE_STROKE_UNSELECTED_PAINT, Color.BLACK);
                    v.setVisualProperty(EDGE_TARGET_ARROW_UNSELECTED_PAINT, Color.BLACK);
                    v.setVisualProperty(EDGE_VISIBLE, true);
                });
            }
        }
    }

    @Override
    public void handleEvent(ShowTraceEvent e) {
        if (e.getNetwork() != this.traceGraph.getNetwork()) {
            return;
        }
        Trace trace = this.traceGraph.findTrace(e.getNodes());
        if (trace != null) {
            this.showTrace(trace);
        }
    }
}