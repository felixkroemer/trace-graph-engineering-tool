package com.felixkroemer.trace_graph_engineering_tool.mappings;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.events.VisualMappingFunctionChangeRecord;
import org.cytoscape.view.vizmap.events.VisualMappingFunctionChangedEvent;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.ContinuousMappingPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Adapted from org.cytoscape.view.vizmap.internal.mappings.ContinousMappingImpl and org.cytoscape.view.vizmap
// .mappings.AbstractVisualMappingFunction to work without columns

/*
 * #%L
 * Cytoscape VizMap Impl (vizmap-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2021 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

/**
 * Implements an interpolation table mapping data to values of a particular
 * class. The data value is extracted from a bundle of attributes by using a
 * specified data attribute name.
 *
 * @param <> Type of object Visual Property holds
 *           <p>
 *           For refactoring changes in this class, please refer to:
 *           cytoscape.visual.mappings.continuous.README.txt.
 */
public class ContinuousMappingImpl implements ContinuousMapping<Integer, Paint> {

    private Map<Long, Integer> mappedValues;

    /**
     * Contains List of Data Points
     */
    private List<ContinuousMappingPoint<Integer, Paint>> points;

    /**
     * Visual Property used in this mapping.
     */
    private final VisualProperty<Paint> vp;

    private final CyEventHelper eventHelper;

    private final Object lock = new Object();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContinuousMappingImpl(final Map<Long, Integer> mappedValues, final VisualProperty<Paint> vp,
                                 final CyEventHelper eventHelper) {
        this.mappedValues = mappedValues;
        this.vp = vp;
        this.eventHelper = eventHelper;
        this.points = new ArrayList<>();
    }

    @Override
    public List<ContinuousMappingPoint<Integer, Paint>> getAllPoints() {
        return Collections.unmodifiableList(points);
    }

    @Override
    public void addPoint(Integer value, BoundaryRangeValues<Paint> brv) {
        synchronized (lock) {
            points.add(new ContinuousMappingPoint<>(value, brv, this, eventHelper));
        }
        eventHelper.addEventPayload(this, new VisualMappingFunctionChangeRecord(),
                VisualMappingFunctionChangedEvent.class);
    }

    @Override
    public void removePoint(int index) {
        synchronized (lock) {
            points.remove(index);
        }
        eventHelper.addEventPayload(this, new VisualMappingFunctionChangeRecord(),
                VisualMappingFunctionChangedEvent.class);
    }

    @Override
    public int getPointCount() {
        return points.size();
    }

    @Override
    public ContinuousMappingPoint<Integer, Paint> getPoint(int index) {
        if (points.isEmpty()) return null;

        if (points.size() > index) return points.get(index);
        else
            throw new IllegalArgumentException("Invalid Index: " + index + ".  There are " + points.size() + " points"
                    + ".");
    }

    @Override
    public Paint getMappedValue(final CyRow row) {
        Paint value = null;

        // Skip if source attribute is not defined.
        // ViewColumn will automatically substitute the per-VS or global default, as appropriate

        // In all cases, attribute value should be a number for continuous mapping.
        try {
            final Integer attrValue = this.mappedValues.get(row.get("SUID", Long.class));
            value = getRangeValue(attrValue);
        } catch (ClassCastException e) {
        }


        return value;
    }

