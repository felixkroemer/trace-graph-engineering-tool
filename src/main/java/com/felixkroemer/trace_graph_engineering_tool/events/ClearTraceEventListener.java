package com.felixkroemer.trace_graph_engineering_tool.events;

import org.cytoscape.event.CyListener;

public interface ClearTraceEventListener extends CyListener {
    public void handleEvent(ClearTraceEvent e);
}
