package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;

import java.awt.*;

public class DrawableSubtrace extends Subtrace {

    private Color color;
    private TraceGraph traceGraph;

    public DrawableSubtrace(CyTable trace, CyNode startNode, int index, TraceGraph traceGraph, Color color) {
        super(trace, startNode, index);
        this.traceGraph = traceGraph;
        this.color = color;
    }

    public DrawableSubtrace(Subtrace trace, TraceGraph traceGraph, Color color) {
        super(trace);
        this.traceGraph = traceGraph;
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }

    public int getWeight() {
        int weight = 0;
        for (var node : this.sequence) {
            var visitDuration = this.traceGraph.getNodeAux(node).getVisitDuration();
            var frequency = this.traceGraph.getNodeAux(node).getFrequency();
            weight += visitDuration + frequency;
        }
        return weight;
    }

    @Override
    public String toString() {
        return "[" + this.getStartIndex() + " -> " + (this.getStartIndex() + this.getSequence().size() - 1) + "]";
    }
}
