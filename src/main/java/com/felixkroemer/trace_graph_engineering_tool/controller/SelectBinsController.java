package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.model.CyTable;

import java.util.List;
import java.util.Set;

public class SelectBinsController {
    private Parameter parameter;

    public SelectBinsController(Parameter parameter) {
        this.parameter = parameter;
    }

    public Parameter getParameter() {
        return this.parameter;
    }

    public CyTable getSourceTable() {
        return this.parameter.getPdm().getSourceTables().iterator().next();
    }

    public void setNewBins(List<Float> bins) {
        this.parameter.setBins(bins.stream().map(f -> (double) f).toList());
    }

    public void setVisibleBins(Set<Integer> visibleBins) {
        this.parameter.setVisibleBins(visibleBins);
    }

}
