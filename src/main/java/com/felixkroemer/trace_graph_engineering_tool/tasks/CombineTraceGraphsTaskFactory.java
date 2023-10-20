package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.work.TaskIterator;

import java.util.Collection;

public class CombineTraceGraphsTaskFactory implements NetworkCollectionTaskFactory {
    private CyServiceRegistrar reg;

    public CombineTraceGraphsTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public TaskIterator createTaskIterator(Collection<CyNetwork> networks) {
        return new TaskIterator(new CombineTraceGraphsTask(networks, this.reg));
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
        var pdmA = manager.findPDMForNetwork(networkA);
        var pdmB = manager.findPDMForNetwork(networkB);

        if (pdmA != null && pdmB != null && pdmA == pdmB) {
            var controllerA = manager.findControllerForNetwork(networkA);
            var controllerB = manager.findControllerForNetwork(networkB);
            return controllerA instanceof TraceGraphController && controllerB instanceof TraceGraphController;
        }
        return false;
    }
}
