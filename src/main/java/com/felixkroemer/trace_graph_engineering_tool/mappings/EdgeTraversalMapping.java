package com.felixkroemer.trace_graph_engineering_tool.mappings;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;

import java.awt.*;
import java.util.Collections;
import java.util.Map;

public class EdgeTraversalMapping extends ContinuousMappingImpl {

    public EdgeTraversalMapping(Map<Long, Integer> traversalsMap, CyEventHelper eventHelper) {
        super(traversalsMap, BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, eventHelper);

        int min, max;
        if (!traversalsMap.isEmpty()) {
            max = Collections.max(traversalsMap.values());
            min = Math.max(Collections.min(traversalsMap.values()), 1);
        } else {
            max = 1;
            min = 1;
        }

        BoundaryRangeValues<Paint> boundarySmall = new BoundaryRangeValues<>(Color.BLUE, Color.BLUE, Color.BLUE);

        int stepMedium = (int) Math.round((max - min) * 0.05);
        BoundaryRangeValues<Paint> boundaryMedium = new BoundaryRangeValues<>(Color.GREEN, Color.GREEN, Color.GREEN);

        int stepLarge = (int) Math.round((max - min) * 0.1);
        BoundaryRangeValues<Paint> boundaryLarge = new BoundaryRangeValues<>(Color.ORANGE, Color.ORANGE, Color.ORANGE);

        BoundaryRangeValues<Paint> boundaryMax = new BoundaryRangeValues<>(Color.RED, Color.RED, Color.RED);

        this.addPoint(min, boundarySmall);
        this.addPoint(stepMedium, boundaryMedium);
        this.addPoint(stepLarge, boundaryLarge);
        this.addPoint(max, boundaryMax);
    }
}
