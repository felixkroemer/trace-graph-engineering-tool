package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Trace {

    protected LinkedList<CyNode> sequence;
    protected int startIndex;
    protected CyTable sourceTable;

    private Trace(CyTable sourceTable) {
        this.sourceTable = sourceTable;
        this.sequence = new LinkedList<>();
    }

    public Trace(CyTable sourceTable, List<CyNode> nodes, int startIndex) {
        this(sourceTable);
        this.sequence = new LinkedList<>(nodes);
        this.startIndex = startIndex;
    }

    public Trace(CyTable sourceTable, CyNode node, int index) {
        this(sourceTable);
        this.sequence.add(node);
        this.startIndex = index;
    }

    public Trace(Trace trace) {
        this.sourceTable = trace.getSourceTable();
        this.sequence = (LinkedList<CyNode>) trace.getSequence();
        this.startIndex = trace.getWindow().getValue0();
    }

    public void addBefore(CyNode node) {
        this.sequence.addFirst(node);
        this.startIndex -= 1;
    }

    public void addAfter(CyNode node) {
        this.sequence.addLast(node);
    }

    public List<CyNode> getSequence() {
        return new LinkedList<>(this.sequence);
    }

    public List<CyNode> getUniqueSequence() {
        return this.sequence.stream().distinct().collect(Collectors.toList());
    }

    public List<Pair<CyNode, Pair<Integer, Integer>>> getIndices() {
        List<Pair<CyNode, Pair<Integer, Integer>>> indices = new ArrayList<>();
        for (int i = 0; i < this.sequence.size(); i++) {
            var node = this.sequence.get(i);
            var start = i;
            while (i < this.sequence.size() - 1 && this.sequence.get(i + 1) == node) {
                i += 1;
            }
            var end = i;
            indices.add(new Pair<>(node, new Pair<>(this.startIndex + start, this.startIndex + end)));
        }
        return indices;
    }

    public CyTable getSourceTable() {
        return this.sourceTable;
    }

    public Pair<Integer, Integer> getWindow() {
        return new Pair<>(startIndex, startIndex + sequence.size());
    }

    public int getStartIndex() {
        return this.startIndex;
    }
}


