package com.felixkroemer.trace_graph_engineering_tool.mappings;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import java.util.Collections;
import java.util.Map;

public class SizeMapping implements PassthroughMapping<CyRow, Double> {
    private Map<Long, Integer> frequency;
    private double max, min;

    public SizeMapping(Map<Long, Integer> frequency) {
        this.frequency = frequency;

        this.max = Collections.max(frequency.values());
        this.min = Collections.min(frequency.values());
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
        var linearValue = this.frequency.get(suid);
        return getLogValue(linearValue);
    }

    private double getLogValue(int linValue) {
        linValue += 1;
        int yMin = 10;
        int yMax = 100;
        double a = (yMin - yMax) / Math.log((this.min) / this.max);
        double b = Math.exp((yMax * Math.log(this.min) - yMin * Math.log(this.max)) / (yMin - yMax));
        return a * Math.log(b * linValue);
    }

}
