package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;

import java.awt.*;

public class TraceExtension extends Trace {

    private CyNode primaryNode;
    private Color color;
    private TraceGraph traceGraph;

    public TraceExtension(CyTable sourceTable, CyNode startNode, int index, TraceGraph traceGraph, Color color) {
        super(sourceTable, startNode, index);
        this.traceGraph = traceGraph;
        this.color = color;
    }

    public TraceExtension(Trace trace, TraceGraph traceGraph, Color color) {
        super(trace);
        this.primaryNode = null;
        this.traceGraph = traceGraph;
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.sequence.size(); i++) {
            var node = this.sequence.get(i);
            if (i < this.sequence.size() - 1) {
                sb.append(String.format("(%d, %d) -> ", node.getSUID(), this.startIndex + i));
            } else {
                sb.append(String.format("(%d, %d)", node.getSUID(), this.startIndex + i));
            }
        }
        return sb.toString();
    }

    public int getWeight() {
        int weight = 0;
        for (var node : this.sequence) {
            var visits = this.traceGraph.getNodeAux(node).getVisits();
            var frequency = this.traceGraph.getNodeAux(node).getFrequency();
            weight += visits + frequency;
        }
        return weight;
    }

    public CyNode getPrimaryNode() {
        return this.primaryNode;
    }

    public void setPrimaryNode(CyNode node) {
        this.primaryNode = node;
    }
}
