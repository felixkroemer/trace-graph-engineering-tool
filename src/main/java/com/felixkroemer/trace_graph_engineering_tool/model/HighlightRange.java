package com.felixkroemer.trace_graph_engineering_tool.model;

public class HighlightRange {
    private int lowerBound;
    private int upperBound;

    public HighlightRange(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public int getLowerBound() {
        return this.lowerBound;
    }

    public int getUpperBound() {
        return this.upperBound;
    }
}
