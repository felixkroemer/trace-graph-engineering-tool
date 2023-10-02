package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;

public class RenderingModeTask extends AbstractNetworkViewTask {
    private CyServiceRegistrar reg;
    private String renderingMode;

    public RenderingModeTask(CyServiceRegistrar reg, String mode, CyNetworkView view) {
        super(view);
        this.reg = reg;
        this.renderingMode = mode;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var manager = this.reg.getService(TraceGraphManager.class);
        TraceGraphController controller = manager.findControllerForNetwork(view.getModel());
        controller.setMode(this.renderingMode);
    }

}
