package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ExtractTaskTaskFactory extends AbstractTaskFactory {
    private CyServiceRegistrar reg;
    private TraceGraphController traceGraphController;

    public ExtractTaskTaskFactory(CyServiceRegistrar reg, TraceGraphController controller) {
        this.reg = reg;
        this.traceGraphController = controller;
    }


    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new ExtractTraceTask(reg));
    }
}
