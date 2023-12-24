package com.felixkroemer.trace_graph_engineering_tool.events;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingController;
import com.felixkroemer.trace_graph_engineering_tool.display_controller.AbstractEdgeDisplayController;
import org.cytoscape.event.AbstractCyEvent;

public final class SetCurrentEdgeDisplayControllerEvent extends AbstractCyEvent<RenderingController> {

    private final AbstractEdgeDisplayController previous;
    private final AbstractEdgeDisplayController current;

    public SetCurrentEdgeDisplayControllerEvent(RenderingController source,
                                                AbstractEdgeDisplayController previous, AbstractEdgeDisplayController current) {
        super(source, SetCurrentEdgeDisplayControllerEventListener.class);
        this.previous = previous;
        this.current = current;
    }

    public AbstractEdgeDisplayController getPreviousController() {
        return this.previous;
    }

    public AbstractEdgeDisplayController getCurrentController() {
        return this.current;
    }
}
