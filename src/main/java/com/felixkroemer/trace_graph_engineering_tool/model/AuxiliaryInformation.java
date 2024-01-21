package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyTable;
import org.javatuples.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Auxiliary node/edge information that is not contained in the local tables
 */
public abstract class AuxiliaryInformation {

    protected List<Pair<CyTable, List<Integer>>> source_rows;

    public AuxiliaryInformation() {
        this.source_rows = new LinkedList<>();
    }

    private List<Integer> getList(CyTable table) {
        for (var x : this.source_rows) {
            if (x.getValue0() == table) {
                return x.getValue1();
            }
        }
        var list = new LinkedList<Integer>();
        this.source_rows.add(new Pair<>(table, list));
        return list;
    }

    public void addSourceRow(CyTable sourceTable, int index) {
        var list = this.getList(sourceTable);
        list.add(index);
    }

    public List<Integer> getSourceRows(CyTable sourceTable) {
        return this.getList(sourceTable);
    }

    public boolean hasNoSourceRows() {
        for (var x : this.source_rows) {
            if (!x.getValue1().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public Set<CyTable> getSourceTables() {
        return this.source_rows.stream().map(Pair::getValue0).collect(Collectors.toSet());
    }
}
