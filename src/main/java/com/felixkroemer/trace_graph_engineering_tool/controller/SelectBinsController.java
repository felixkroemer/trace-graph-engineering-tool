package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.UIState;
import org.cytoscape.model.CyTable;

import java.util.List;
import java.util.Set;

public class SelectBinsController {
    private Parameter parameter;
    private UIState uiState;
    private CyTable sourceTable;

    public SelectBinsController(Parameter parameter, UIState uiState, CyTable sourceTable) {
        this.parameter = parameter;
        this.uiState = uiState;
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
        this.uiState.highlightBins(parameter, highlightedBins);
    }

    public Set<Integer> getHighlightedBins() {
        return this.uiState.getHighlightedBins(this.parameter);
    }

}
