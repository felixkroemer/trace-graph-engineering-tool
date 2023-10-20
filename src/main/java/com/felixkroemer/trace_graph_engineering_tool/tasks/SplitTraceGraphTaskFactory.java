package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.work.TaskIterator;

import java.util.Collection;

public class SplitTraceGraphTaskFactory implements NetworkCollectionTaskFactory {
    private CyServiceRegistrar reg;

    public SplitTraceGraphTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public TaskIterator createTaskIterator(Collection<CyNetwork> networks) {
        return new TaskIterator(new SplitTraceGraphTask(networks, this.reg));
    }

    @Override
    public boolean isReady(Collection<CyNetwork> networks) {
        if (networks.size() != 1) {
            return false;
        }

        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networks.iterator().next());

        if (manager.getSourceTables(controller.getPDM()).size() <= 1) {
            return false;
        }

        return controller instanceof TraceGraphController;
    }
}
