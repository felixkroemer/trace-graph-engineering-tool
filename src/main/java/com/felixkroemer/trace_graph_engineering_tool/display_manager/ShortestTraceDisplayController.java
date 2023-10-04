package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import com.felixkroemer.trace_graph_engineering_tool.model.Trace;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.model.UIState;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.view.model.CyNetworkView;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class ShortestTraceDisplayController extends AbstractDisplayController implements PropertyChangeListener {

    private UIState uiState;

    public ShortestTraceDisplayController(CyNetworkView view, TraceGraph traceGraph, UIState uiState) {
        super(view, traceGraph);
        this.uiState = uiState;

        this.uiState.addObserver(this);
        if (uiState.getTrace() != null) {
            this.showTrace(uiState.getTrace());
        }
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
        this.uiState.removeObserver(this);
    }

    public void showTrace(Trace trace) {
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
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "trace" -> {
                this.showTrace((Trace) evt.getNewValue());
            }
        }
    }
}
