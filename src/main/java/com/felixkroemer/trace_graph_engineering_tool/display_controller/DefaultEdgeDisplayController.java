package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingController;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.DefaultEdgeDisplayControllerPanel;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.EdgeDisplayControllerPanel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.javatuples.Pair;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

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
        for (CyRow row : this.traceGraph.getNetwork().getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS).getAllRows()) {
            int traversals = row.get(Columns.EDGE_TRAVERSALS, Integer.class);
            if (traversals > maxTraversals) maxTraversals = traversals;
        }
        this.maxTraversals = maxTraversals;
        this.pcs.firePropertyChange(DefaultEdgeDisplayController.MAX_TRAVERSALS, null, this.maxTraversals);
    }

    @Override
    public VisualStyle adjustVisualStyle(VisualStyle defaultVisualStyle) {
        this.updateMaxTraversals();
        var visualStyleFactory = registrar.getService(VisualStyleFactory.class);
        var newStyle = visualStyleFactory.createVisualStyle(defaultVisualStyle);

        VisualMappingFunctionFactory visualMappingFunctionFactory =
                registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");

        var traversalMapping = Mappings.createEdgeTraversalMapping(1, maxTraversals, visualMappingFunctionFactory);
        newStyle.addVisualMappingFunction(traversalMapping);

        return newStyle;
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
        var edgeTable = traceGraph.getNetwork().getDefaultEdgeTable();
        var suids = edgeTable.getColumn("SUID").getValues(Long.class);
        var values = edgeTable.getColumn(Columns.EDGE_TRAVERSALS).getValues(Integer.class);
        List<Pair<Long, Integer>> pairs = new ArrayList<>(suids.size());
        for (int i = 0; i < suids.size(); i++) {
            pairs.add(new Pair<>(suids.get(i), values.get(i)));
        }
        for (var pair : pairs) {
            var edge = traceGraph.getNetwork().getEdge(pair.getValue0());
            networkView.getEdgeView(edge).setVisualProperty(EDGE_VISIBLE,
                    pair.getValue1() >= displayRange.getValue0() && pair.getValue1() <= displayRange.getValue1());
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
