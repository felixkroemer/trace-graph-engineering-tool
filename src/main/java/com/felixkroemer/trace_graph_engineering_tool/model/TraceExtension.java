package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import java.awt.*;
import java.util.LinkedList;


public class TraceExtension extends Trace {

    private CyNode primaryNode;
    private Color color;
    private TraceGraph traceGraph;

    public TraceExtension(TraceGraph traceGraph, Color color) {
        super();
        this.color = color;
        this.traceGraph = traceGraph;
    }

    public TraceExtension(Trace trace, TraceGraph traceGraph) {
        super();
        this.primaryNode = null;
        this.traceGraph = traceGraph;
        this.color = Color.BLACK;
        this.sequence = (LinkedList<CyNode>) trace.getSequence();
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
                sb.append(String.format("(%d, %d) -> ", node.getSUID(), this.provenance.get(node).getIndex()));
            } else {
                sb.append(String.format("(%d, %d)", node.getSUID(), this.provenance.get(node).getIndex()));
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

    public void setPrimaryNode(CyNode node) {
        this.primaryNode = node;
    }

    public CyNode getPrimaryNode() {
        return this.primaryNode;
    }
}
