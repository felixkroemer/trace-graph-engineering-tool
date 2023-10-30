package com.felixkroemer.trace_graph_engineering_tool.events;


import com.felixkroemer.trace_graph_engineering_tool.display_controller.TracesDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import org.cytoscape.event.AbstractCyEvent;
import org.cytoscape.model.CyNetwork;

import java.util.Collection;
import java.util.Set;

public final class ShowTraceSetEvent extends AbstractCyEvent<TracesDisplayController> {

    private final Collection<TraceExtension> traces;

    private final CyNetwork network;


    public ShowTraceSetEvent(TracesDisplayController source, Collection<TraceExtension> traces, CyNetwork network) {
        super(source, ShowTraceSetEventListener.class);
        this.traces = traces;
        this.network = network;
    }

    public Collection<TraceExtension> getTraces() {
        return this.traces;
    }

    public CyNetwork getNetwork() {
        return network;
    }

}
