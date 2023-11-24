package com.felixkroemer.trace_graph_engineering_tool.mappings;

import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import java.util.Collections;
import java.util.Map;

public class ComparisonSizeMapping implements PassthroughMapping<CyRow, Double> {
    private Map<Long, Integer> baseMapping;
    private Map<Long, Integer> deltaMapping;
    private double min, max;

    public ComparisonSizeMapping(Map<Long, Integer> baseMapping, Map<Long, Integer> deltaMapping) {
        this.baseMapping = baseMapping;
        this.deltaMapping = deltaMapping;

        var baseMax = Collections.max(baseMapping.values());
        var deltaMax = Collections.max(deltaMapping.values());
        this.max = baseMax > deltaMax ? baseMax : deltaMax;

        var baseMin = Collections.min(baseMapping.values());
        var deltaMin = Collections.min(deltaMapping.values());
        this.min = baseMin < deltaMin ? baseMin : deltaMin;

        this.min += 1;
        this.max += 1;
    }


    @Override
    public String getMappingColumnName() {
        return "SUID";
    }

    @Override
    public Class<CyRow> getMappingColumnType() {
        return CyRow.class;
    }

    @Override
    public VisualProperty<Double> getVisualProperty() {
        return BasicVisualLexicon.NODE_SIZE;
    }

    @Override
    public void apply(CyRow row, View<? extends CyIdentifiable> view) {
        final double size = getMappedValue(row);

        view.setVisualProperty(getVisualProperty(), size);
    }

    @Override
    public Double getMappedValue(CyRow row) {
        var suid = row.get("SUID", Long.class);
        var group = row.get(Columns.COMPARISON_GROUP_MEMBERSHIP, Integer.class);
        int linearValue;
        if (group == 2) { // base + delta
            var baseValue = this.baseMapping.get(suid);
            var deltaValue = this.deltaMapping.get(suid);
            linearValue = (baseValue + deltaValue) / 2;
        } else if (group == 1) { // delta
            linearValue = this.deltaMapping.get(suid);
        } else { // base
            linearValue = this.baseMapping.get(suid);
        }
        return getLogValue(linearValue);
    }

    private double getLogValue(int linValue) {
        linValue += 1;
        int yMin = 10;
        int yMax = 100;
        double a = (yMin - yMax) / Math.log((min) / max);
        double b = Math.exp((yMax * Math.log(min) - yMin * Math.log(max)) / (yMin - yMax));
        return a * Math.log(b * linValue);
    }

}
