package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

import static com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController.NETWORK_TYPE_DEFAULT;

public class ShowTraceNodeTaskFactory extends AbstractNodeViewTaskFactory {

    private CyServiceRegistrar reg;

    public ShowTraceNodeTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(networkView.getModel());
        if (controller != null) {
            var selectedNodes = CyTableUtil.getNodesInState(networkView.getModel(), CyNetwork.SELECTED, true);
            if (selectedNodes.size() == 2) {
                return controller.getNetworkType(networkView.getModel()).equals(NETWORK_TYPE_DEFAULT);
            }
        }
        return false;
    }

    @Override
    public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
        return new TaskIterator(new ShowTraceTask(reg, nodeView, networkView));
    }
}
