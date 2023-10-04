package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

import static com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController.NETWORK_TYPE_DEFAULT;

public class ShowTraceDetailsNodeTaskFactory extends AbstractNodeViewTaskFactory {

    private CyServiceRegistrar reg;

    public ShowTraceDetailsNodeTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networkView.getModel());
        if (controller != null) {
            return controller.getNetworkType(networkView.getModel()).equals(NETWORK_TYPE_DEFAULT) && controller.getUiState().getTraceSet() != null;
        } else {
            return false;
        }
    }

    @Override
    public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
        return new TaskIterator(new ShowTraceDetailsTask(reg, nodeView, networkView));
    }
}
