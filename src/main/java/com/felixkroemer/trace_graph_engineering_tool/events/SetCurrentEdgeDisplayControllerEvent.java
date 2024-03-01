package com.felixkroemer.trace_graph_engineering_tool.events;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingController;
import com.felixkroemer.trace_graph_engineering_tool.display_controller.EdgeDisplayController;
import org.cytoscape.event.AbstractCyEvent;

public final class SetCurrentEdgeDisplayControllerEvent extends AbstractCyEvent<RenderingController> {

    private final EdgeDisplayController previous;
    private final EdgeDisplayController current;

    public SetCurrentEdgeDisplayControllerEvent(RenderingController source, EdgeDisplayController previous,
                                                EdgeDisplayController current) {
        super(source, SetCurrentEdgeDisplayControllerEventListener.class);
        this.previous = previous;
        this.current = current;
    }

    public EdgeDisplayController getPreviousController() {
        return this.previous;
    }

    public EdgeDisplayController getCurrentController() {
        return this.current;
    }
}
