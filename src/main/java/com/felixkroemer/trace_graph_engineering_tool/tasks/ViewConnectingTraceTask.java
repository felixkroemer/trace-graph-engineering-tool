package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class ViewConnectingTraceTask extends AbstractTask {
    private CyServiceRegistrar registrar;
    private Logger logger;
    private CyNetworkView networkView;
    private List<CyNode> selectedNodes;

    public ViewConnectingTraceTask(CyServiceRegistrar reg, CyNetworkView networkView) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.registrar = reg;
        this.networkView = networkView;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var network = networkView.getModel();
        var selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        var manager = this.registrar.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(network);
        List<CyNode> trace = new LinkedList<>();


        controller.highlightTrace(trace);
    }
}
