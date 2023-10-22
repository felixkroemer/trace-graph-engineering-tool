package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNode;
import org.javatuples.Pair;

import java.awt.*;
import java.util.LinkedList;


public class TraceExtension extends Trace {

    private final CyNode node;
    private Color color;

    public TraceExtension(CyNode node, int sourceIndex, Color color) {
        super();
        this.node = node;
        this.color = color;
        this.sequence.add(new Pair<>(node, sourceIndex));
    }

    public TraceExtension(Trace trace) {
        super();
        this.node = null;
        this.color = Color.BLACK;
        this.sequence = (LinkedList<Pair<CyNode, Integer>>) trace.getSequence();
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
                sb.append(String.format("(%d, %d) -> ", node.getValue0().getSUID(), node.getValue1()));
            } else {
                sb.append(String.format("(%d, %d)", node.getValue0().getSUID(), node.getValue1()));
            }
        }
        return sb.toString();
    }

    public CyNode getNode() {
        return this.node;
    }
}
