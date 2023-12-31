package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;

import java.util.List;
import java.util.Set;

public class SelectBinsController {

    private CyServiceRegistrar registrar;
    private Parameter parameter;

    public SelectBinsController(Parameter parameter, CyServiceRegistrar registrar) {
        this.registrar = registrar;
        this.parameter = parameter;
    }

    public Parameter getParameter() {
        return this.parameter;
    }

    public CyTable getSourceTable() {
        var manager = registrar.getService(TraceGraphManager.class);
        var tables = manager.getSourceTables(parameter.getPdm());
        return tables.iterator().next();
    }

    public void setNewBins(List<Float> bins) {
        this.parameter.setBins(bins.stream().map(f -> (double) f).toList());
    }

    public void setVisibleBins(Set<Integer> visibleBins) {
        this.parameter.setVisibleBins(visibleBins);
    }
}
