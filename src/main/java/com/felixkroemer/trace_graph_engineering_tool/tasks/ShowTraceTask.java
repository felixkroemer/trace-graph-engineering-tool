package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.DrawableSubtrace;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;

public class ShowTraceTask extends AbstractTask {

    private CyServiceRegistrar registrar;
    private CyNetwork network;

    public ShowTraceTask(CyServiceRegistrar reg, CyNetwork network) {
        this.registrar = reg;
        this.network = network;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        CyEventHelper helper = registrar.getService(CyEventHelper.class);

        var manager = registrar.getService(TraceGraphManager.class);
        var controller = (TraceGraphController) manager.findControllerForNetwork(network);

        if (controller != null) {
            var traceGraph = controller.getTraceGraph();
            var trace = traceGraph.findMinimalSubtrace(nodes);
            if (trace != null) {
                DrawableSubtrace extension = new DrawableSubtrace(trace, traceGraph, Color.BLACK);
                helper.fireEvent(new ShowTraceEvent(this, extension, network));
            } else {
                new Thread(() -> {
                    JOptionPane.showMessageDialog(null, "No Subtrace found that contains all nodes", null, JOptionPane.ERROR_MESSAGE);
                }).start();
            }
        }
    }
}
