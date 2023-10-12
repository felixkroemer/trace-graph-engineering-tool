package com.felixkroemer.trace_graph_engineering_tool.controller;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.*;

public abstract class NetworkController {

    protected CyServiceRegistrar registrar;

    public NetworkController(CyServiceRegistrar registrar) {
        this.registrar = registrar;
    }

    public abstract CyNetwork getNetwork();

    public abstract CyNetworkView getView();

    public abstract VisualStyle getVisualStyle();

    public TaskIterator createLayoutTask() {
        var layoutManager = registrar.getService(CyLayoutAlgorithmManager.class);
        // available as preinstalled app
        CyLayoutAlgorithm layoutFactory = layoutManager.getLayout("force-directed-cl");
        Object context = layoutFactory.getDefaultLayoutContext();
        return layoutFactory.createTaskIterator(this.getView(), context, CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
    }

    public void registerNetwork() {
        var networkManager = registrar.getService(CyNetworkManager.class);
        networkManager.addNetwork(this.getNetwork());
        var networkViewManager = registrar.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(this.getView());
        this.applyStyleAndLayout();
    }

    public void applyStyleAndLayout() {
        TaskIterator iterator = new TaskIterator();
        iterator.append(new AbstractTask() {
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                getVisualStyle().apply(getView());
            }
        });
        iterator.append(this.createLayoutTask());
        var taskManager = this.registrar.getService(TaskManager.class);
        taskManager.execute(iterator);
    }

}
