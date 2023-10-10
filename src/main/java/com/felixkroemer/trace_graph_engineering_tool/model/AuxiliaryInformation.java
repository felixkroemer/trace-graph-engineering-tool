package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyTable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/*
 * Auxiliary node/edge information that is not contained in the local tables
 */
public class AuxiliaryInformation {
    private final Map<CyTable, List<Integer>> source_rows;

    public AuxiliaryInformation() {
        this.source_rows = new HashMap<>();
    }

    public void addSourceRow(CyTable sourceTable, int index) {
        this.source_rows.computeIfAbsent(sourceTable, k -> new LinkedList<>());
        this.source_rows.get(sourceTable).add(index);
    }

    public List<Integer> getSourceRows(CyTable sourceTable) {
        return this.source_rows.get(sourceTable);
    }

}
