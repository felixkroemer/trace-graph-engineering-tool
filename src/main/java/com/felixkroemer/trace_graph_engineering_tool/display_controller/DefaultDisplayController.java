package com.felixkroemer.trace_graph_engineering_tool.display_controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Mappings;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;

public class DefaultDisplayController extends AbstractDisplayController {

    public static final String RENDERING_MODE_FULL = "RENDERING_MODE_FULL";

    public DefaultDisplayController(CyServiceRegistrar registrar, CyNetworkView view, TraceGraph traceGraph) {
        super(registrar, view, traceGraph);
        this.showALlEdges();
    }

    @Override
    public void handleNodesSelected(SelectedNodesAndEdgesEvent event) {
    }

    @Override
    public void init() {
    }

    @Override
    public void disable() {
    }

    @Override
    public VisualStyle adjustVisualStyle(VisualStyle defaultVisualStyle) {
        var visualStyleFactory = registrar.getService(VisualStyleFactory.class);
        var newStyle = visualStyleFactory.createVisualStyle(defaultVisualStyle);

        VisualMappingFunctionFactory visualMappingFunctionFactory =
                registrar.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");

        int maxTraversals = 0;
        for (CyRow row : this.traceGraph.getNetwork().getTable(CyEdge.class, CyNetwork.LOCAL_ATTRS).getAllRows()) {
            int traversals = row.get(Columns.EDGE_TRAVERSALS, Integer.class);
            if (traversals > maxTraversals) maxTraversals = traversals;
        }

        var traversalMapping = Mappings.createEdgeTraversalMapping(1, maxTraversals, visualMappingFunctionFactory);
        newStyle.addVisualMappingFunction(traversalMapping);

        return newStyle;
    }

    @Override
    public String getID() {
        return RENDERING_MODE_FULL;
    }
}
