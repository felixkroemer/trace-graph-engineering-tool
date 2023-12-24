package com.felixkroemer.trace_graph_engineering_tool.events;

import org.cytoscape.event.CyListener;

public interface SetCurrentTraceGraphControllerListener extends CyListener {

    void handleEvent(SetCurrentTraceGraphControllerEvent e);
}
