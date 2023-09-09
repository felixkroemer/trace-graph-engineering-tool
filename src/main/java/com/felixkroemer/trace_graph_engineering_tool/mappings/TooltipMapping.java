package com.felixkroemer.trace_graph_engineering_tool.mappings;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;

public class TooltipMapping implements VisualMappingFunction<CyRow, String> {

    public ParameterDiscretizationModel pdm;
    private CyRow lastRow;

    public TooltipMapping(ParameterDiscretizationModel pdm) {
        this.pdm = pdm;
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
    public VisualProperty<String> getVisualProperty() {
        return BasicVisualLexicon.NODE_TOOLTIP;
    }

    @Override
    public void apply(CyRow row, View<? extends CyIdentifiable> view) {
        final String tooltip = getMappedValue(row);

        view.setVisualProperty(getVisualProperty(), tooltip);
    }

    @Override
    public String getMappedValue(CyRow row) {
        StringBuilder sb = new StringBuilder();
        for (Parameter p : this.pdm.getParameters()) {
            sb.append(p.getName()).append(":");
            sb.append(row.get(p.getName(), Integer.class));
            sb.append("\n");
        }
        this.lastRow = row;
        return sb.toString();
    }
}
