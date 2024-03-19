package com.felixkroemer.trace_graph_engineering_tool.model;

public class EdgeAuxiliaryInformation extends AuxiliaryInformation {

    private int traversals;

    public EdgeAuxiliaryInformation() {
        super();
        this.traversals = 1;
    }

    public void increaseTraversals() {
        this.traversals += 1;
    }

    public int getTraversals() {
        return this.traversals;
    }
}
