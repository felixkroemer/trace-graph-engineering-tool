package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import static com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController.NETWORK_TYPE_DEFAULT;

public class RenderingModeTaskFactory extends AbstractNetworkViewTaskFactory {

    private CyServiceRegistrar reg;
    private String renderingMode;

    public RenderingModeTaskFactory(CyServiceRegistrar reg, String mode) {
        this.reg = reg;
        this.renderingMode = mode;
    }

    @Override
    public boolean isReady(CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networkView.getModel());
        if (controller != null) {
            return controller.getNetworkType(networkView.getModel()).equals(NETWORK_TYPE_DEFAULT);
        } else {
            return false;
        }
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        return new TaskIterator(new RenderingModeTask(this.reg, this.renderingMode, networkView));
    }
}
