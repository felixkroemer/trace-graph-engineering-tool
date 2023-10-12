package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.work.TaskIterator;

import java.util.Collection;

public class CompareTraceGraphsTaskFactory implements NetworkCollectionTaskFactory {
    private CyServiceRegistrar reg;

    public CompareTraceGraphsTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public TaskIterator createTaskIterator(Collection<CyNetwork> networks) {
        return new TaskIterator(new CompareTraceGraphsTask(networks, this.reg));
    }

    @Override
    public boolean isReady(Collection<CyNetwork> networks) {
        var manager = this.reg.getService(TraceGraphManager.class);

        if (networks.size() != 2) {
            return false;
        }

        var iterator = networks.iterator();
        var networkA = iterator.next();
        var networkB = iterator.next();
        var controllerA = manager.findControllerForNetwork(networkA);
        var controllerB = manager.findControllerForNetwork(networkB);

        if (controllerA == null || controllerB == null) {
            return false;
        } else {
            if (controllerA.getTraceGraph().getPDM() != controllerB.getTraceGraph().getPDM()) {
                return false;
            }
        }
        return true;
    }
}
