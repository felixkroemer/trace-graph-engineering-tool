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

    public AuxiliaryInformation(AuxiliaryInformation source) {
        this.source_rows = new LinkedList<>();
        for (var trace : source.source_rows) {
            this.source_rows.add(new Pair<>(trace.getValue0(), new LinkedList<>(trace.getValue1())));
        }
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

    public void addSituation(CyTable trace, int index) {
        var list = this.getList(trace);
        list.add(index);
    }

    public List<Integer> getSourceRows(CyTable trace) {
        return this.getList(trace);
    }

    public boolean hasNoSourceRows() {
        for (var x : this.source_rows) {
            if (!x.getValue1().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public Set<CyTable> getTraces() {
        return this.source_rows.stream().map(Pair::getValue0).collect(Collectors.toSet());
    }

    public void removeTrace(CyTable trace) {
        this.source_rows.removeIf((pair) -> pair.getValue0() == trace);
    }
}
