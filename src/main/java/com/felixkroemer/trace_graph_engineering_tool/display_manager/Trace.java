package com.felixkroemer.trace_graph_engineering_tool.display_manager;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;

import java.awt.*;
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

    private final Color color;
    private final List<CyEdge> edges;

    public Trace(Color color, List<CyEdge> edges) {
        this.color = color;
        this.edges = edges;
    }

    public Color getColor() {
        return color;
    }

    public List<CyEdge> getEdges() {
        return edges;
    }
}
