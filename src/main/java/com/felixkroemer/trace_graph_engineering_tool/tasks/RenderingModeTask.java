package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingMode;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;

public class RenderingModeTask extends AbstractTask {
    private CyServiceRegistrar reg;
    private RenderingMode mode;
    private Logger logger;

    public RenderingModeTask(CyServiceRegistrar reg, RenderingMode mode) {
        this.reg = reg;
        this.mode = mode;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var controller = this.reg.getService(TraceGraphController.class);
        controller.setMode(this.mode);
    }
    
}
