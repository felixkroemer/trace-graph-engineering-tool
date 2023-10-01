package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkType;
import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingMode;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class RenderingModeTaskFactory extends AbstractNetworkViewTaskFactory {

    private CyServiceRegistrar reg;
    private RenderingMode mode;

    public RenderingModeTaskFactory(CyServiceRegistrar reg, RenderingMode mode) {
        this.reg = reg;
        this.mode = mode;
    }

    @Override
    public boolean isReady(CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networkView.getModel());
        if (controller != null) {
            return controller.getNetworkType(networkView.getModel()) == NetworkType.DEFAULT;
        } else {
            return false;
        }
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        return new TaskIterator(new RenderingModeTask(this.reg, this.mode, networkView));
    }
}
