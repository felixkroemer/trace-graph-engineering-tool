package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.service.util.CyServiceRegistrar;

import java.util.LinkedList;
import java.util.List;

public class TraceGraphController {

    private List<TraceGraph> networks;
    private CyServiceRegistrar registrar;

    public TraceGraphController(CyServiceRegistrar registrar) {
        this.networks = new LinkedList<>();
        this.registrar = registrar;
    }

}
