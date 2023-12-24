package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyTable;

public class TraceElementProvenance {

    private CyTable sourceTable;
    private int index;

    public TraceElementProvenance(CyTable table, int index) {
        this.sourceTable = table;
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}
