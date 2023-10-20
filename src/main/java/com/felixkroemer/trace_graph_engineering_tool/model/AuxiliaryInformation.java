package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyTable;

import java.util.*;
import java.util.stream.Collectors;

/*
 * Auxiliary node/edge information that is not contained in the local tables
 */
public class AuxiliaryInformation {
    private Map<CyTable, List<Integer>> source_rows;

    public AuxiliaryInformation() {
        this.source_rows = new HashMap<>();
    }

    public void addSourceRow(CyTable sourceTable, int index) {
        this.source_rows.computeIfAbsent(sourceTable, k -> new LinkedList<>());
        this.source_rows.get(sourceTable).add(index);
    }

    public void removeSourceRows(CyTable table) {
        this.source_rows.remove(table);
    }

    public List<Integer> getSourceRows(CyTable sourceTable) {
        return this.source_rows.get(sourceTable);
    }

    public boolean hasNoSourceRows() {
        this.source_rows =
                this.source_rows.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return this.source_rows.isEmpty();
    }

    public Set<CyTable> getSourceTables() {
        return this.source_rows.keySet();
    }

}
