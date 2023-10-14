package com.felixkroemer.trace_graph_engineering_tool.events;


import com.felixkroemer.trace_graph_engineering_tool.display_manager.TracesDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import org.cytoscape.event.AbstractCyEvent;
import org.cytoscape.model.CyNetwork;

import java.util.Set;

public final class ShowTraceSetEvent extends AbstractCyEvent<TracesDisplayController> {

    private final Set<TraceExtension> traces;

    private final CyNetwork network;


    public ShowTraceSetEvent(TracesDisplayController source, Set<TraceExtension> traces, CyNetwork network) {
        super(source, ShowTraceSetEventListener.class);
        this.traces = traces;
        this.network = network;
    }

    public Set<TraceExtension> getTraces() {
        return this.traces;
    }

    public CyNetwork getNetwork() {
        return network;
    }

}