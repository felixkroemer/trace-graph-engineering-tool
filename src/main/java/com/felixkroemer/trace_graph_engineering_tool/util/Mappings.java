package com.felixkroemer.trace_graph_engineering_tool.util;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;

import java.awt.*;

public class Mappings {

    public static VisualMappingFunction<Integer, Paint> createEdgeTraversalMapping(int xMin, int xMax,
                                                                                   VisualMappingFunctionFactory factory) {
        ContinuousMapping<Integer, Paint> mapping =
                (ContinuousMapping<Integer, Paint>) factory.createVisualMappingFunction(Columns.EDGE_TRAVERSALS,
                        Integer.class, BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);


        BoundaryRangeValues<Paint> boundarySmall = new BoundaryRangeValues<>(Color.BLUE, Color.BLUE, Color.BLUE);

        int stepMedium = (int) Math.round((xMax - xMin) * 0.05);
        BoundaryRangeValues<Paint> boundaryMedium = new BoundaryRangeValues<>(Color.GREEN, Color.GREEN, Color.GREEN);

        int stepLarge = (int) Math.round((xMax - xMin) * 0.1);
        BoundaryRangeValues<Paint> boundaryLarge = new BoundaryRangeValues<>(Color.ORANGE, Color.ORANGE, Color.ORANGE);

        BoundaryRangeValues<Paint> boundaryMax = new BoundaryRangeValues<>(Color.RED, Color.RED, Color.RED);


        mapping.addPoint(xMin, boundarySmall);
        mapping.addPoint(stepMedium, boundaryMedium);
        mapping.addPoint(stepLarge, boundaryLarge);
        mapping.addPoint(xMax, boundaryMax);

        return mapping;
    }

}
