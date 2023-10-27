package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkCollectionTask;
import org.cytoscape.work.TaskMonitor;

import java.util.Collection;

public class CombineTraceGraphsTask extends AbstractNetworkCollectionTask {

    private TraceGraphController controllerA;
    private TraceGraphController controllerB;
    private CyServiceRegistrar registrar;

    public CombineTraceGraphsTask(Collection<CyNetwork> networks, CyServiceRegistrar registrar) {
        super(networks);
        this.registrar = registrar;
        var manager = registrar.getService(TraceGraphManager.class);
        var iterator = networks.iterator();
        var networkA = iterator.next();
        var networkB = iterator.next();
        this.controllerA = (TraceGraphController) manager.findControllerForNetwork(networkA);
        this.controllerB = (TraceGraphController) manager.findControllerForNetwork(networkB);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var manager = registrar.getService(CyApplicationManager.class);
        if (manager.getCurrentNetwork() == controllerA.getNetwork()) {
            controllerA.mergeTraceGraph(controllerB);
        } else if (manager.getCurrentNetwork() == controllerB.getNetwork()) {
            controllerB.mergeTraceGraph(controllerA);
        } else {
            controllerA.mergeTraceGraph(controllerB);
        }
    }
}
