package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.model.CyTable;

import java.util.List;
import java.util.Set;

public class SelectBinsController {
    private Parameter parameter;
    private CyTable sourceTable;

    public SelectBinsController(Parameter parameter, CyTable sourceTable) {
        this.parameter = parameter;
        this.sourceTable = sourceTable;
    }

    public Parameter getParameter() {
        return this.parameter;
    }

    public CyTable getSourceTable() {
        return this.sourceTable;
    }

    public void setNewBins(List<Float> bins) {
        this.parameter.setBins(bins.stream().map(f -> (double) f).toList());
    }

    public void setNewHighlightedBins(Set<Integer> highlightedBins) {
        this.parameter.highlightBins(highlightedBins);
    }

    public Set<Integer> getHighlightedBins() {
        return this.parameter.getHighlightedBins();
    }

}
