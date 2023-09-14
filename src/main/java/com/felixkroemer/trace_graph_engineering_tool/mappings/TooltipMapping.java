package com.felixkroemer.trace_graph_engineering_tool.mappings;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

public class TooltipMapping implements PassthroughMapping<CyRow, String> {

    public ParameterDiscretizationModel pdm;
    private int maxLength;

    public TooltipMapping(ParameterDiscretizationModel pdm) {
        this.pdm = pdm;
        this.maxLength = 0;
        for (Parameter p : pdm.getParameters()) {
            maxLength = Math.max(p.getName().length(), maxLength);
        }
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
            sb.append(p.getName());
            sb.append('\t');
            sb.append(row.get(p.getName(), Integer.class));
            sb.append("\n");
        }
        return sb.toString();
    }
}
