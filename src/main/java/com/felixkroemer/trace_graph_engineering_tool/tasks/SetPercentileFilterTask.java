package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.SetPercentileFilterPanel;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;

public class SetPercentileFilterTask extends AbstractTask {

    private NetworkController controller;

    public SetPercentileFilterTask(NetworkController controller) {
        this.controller = controller;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        SwingUtilities.invokeLater(() -> Util.showDialog(new SetPercentileFilterPanel(controller), "Set percentile filter"));
    }
}
