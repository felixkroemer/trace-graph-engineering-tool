package com.felixkroemer.trace_graph_engineering_tool.model;

/*
 * Auxiliary node/edge information that is not contained in the local tables
 */
public class NodeAuxiliaryInformation extends AuxiliaryInformation {

    private int visits;
    private int frequency;

    public NodeAuxiliaryInformation() {
        super();
        this.visits = 1;
        this.frequency = 0;
    }

    public void increaseVisits() {
        this.visits += 1;
    }

    public int getVisits() {
        return this.visits;
    }

    public void increaseFrequency() {
        this.frequency += 1;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void fixVisitsAndFrequency() {
        int visits = 0;
        int frequency = 0;
        for (var list : this.source_rows.values()) {
            int prev = Integer.MIN_VALUE;
            for (int i : list) {
                if (i == prev + 1) {
                    frequency += 1;
                } else {
                    visits += 1;
                }
                prev = i;
            }
        }
        this.visits = visits;
        this.frequency = frequency;
    }
}
