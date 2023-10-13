package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.util.Util;
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
        return Util.isTraceGraphNetwork(networkView.getModel());
    }

    @Override
    public TaskIterator createTaskIterator(View<CyEdge> edgeView, CyNetworkView networkView) {

        return new TaskIterator(new ShowTraceDetailsTask(reg, edgeView, networkView));
    }
}
