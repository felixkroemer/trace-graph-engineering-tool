package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkType;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractEdgeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

public class ShowTraceDetailsEdgeTaskFactory extends AbstractEdgeViewTaskFactory {

    private CyServiceRegistrar reg;

    public ShowTraceDetailsEdgeTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public boolean isReady(View<CyEdge> nodeView, CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networkView.getModel());
        if (controller != null) {
            return controller.getNetworkType(networkView.getModel()) == NetworkType.DEFAULT;
        } else {
            return false;
        }
    }

    @Override
    public TaskIterator createTaskIterator(View<CyEdge> nodeView, CyNetworkView networkView) {
        return new TaskIterator(new ShowTraceDetailsTask(reg, nodeView, networkView));
    }
}