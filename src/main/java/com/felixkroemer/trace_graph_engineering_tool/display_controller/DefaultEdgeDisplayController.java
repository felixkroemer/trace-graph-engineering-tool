package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingController;
import com.felixkroemer.trace_graph_engineering_tool.mappings.EdgeTraversalMapping;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.DefaultEdgeDisplayControllerPanel;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.EdgeDisplayControllerPanel;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;
import org.javatuples.Pair;

import java.beans.PropertyChangeListener;
import java.util.HashMap;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_VISIBLE;

public class DefaultEdgeDisplayController extends AbstractEdgeDisplayController {

    public static final String RENDERING_MODE_FULL = "RENDERING_MODE_FULL";
    public static final String DISPLAY_RANGE = "display_range";
    public static final String MAX_TRAVERSALS = "max_traversals";
    private Pair<Integer, Integer> displayRange;
    private int maxTraversals;

    public DefaultEdgeDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph,
                                        RenderingController renderingController) {
        super(registrar, view, traceGraph, renderingController);
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
    }

    @Override
    public void onNetworkChanged() {
        this.init();
    }

    @Override
    public void init() {
        this.maxTraversals = 0;
        this.updateMaxTraversals();
        this.displayRange = new Pair<>(0, maxTraversals);
        this.pcs.firePropertyChange(DefaultEdgeDisplayController.DISPLAY_RANGE, null, this.displayRange);
        this.hideEdges();
    }

    @Override
    public void dispose() {

    }

    public void updateMaxTraversals() {
        int maxTraversals = 0;
        for (CyEdge edge : this.traceGraph.getNetwork().getEdgeList()) {
            int traversals = this.traceGraph.getEdgeAux(edge).getTraversals();
            if (traversals > maxTraversals) maxTraversals = traversals;
        }
        this.maxTraversals = maxTraversals;
        this.pcs.firePropertyChange(DefaultEdgeDisplayController.MAX_TRAVERSALS, null, this.maxTraversals);
    }

    @Override
    public VisualStyle adjustVisualStyle(VisualStyle defaultVisualStyle) {
        this.updateMaxTraversals();

        var traversalsMapping = new HashMap<Long, Integer>();
        for (CyEdge edge : this.traceGraph.getNetwork().getEdgeList()) {
            traversalsMapping.put(edge.getSUID(), this.traceGraph.getEdgeAux(edge).getTraversals());
        }
        defaultVisualStyle.addVisualMappingFunction(new EdgeTraversalMapping(traversalsMapping, registrar.getService(CyEventHelper.class)));

        return defaultVisualStyle;
    }

    @Override
    public String getID() {
        return RENDERING_MODE_FULL;
    }

    public EdgeDisplayControllerPanel getSettingsPanel() {
        return new DefaultEdgeDisplayControllerPanel(this);
    }

    public Pair<Integer, Integer> getDisplayRange() {
        return this.displayRange;
    }

    public void setDisplayRange(int from, int to) {
        this.displayRange = new Pair<>(from, to);
        this.hideEdges();
    }

    private void hideEdges() {
        CyEventHelper helper = registrar.getService(CyEventHelper.class);
        helper.flushPayloadEvents();
        for (CyEdge edge : this.traceGraph.getNetwork().getEdgeList()) {
            var traversals = this.traceGraph.getEdgeAux(edge).getTraversals();
            networkView.getEdgeView(edge).setVisualProperty(EDGE_VISIBLE,
                    traversals >= displayRange.getValue0() && traversals <= displayRange.getValue1());
        }
    }

    public int getMaxTraversals() {
        return this.maxTraversals;
    }

    public void addObserver(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(DefaultEdgeDisplayController.DISPLAY_RANGE, listener);
        this.pcs.addPropertyChangeListener(DefaultEdgeDisplayController.MAX_TRAVERSALS, listener);
    }
}
