package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingMode;
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
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        return new TaskIterator(new RenderingModeTask(this.reg, this.mode));
    }
}
