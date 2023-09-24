package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingMode;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;

public class RenderingModeTask extends AbstractNetworkViewTask {
    private CyServiceRegistrar reg;
    private RenderingMode mode;
    private Logger logger;

    public RenderingModeTask(CyServiceRegistrar reg, RenderingMode mode, CyNetworkView view) {
        super(view);
        this.reg = reg;
        this.mode = mode;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var manager = this.reg.getService(TraceGraphManager.class);
        TraceGraphController controller = manager.findControllerForNetwork(view.getModel());
        controller.setMode(this.mode);
    }

}
