package com.felixkroemer.trace_graph_engineering_tool.events;


import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkComparisonController;
import org.cytoscape.event.AbstractCyEvent;

public final class SetCurrentComparisonControllerEvent extends AbstractCyEvent<NetworkComparisonController> {

    private final NetworkComparisonController comparisonController;

    public NetworkComparisonController getNetworkComparisonController() {
        return this.comparisonController;
    }

    public SetCurrentComparisonControllerEvent(NetworkComparisonController source,
                                               NetworkComparisonController comparisonController) {
        super(source, SetCurrentComparisonControllerListener.class);
        this.comparisonController = comparisonController;
    }
}
