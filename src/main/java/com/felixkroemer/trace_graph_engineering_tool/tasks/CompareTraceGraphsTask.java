package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkComparisonController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkCollectionTask;
import org.cytoscape.work.TaskMonitor;

import java.util.Collection;

public class CompareTraceGraphsTask extends AbstractNetworkCollectionTask {

    private CyNetwork networkA;
    private CyNetwork networkB;
    private CyServiceRegistrar registrar;

    public CompareTraceGraphsTask(Collection<CyNetwork> networks, CyServiceRegistrar registrar) {
        super(networks);
        this.registrar = registrar;
        var iterator = networks.iterator();
        networkA = iterator.next();
        networkB = iterator.next();
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var manager = this.registrar.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(this.networkA);
        var pdm = controller.getTraceGraph().getPDM();
        var rootNetwork = pdm.getRootNetwork();
        var network = rootNetwork.addSubNetwork();
        NetworkComparisonController networkComparisonController =
                new NetworkComparisonController(networkA, networkB, network, registrar);
        networkComparisonController.registerNetwork();
    }
}
