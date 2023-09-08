package com.felixkroemer.trace_graph_engineering_tool.util;

import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;

import java.awt.*;

public class Mappings {

    public static VisualMappingFunction createSizeMapping(int xMin, int xMax, VisualMappingFunctionFactory factory) {
        ContinuousMapping sizeMapping = (ContinuousMapping) factory.createVisualMappingFunction("visits",
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
            boundary = new BoundaryRangeValues<Double>(y, y, y);
            sizeMapping.addPoint(x, boundary);
            x = xMin + ((xMax - xMin) / steps) * i;
            y = a * Math.log(b * x);
        }

        return sizeMapping;
    }

    public static VisualMappingFunction createColorMapping(int xMin, int xMax, VisualMappingFunctionFactory factory) {
        ContinuousMapping mapping = (ContinuousMapping) factory.createVisualMappingFunction("frequency",
                Integer.class, BasicVisualLexicon.NODE_FILL_COLOR);


        double stepSmall = xMin;
        BoundaryRangeValues<Paint> boundarySmall = new BoundaryRangeValues<Paint>(Color.BLUE, Color.BLUE, Color.BLUE);

        double stepMedium = (xMax - xMin) * 0.1;
        BoundaryRangeValues<Paint> boundaryMedium = new BoundaryRangeValues<Paint>(Color.GREEN, Color.GREEN,
                Color.GREEN);

        double stepLarge = xMax;
        BoundaryRangeValues<Paint> boundaryLarge = new BoundaryRangeValues<Paint>(Color.RED, Color.RED, Color.RED);


        mapping.addPoint(stepSmall, boundarySmall);
        mapping.addPoint(stepMedium, boundaryMedium);
        mapping.addPoint(stepLarge, boundaryLarge);

        return mapping;
    }

}
