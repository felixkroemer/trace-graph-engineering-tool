package com.felixkroemer.trace_graph_engineering_tool.tasks;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class ExtractTraceTask extends AbstractTask {

    private CyServiceRegistrar registrar;

    public ExtractTraceTask(CyServiceRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {

    }
}
