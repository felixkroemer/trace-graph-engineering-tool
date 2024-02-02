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

    public void fixTraversals() {
        int traversals = 0;
        for (var pair : this.situations) {
            traversals += pair.getValue1().size();
        }
        this.traversals = traversals;
    }
}
