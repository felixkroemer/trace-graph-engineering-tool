package com.felixkroemer.trace_graph_engineering_tool.events;

import org.cytoscape.event.CyListener;

public interface SetCurrentComparisonControllerListener extends CyListener {
    public void handleEvent(SetCurrentComparisonControllerEvent e);
}
