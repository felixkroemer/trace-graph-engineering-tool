package com.felixkroemer.trace_graph_engineering_tool.events;


import com.felixkroemer.trace_graph_engineering_tool.tasks.ShowTraceTask;
import org.cytoscape.event.AbstractCyEvent;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import java.util.List;

public final class ShowTraceEvent extends AbstractCyEvent<ShowTraceTask> {

    private final List<CyNode> nodes;

    private final CyNetwork network;


    public ShowTraceEvent(ShowTraceTask source, List<CyNode> nodes, CyNetwork network) {
        super(source, ShowTraceEventListener.class);
        this.nodes = nodes;
        this.network = network;
    }

    public List<CyNode> getNodes() {
        return this.nodes;
    }

    public CyNetwork getNetwork() {
        return network;
    }

}
