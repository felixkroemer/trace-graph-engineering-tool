package com.felixkroemer.trace_graph_engineering_tool.events;

import org.cytoscape.event.CyListener;

public interface UpdatedPDMEventListener extends CyListener {

    void handleEvent(UpdatedPDMEvent e);
}
