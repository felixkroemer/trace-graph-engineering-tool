package com.felixkroemer.trace_graph_engineering_tool.events;

import org.cytoscape.event.CyListener;

public interface UpdatedTraceGraphEventListener extends CyListener {

    void handleEvent(UpdatedTraceGraphEvent e);
}
