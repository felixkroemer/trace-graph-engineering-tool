package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;

import java.util.LinkedList;
import java.util.List;

public class TraceGraphController {

    private List<TraceGraph> networks;
    private CyServiceRegistrar registrar;
    private CyNetworkManager networkManager;

    public TraceGraphController(CyServiceRegistrar registrar) {
        this.networks = new LinkedList<>();
        this.registrar = registrar;
        this.networkManager = registrar.getService(CyNetworkManager.class);
    }

    public void registerTraceGraph(TraceGraph tg) {
        networkManager.addNetwork(tg.getNetwork());
    }

}
