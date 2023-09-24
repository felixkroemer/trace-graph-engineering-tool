package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.display_manager.Trace;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ShowTraceDetailsTask extends AbstractTask {
    private CyServiceRegistrar reg;
    private Logger logger;

    private Set<Trace> traces;

    public ShowTraceDetailsTask(CyServiceRegistrar reg, Set<Trace> traceSet) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.reg = reg;
        this.traces = traceSet;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyNetworkFactory networkFactory = reg.getService(CyNetworkFactory.class);
        var network = networkFactory.createNetwork();
        CyNetworkManager networkManager = reg.getService(CyNetworkManager.class);
        networkManager.addNetwork(network);
        CyNetworkViewFactory networkViewFactory = reg.getService(CyNetworkViewFactory.class);
        var view = networkViewFactory.createNetworkView(network);
        CyNetworkViewManager networkViewManager = reg.getService(CyNetworkViewManager.class);
        networkViewManager.addNetworkView(view);

        for (var trace : traces) {
            CyNode prevTraceNode = null;
            CyNode prevNode = null;
            for (var node : trace.getSequence()) {
                if (prevNode == null || node.getValue0() != prevNode) {
                    var traceNode = network.addNode();
                    if (prevTraceNode != null) {
                        network.addEdge(prevTraceNode, traceNode, true);
                    }
                    prevTraceNode = traceNode;
                }
                prevNode = node.getValue0();
            }
        }

    }

}
