package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Trace {
    protected LinkedList<CyNode> sequence;
    protected Map<CyNode, TraceElementProvenance> provenance;

    public Trace() {
        this.sequence = new LinkedList<>();
        this.provenance = new HashMap<>();
    }

    public void addBefore(CyNode node, CyTable sourceTable, int sourceIndex) {
        this.sequence.addFirst(node);
        this.provenance.put(node, new TraceElementProvenance(sourceTable, sourceIndex));
    }

    public void addAfter(CyNode node, CyTable sourceTable, int sourceIndex) {
        this.sequence.addLast(node);
        this.provenance.put(node, new TraceElementProvenance(sourceTable, sourceIndex));
    }

    public List<CyNode> getSequence() {
        return this.sequence;
    }

    public TraceElementProvenance getProvenance(CyNode node) {
        return this.provenance.get(node);
    }
}


