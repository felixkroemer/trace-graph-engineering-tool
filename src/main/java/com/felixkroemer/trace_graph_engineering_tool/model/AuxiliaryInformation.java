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

    protected List<Pair<CyTable, List<Integer>>> situations;

    public AuxiliaryInformation() {
        this.situations = new LinkedList<>();
    }

    public AuxiliaryInformation(AuxiliaryInformation source) {
        this.situations = new LinkedList<>();
        for (var trace : source.situations) {
            this.situations.add(new Pair<>(trace.getValue0(), new LinkedList<>(trace.getValue1())));
        }
    }

    private List<Integer> getList(CyTable table) {
        for (var x : this.situations) {
            if (x.getValue0() == table) {
                return x.getValue1();
            }
        }
        var list = new LinkedList<Integer>();
        this.situations.add(new Pair<>(table, list));
        return list;
    }

    public void addSituation(CyTable trace, int index) {
        var list = this.getList(trace);
        list.add(index);
    }

    public List<Integer> getSituations(CyTable trace) {
        return this.getList(trace);
    }

    public boolean hasNoSituations() {
        for (var x : this.situations) {
            if (!x.getValue1().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public Set<CyTable> getTraces() {
        return this.situations.stream().map(Pair::getValue0).collect(Collectors.toSet());
    }

    public void removeTrace(CyTable trace) {
        this.situations.removeIf((pair) -> pair.getValue0() == trace);
    }
}
