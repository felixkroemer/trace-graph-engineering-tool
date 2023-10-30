package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.events.ClearTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEventListener;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

public class ShortestTraceDisplayController extends AbstractDisplayController implements ShowTraceEventListener {

    public static final String RENDERING_MODE_SHORTEST_TRACE = "RENDERING_MODE_SHORTEST_TRACE";

    //TODO: find better way to pass initial trace
    private TraceExtension trace;

    public ShortestTraceDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph,
                                          TraceExtension trace) {
        super(registrar, view, traceGraph);
        this.trace = trace;
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
/*        if (event.getSelectedEdges().size() == 1) {
            CyEdge edge = event.getSelectedEdges().iterator().next();
            traceGraph.getNetwork().getRow(edge).set(CyNetwork.SELECTED, false);

            var fartherNode = this.findFartherNode(edge.getSource(), edge.getTarget());
            var x = this.networkView.getNodeView(fartherNode).getVisualProperty(NODE_X_LOCATION);
            var y = this.networkView.getNodeView(fartherNode).getVisualProperty(NODE_Y_LOCATION);

            networkView.setVisualProperty(NETWORK_CENTER_X_LOCATION, x);
            networkView.setVisualProperty(NETWORK_CENTER_Y_LOCATION, y);
        }*/
    }

    @Override
    public void init() {
        this.registrar.registerService(this, ShowTraceEventListener.class);
        this.hideAllEdges();
        this.showTrace(this.trace);
    }

    @Override
    public void disable() {
        this.registrar.unregisterService(this, ShowTraceEventListener.class);
        this.traceGraph.setTrace(null);
        var eventHelper = registrar.getService(CyEventHelper.class);
        eventHelper.fireEvent(new ClearTraceEvent(this, this.trace, networkView.getModel()));
    }

    @Override
    public VisualStyle adjustVisualStyle(VisualStyle defaultVisualStyle) {
        return defaultVisualStyle;
    }

    @Override
    public String getID() {
        return RENDERING_MODE_SHORTEST_TRACE;
    }

    public void showTrace(TraceExtension trace) {
        this.hideAllEdges();
        for (int i = 0; i < trace.getSequence().size() - 1; i++) {
            CyEdge edge;
            // is null if the edge is a self edge
            if ((edge = this.traceGraph.getEdge(trace.getSequence().get(i).getValue0(),
                    trace.getSequence().get(i + 1).getValue0())) != null) {
                this.networkView.getEdgeView(edge).batch(v -> {
                    v.setVisualProperty(EDGE_VISIBLE, true);
                    v.setVisualProperty(EDGE_STROKE_UNSELECTED_PAINT, trace.getColor());
                    v.setVisualProperty(EDGE_TARGET_ARROW_UNSELECTED_PAINT, trace.getColor());
                });
            }
        }
    }

    @Override
    public void handleEvent(ShowTraceEvent e) {
        if (e.getNetwork() != this.traceGraph.getNetwork()) {
            return;
        }
        this.showTrace(e.getTrace());
    }
}
