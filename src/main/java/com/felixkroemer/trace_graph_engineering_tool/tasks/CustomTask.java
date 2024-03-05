package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class CustomTask extends AbstractTask {

    private NetworkController controller;

    public CustomTask(NetworkController controller) {
        this.controller = controller;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        //controller.getNetwork().removeEdges(controller.getNetwork().getEdgeList());
        controller.getNetwork().removeNodes(controller.getNetwork().getNodeList());
    }
}
