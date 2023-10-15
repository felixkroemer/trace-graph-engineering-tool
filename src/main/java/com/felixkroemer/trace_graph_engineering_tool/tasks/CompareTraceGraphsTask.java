package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkComparisonController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.Columns;
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
        var pdm = manager.findPDMForNetwork(networkA);
        var rootNetwork = pdm.getRootNetwork();
        var subNetwork = rootNetwork.addSubNetwork();

        var localNetworkTable = subNetwork.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
        localNetworkTable.createColumn(Columns.NETWORK_COMPARISON_MARKER, Integer.class, true);

        NetworkComparisonController networkComparisonController = new NetworkComparisonController(networkA, networkB,
                subNetwork, registrar);
        manager.registerTraceGraph(pdm, networkComparisonController);
    }
}
