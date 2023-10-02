package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import static com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController.NETWORK_TYPE_TRACE_DETAILS;

public class ViewDefaultViewNetworkTaskFactory extends AbstractNetworkViewTaskFactory {

    private CyServiceRegistrar reg;

    public ViewDefaultViewNetworkTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public boolean isReady(CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networkView.getModel());
        if (controller != null) {
            return controller.getNetworkType(networkView.getModel()).equals(NETWORK_TYPE_TRACE_DETAILS);
        } else {
            return false;
        }
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        return new TaskIterator(new ViewDefaultViewTask(reg, networkView, null));
    }
}
