package com.felixkroemer.trace_graph_engineering_tool.events;

import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import org.cytoscape.event.AbstractCyEvent;

public final class UpdatedPDMEvent extends AbstractCyEvent<Object> {

    private final ParameterDiscretizationModel pdm;

    public UpdatedPDMEvent(Object source, ParameterDiscretizationModel pdm) {
        super(source, UpdatedPDMEventListener.class);
        this.pdm = pdm;
    }

    public ParameterDiscretizationModel getPDM() {
        return this.pdm;
    }
}
