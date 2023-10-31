package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.javatuples.Pair;

import java.awt.*;
import java.util.LinkedList;


public class TraceExtension extends Trace {

    private final CyNode node;
    private Color color;
    private int weight;
    private CyNetwork network;

    public TraceExtension(CyNode node, CyNetwork network, int sourceIndex, Color color) {
        super();
        this.node = node;
        this.color = color;
        this.weight = 0;
        this.network = network;
        this.sequence.add(new Pair<>(node, sourceIndex));
    }

    public TraceExtension(Trace trace, CyNetwork network) {
        super();
        this.node = null;
        this.network = network;
        this.color = Color.BLACK;
        this.sequence = (LinkedList<Pair<CyNode, Integer>>) trace.getSequence();
        for(var node : trace.getSequence()) {
            this.increaseWeight(node.getValue0());
        }
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

    @Override
    public void addBefore(CyNode node, int sourceIndex) {
        super.addBefore(node, sourceIndex);
        this.increaseWeight(node);
    }

    public void increaseWeight(CyNode node) {
        var visits = this.network.getDefaultNodeTable().getRow(node.getSUID()).get(Columns.NODE_VISITS, Integer.class);
        var frequency = this.network.getDefaultNodeTable().getRow(node.getSUID()).get(Columns.NODE_FREQUENCY, Integer.class);
        this.weight += visits + frequency;
    }

    @Override
    public void addAfter(CyNode node, int sourceIndex) {
        super.addAfter(node, sourceIndex);
        this.increaseWeight(node);
    }

    public int getWeight() {
        return this.weight;
    }


    public CyNode getNode() {
        return this.node;
    }
}
