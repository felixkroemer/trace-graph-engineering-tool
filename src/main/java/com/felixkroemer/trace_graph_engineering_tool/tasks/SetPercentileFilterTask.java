package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import com.felixkroemer.trace_graph_engineering_tool.view.SetPercentileFilterDialog;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;

public class SetPercentileFilterTask extends AbstractTask {

    private CyServiceRegistrar registrar;
    private NetworkController controller;

    public SetPercentileFilterTask(CyServiceRegistrar registrar, NetworkController controller) {
        this.registrar = registrar;
        this.controller = controller;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        SwingUtilities.invokeLater(() -> {
            JDialog d = new JDialog();
            d.setTitle("Set Percentile Filter");
            d.setContentPane(new SetPercentileFilterDialog(registrar, controller));
            d.setModalityType(APPLICATION_MODAL);
            d.pack();
            d.setLocationRelativeTo(null);
            d.setVisible(true);
            d.setResizable(false);
        });
    }
}
