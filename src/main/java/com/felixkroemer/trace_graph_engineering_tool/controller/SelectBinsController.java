package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Collection<CyTable> getSourceTables() {
        var manager = registrar.getService(TraceGraphManager.class);
        return manager.getSourceTables(parameter.getPdm());
    }

    public void setNewBins(List<Float> bins) {
        this.parameter.setBins(bins.stream().map(f -> (double) f).collect(Collectors.toList()));
    }

    public void setVisibleBins(Set<Integer> visibleBins) {
        this.parameter.setVisibleBins(visibleBins);
    }
}
