package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyTable;

import java.util.*;
import java.util.stream.Collectors;

/*
 * Auxiliary node/edge information that is not contained in the local tables
 */
public class AuxiliaryInformation {
    private Map<CyTable, List<Integer>> source_rows;
    private int visits;
    private int frequency;

    public AuxiliaryInformation() {
        this.source_rows = new HashMap<>();
        this.visits = 1;
        this.frequency = 0;
    }

    public void increaseVisits() {
        this.visits += 1;
    }

    public void resetVisits() {
        this.visits = 1;
    }

    public int getVisits() {
        return this.visits;
    }

    public void increaseFrequency() {
        this.frequency += 1;
    }

    public void resetFrequency() {
        this.frequency = 0;
    }

    public int getFrequency() {
        return this.frequency;
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

    public void fixVisitsAndFrequency() {
        int visits = 0;
        int frequency = 0;
        for (var list : this.source_rows.values()) {
            int prev = Integer.MIN_VALUE;
            for (int i : list) {
                if (i == prev + 1) {
                    frequency += 1;
                } else {
                    visits += 1;
                }
                prev = i;
            }
        }
        this.visits = visits;
        this.frequency = frequency;
    }

}
