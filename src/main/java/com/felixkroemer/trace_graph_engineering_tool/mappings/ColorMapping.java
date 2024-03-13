package com.felixkroemer.trace_graph_engineering_tool.mappings;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;

import java.awt.*;
import java.util.Collections;
import java.util.Map;

public class ColorMapping extends ContinuousMappingImpl {

    public ColorMapping(Map<Long, Integer> visitDurationMap, CyEventHelper eventHelper) {
        super(visitDurationMap, BasicVisualLexicon.NODE_FILL_COLOR, eventHelper);

        int min, max;
        if (!visitDurationMap.isEmpty()) {
            max = Collections.max(visitDurationMap.values());
            min = Math.max(Collections.min(visitDurationMap.values()), 1);
        } else {
            max = 1;
            min = 1;
        }

        Color purple = Color.decode("#440154");
        Color blue = Color.decode("#39568C");
        Color turquoise = Color.decode("#1F968B");
        Color green = Color.decode("#73D055");
        Color yellow = Color.decode("#FDE725");

        BoundaryRangeValues<Paint> boundaryMin = new BoundaryRangeValues<>(purple, purple, purple);

        int stepSmall = (int) Math.round((max - min) * 0.001);
        BoundaryRangeValues<Paint> boundarySmall = new BoundaryRangeValues<>(blue, blue, blue);

        int stepMedium = (int) Math.round((max - min) * 0.01);
        BoundaryRangeValues<Paint> boundaryMedium = new BoundaryRangeValues<>(turquoise, turquoise, turquoise);

        int stepLarge = (int) Math.round((max - min) * 0.1);
        BoundaryRangeValues<Paint> boundaryLarge = new BoundaryRangeValues<>(green, green, green);

        BoundaryRangeValues<Paint> boundaryMax = new BoundaryRangeValues<>(yellow, yellow, yellow);

        this.addPoint(min, boundaryMin);
        this.addPoint(stepSmall, boundarySmall);
        this.addPoint(stepMedium, boundaryMedium);
        this.addPoint(stepLarge, boundaryLarge);
        this.addPoint(max, boundaryMax);
    }
}
