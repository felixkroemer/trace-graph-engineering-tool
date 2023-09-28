package com.felixkroemer.trace_graph_engineering_tool.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class ViewConnectingTraceNetworkTaskFactory extends AbstractNetworkViewTaskFactory {

    private CyServiceRegistrar reg;

    public ViewConnectingTraceNetworkTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public boolean isReady(CyNetworkView networkView) {
        var network = networkView.getModel();
        var selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        if (selectedNodes.size() >= 2) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        return new TaskIterator(new ViewConnectingTraceTask(reg, networkView));
    }
}
