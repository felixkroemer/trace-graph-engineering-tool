package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNode;
import org.javatuples.Pair;

import java.util.LinkedList;
import java.util.List;

public class Trace {
    protected LinkedList<Pair<CyNode, Integer>> sequence;

    public Trace() {
        this.sequence = new LinkedList<>();
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
}
