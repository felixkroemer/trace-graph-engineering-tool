package com.felixkroemer.trace_graph_engineering_tool.tasks;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResetTask extends AbstractTask {
    private CyServiceRegistrar reg;
    private Logger logger;

    public ResetTask(CyServiceRegistrar reg) {
        this.reg = reg;
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        CyNetworkManager manager = reg.getService(CyNetworkManager.class);
        for (CyNetwork network : manager.getNetworkSet()) {
            manager.destroyNetwork(network);
        }
    }
}
