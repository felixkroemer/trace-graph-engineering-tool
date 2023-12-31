package com.felixkroemer.trace_graph_engineering_tool.events;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import org.cytoscape.event.AbstractCyEvent;
import org.cytoscape.model.CyNetwork;

public final class ShowTraceEvent extends AbstractCyEvent<Object> {

    private final TraceExtension trace;
    private final CyNetwork network;

    public ShowTraceEvent(Object source, TraceExtension trace, CyNetwork network) {
        super(source, ShowTraceEventListener.class);
        this.trace = trace;
        this.network = network;
    }

    public TraceExtension getTrace() {
        return this.trace;
    }

    public CyNetwork getNetwork() {
        return network;
    }
}
