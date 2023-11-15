package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import java.awt.*;
import java.util.LinkedList;


public class TraceExtension extends Trace {

    private CyNode primaryNode;
    private Color color;
    private CyNetwork network;

    public TraceExtension(CyNetwork network, Color color) {
        super();
        this.color = color;
        this.network = network;
    }

    public TraceExtension(Trace trace, CyNetwork network) {
        super();
        this.primaryNode = null;
        this.network = network;
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
            var visits = this.network.getDefaultNodeTable().getRow(node.getSUID()).get(Columns.NODE_VISITS,
                    Integer.class);
            var frequency = this.network.getDefaultNodeTable().getRow(node.getSUID()).get(Columns.NODE_FREQUENCY,
                    Integer.class);
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
