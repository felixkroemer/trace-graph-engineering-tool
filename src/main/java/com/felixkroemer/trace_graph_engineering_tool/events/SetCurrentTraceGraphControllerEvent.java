package com.felixkroemer.trace_graph_engineering_tool.events;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import org.cytoscape.event.AbstractCyEvent;

public final class SetCurrentTraceGraphControllerEvent extends AbstractCyEvent<TraceGraphController> {

    private final TraceGraphController traceGraphController;

    public SetCurrentTraceGraphControllerEvent(TraceGraphController source, TraceGraphController traceGraphController) {
        super(source, SetCurrentTraceGraphControllerListener.class);
        this.traceGraphController = traceGraphController;
    }

    public TraceGraphController getTraceGraphController() {
        return this.traceGraphController;
    }
}
