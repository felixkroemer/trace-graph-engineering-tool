package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewDefaultViewTask extends AbstractTask {

    private CyServiceRegistrar registrar;
    private Logger logger;
    private CyNetworkView networkView;
    private CyNode node;

    public ViewDefaultViewTask(CyServiceRegistrar reg, CyNetworkView networkView, CyNode node) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.registrar = reg;
        this.networkView = networkView;
        this.node = node;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var manager = this.registrar.getService(TraceGraphManager.class);
        //manager.focusNode(this.node);
    }
}
