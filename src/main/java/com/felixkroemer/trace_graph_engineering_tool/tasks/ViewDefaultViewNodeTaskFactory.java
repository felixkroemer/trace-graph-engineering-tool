package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkType;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

public class ViewDefaultViewNodeTaskFactory extends AbstractNodeViewTaskFactory {

    private CyServiceRegistrar reg;

    public ViewDefaultViewNodeTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networkView.getModel());
        return controller.getNetworkType(networkView.getModel()) == NetworkType.TRACE_DETAILS;
    }

    @Override
    public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
        return new TaskIterator(new ViewDefaultViewTask(reg, networkView, nodeView.getModel()));
    }
}
