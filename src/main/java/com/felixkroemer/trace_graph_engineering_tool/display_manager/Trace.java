package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import org.cytoscape.model.CyNode;
import org.javatuples.Pair;

import java.util.LinkedList;
import java.util.List;


public class Trace {

    private final LinkedList<Pair<CyNode, Integer>> sequence;
    private final CyNode node;

    public Trace(CyNode node, int sourceIndex) {
        this.sequence = new LinkedList<>();
        this.node = node;
        this.sequence.add(new Pair<>(node, sourceIndex));
    }

    public void addBefore(CyNode node, int sourceIndex) {
        this.sequence.addFirst(new Pair<>(node, sourceIndex));
    }

    public void addAfter(CyNode node, int sourceIndex) {
        this.sequence.addLast(new Pair<>(node, sourceIndex));
    }

    public List<Pair<CyNode, Integer>> getSequence() {
        return this.sequence;
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
