package com.felixkroemer.trace_graph_engineering_tool.events;

import org.cytoscape.event.AbstractCyEvent;

public final class UpdatedTraceGraphEvent extends AbstractCyEvent<Object> {

    public UpdatedTraceGraphEvent(Object source) {
        super(source, UpdatedTraceGraphEventListener.class);
    }
}
