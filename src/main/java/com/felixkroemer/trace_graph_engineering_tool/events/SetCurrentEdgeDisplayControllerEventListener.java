package com.felixkroemer.trace_graph_engineering_tool.events;

import org.cytoscape.event.CyListener;

public interface SetCurrentEdgeDisplayControllerEventListener extends CyListener {

    void handleEvent(SetCurrentEdgeDisplayControllerEvent e);
}
