package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class RenderingModeTaskFactory extends AbstractNetworkViewTaskFactory {

    private CyServiceRegistrar reg;
    private String renderingMode;

    public RenderingModeTaskFactory(CyServiceRegistrar reg, String mode) {
        this.reg = reg;
        this.renderingMode = mode;
    }

    @Override
    public boolean isReady(CyNetworkView networkView) {
        if (networkView != null) {
            return Util.isTraceGraphNetwork(networkView.getModel());
        } else {
            return false;
        }
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        return new TaskIterator(new RenderingModeTask(this.reg, this.renderingMode, networkView));
    }
}
