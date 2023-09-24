package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.javatuples.Pair;

import java.util.LinkedList;
import java.util.List;


public class Trace {

    // object for self edges, which are not rendered in the graph
    public static CyEdge SELF_EDGE = new CyEdge() {
        @Override
        public CyNode getSource() {
            return null;
        }

        @Override
        public CyNode getTarget() {
            return null;
        }

        @Override
        public boolean isDirected() {
            return false;
        }

        @Override
        public Long getSUID() {
            return null;
        }
    };

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
}
