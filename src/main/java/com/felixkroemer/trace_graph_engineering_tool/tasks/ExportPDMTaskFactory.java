package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class ExportPDMTaskFactory extends AbstractNetworkViewTaskFactory {

    private CyServiceRegistrar reg;

    public ExportPDMTaskFactory(CyServiceRegistrar reg) {
        this.reg = reg;
    }

    @Override
    public boolean isReady(CyNetworkView networkView) {

        return networkView != null && Util.isTraceGraphNetwork(networkView.getModel());
    }

    @Override
    public TaskIterator createTaskIterator(CyNetworkView networkView) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var pdm = manager.findPDMForNetwork(networkView.getModel());
        return new TaskIterator(new ExportPDMTask(pdm));
    }
}
