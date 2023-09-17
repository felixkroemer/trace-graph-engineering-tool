package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ManualTask extends AbstractTask {
    private CyServiceRegistrar reg;
    private Logger logger;

    public ManualTask(CyServiceRegistrar reg) {
        this.reg = reg;
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        TraceGraphController controller = reg.getService(TraceGraphController.class);
        TraceGraph traceGraph = controller.getActiveTraceGraph();
        controller.onBinsChanged(traceGraph.getPDM().getParameter("speed"), List.of(30.0));
    }
}