    private Paint getRangeValue(Integer domainValue) {
        if (points.isEmpty() || domainValue == null || Double.isNaN(((Number) domainValue).doubleValue())) return null;

        ContinuousMappingPoint<Integer, Paint> firstPoint = points.get(0);
        Integer minDomain = firstPoint.getValue();

        // if given domain value is smaller than any in our list,
        // return the range value for the smallest domain value we have.
        int firstCmp = compareValues(domainValue, minDomain);

        if (firstCmp <= 0) {
            BoundaryRangeValues<Paint> bv = firstPoint.getRange();

            if (firstCmp < 0) return bv.lesserValue;
            else return bv.equalValue;
        }

        // if given domain value is larger than any in our Vector,
        // return the range value for the largest domain value we have.
        ContinuousMappingPoint<Integer, Paint> lastPoint = points.get(points.size() - 1);
        Integer maxDomain = lastPoint.getValue();

        if (compareValues(domainValue, maxDomain) > 0) {
            BoundaryRangeValues<Paint> bv = lastPoint.getRange();

            return bv.greaterValue;
        }

        // Note that the list of Points is sorted.
        // Also, the case of the inValue equalling the smallest key was
        // checked above.
        ContinuousMappingPoint<Integer, Paint> currentPoint;
        int index = 0;

        for (index = 0; index < points.size(); index++) {
            currentPoint = points.get(index);

            Integer currentValue = currentPoint.getValue();
            int cmpValue = compareValues(domainValue, currentValue);

            if (cmpValue == 0) {
                BoundaryRangeValues<Paint> bv = currentPoint.getRange();

                return bv.equalValue;
            } else if (cmpValue < 0) break;
        }

        return getRangeValue(index, domainValue);
    }

    /**
     * This is tricky. The desired domain value is greater than lowerDomain and
     * less than upperDomain. Therefore, we want the "greater" field of the
     * lower boundary value (because the desired domain value is greater) and
     * the "lesser" field of the upper boundary value (semantic difficulties).
     */
    @SuppressWarnings("unchecked")
    private Paint getRangeValue(int index, Integer domainValue) {
        // Get Lower Domain and Range
        ContinuousMappingPoint<Integer, Paint> lowerBound = points.get(index - 1);
        int lowerDomain = lowerBound.getValue();
        BoundaryRangeValues<Paint> lv = lowerBound.getRange();
        Color lowerRange = (Color) lv.greaterValue;

        // Get Upper Domain and Range
        ContinuousMappingPoint<Integer, Paint> upperBound = points.get(index);
        int upperDomain = upperBound.getValue();
        BoundaryRangeValues<Paint> gv = upperBound.getRange();
        Color upperRange = (Color) gv.lesserValue;

        double frac = (double) (domainValue - lowerDomain) / (upperDomain - lowerDomain);

        double red = lowerRange.getRed() + (frac * (upperRange.getRed() - lowerRange.getRed()));
        double green = lowerRange.getGreen() + (frac * (upperRange.getGreen() - lowerRange.getGreen()));
        double blue = lowerRange.getBlue() + (frac * (upperRange.getBlue() - lowerRange.getBlue()));
        double alpha = lowerRange.getAlpha() + (frac * (upperRange.getAlpha() - lowerRange.getAlpha()));

        Color value = new Color((int) Math.round(red), (int) Math.round(green), (int) Math.round(blue),
                (int) Math.round(alpha));

        return value;
    }

    @Override
    public String toString() {
        return ContinuousMapping.CONTINUOUS;
    }

    @Override
    public String getMappingColumnName() {
        return "SUID";
    }

    @Override
    public Class<Integer> getMappingColumnType() {
        return Integer.class;
    }

    @Override
    public VisualProperty<Paint> getVisualProperty() {
        return vp;
    }

    /**
     * Helper function to compare Number objects. This is needed because Java
     * doesn't allow comparing, for example, Integer objects to Double objects.
     */
    private int compareValues(Integer probe, Integer target) {
        final Number n1 = (Number) probe;
        final Number n2 = (Number) target;
        double d1 = n1 != null ? n1.doubleValue() : Double.NEGATIVE_INFINITY;
        double d2 = n2 != null ? n2.doubleValue() : Double.NEGATIVE_INFINITY;

        if (d1 < d2) return -1;
        else if (d1 > d2) return 1;
        else return 0;
    }

    @Override
    public void apply(final CyRow row, final View<? extends CyIdentifiable> view) {
        final Paint value = getMappedValue(row);

        if (value != null) view.setVisualProperty(vp, value);
    }
}
