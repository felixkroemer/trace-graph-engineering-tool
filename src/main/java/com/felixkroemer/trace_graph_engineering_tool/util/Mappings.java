package com.felixkroemer.trace_graph_engineering_tool.util;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;

import java.awt.*;

public class Mappings {

    public static VisualMappingFunction<Integer, Double> createSizeMapping(int xMin, int xMax,
                                                                           VisualMappingFunctionFactory factory) {
        ContinuousMapping<Integer, Double> sizeMapping =
                (ContinuousMapping<Integer, Double>) factory.createVisualMappingFunction(Columns.NODE_VISITS,
                        Integer.class, BasicVisualLexicon.NODE_SIZE);

        double steps = 15.0;

        int yMin = 10;
        int yMax = 100;

        double a = (yMin - yMax) / Math.log((xMin * 1.0) / xMax);
        double b = Math.exp((yMax * Math.log(xMin) - yMin * Math.log(xMax)) / (yMin - yMax));

        double x = xMin;
        BoundaryRangeValues<Double> boundary;
        double y = yMin;
        for (int i = 1; i <= steps + 1; i++) {
            boundary = new BoundaryRangeValues<>(y, y, y);
            sizeMapping.addPoint((int) Math.round(x), boundary);
            x = (xMin + ((xMax - xMin) / steps) * i);
            y = a * Math.log(b * x);
        }

        return sizeMapping;
    }

    public static VisualMappingFunction<Integer, Paint> createColorMapping(int xMin, int xMax,
                                                                           VisualMappingFunctionFactory factory) {
        ContinuousMapping<Integer, Paint> mapping =
                (ContinuousMapping<Integer, Paint>) factory.createVisualMappingFunction(Columns.NODE_FREQUENCY,
                        Integer.class, BasicVisualLexicon.NODE_FILL_COLOR);


        BoundaryRangeValues<Paint> boundarySmall = new BoundaryRangeValues<>(Color.BLUE, Color.BLUE, Color.BLUE);

        int stepMedium = (int) Math.round((xMax - xMin) * 0.1);
        BoundaryRangeValues<Paint> boundaryMedium = new BoundaryRangeValues<>(Color.GREEN, Color.GREEN, Color.GREEN);

        BoundaryRangeValues<Paint> boundaryLarge = new BoundaryRangeValues<>(Color.RED, Color.RED, Color.RED);


        mapping.addPoint(xMin, boundarySmall);
        mapping.addPoint(stepMedium, boundaryMedium);
        mapping.addPoint(xMax, boundaryLarge);

        return mapping;
    }

    public static VisualMappingFunction<CyRow, String> createTooltipMapping(VisualMappingFunctionFactory factory) {
        // parameters are not needed here because column name, column type and vp are fixed
        return factory.createVisualMappingFunction(null, null, null);
    }

}
