package com.felixkroemer.trace_graph_engineering_tool.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ResetTaskFactory extends AbstractTaskFactory {

    private CyServiceRegistrar reg;

    public ResetTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new ResetTask(this.reg));
    }
}
