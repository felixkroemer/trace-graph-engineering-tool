package com.felixkroemer.trace_graph_engineering_tool.controller;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

public abstract class NetworkController {

    protected CyServiceRegistrar registrar;

    public NetworkController(CyServiceRegistrar registrar) {
        this.registrar = registrar;
    }

    public abstract CyNetwork getNetwork();

    public abstract CyNetworkView getView();

    public void registerNetwork() {
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(this.getNetwork());
        var networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(this.getView());
        this.applyStyleAndLayout();
    }

    public abstract void applyStyleAndLayout();

}
