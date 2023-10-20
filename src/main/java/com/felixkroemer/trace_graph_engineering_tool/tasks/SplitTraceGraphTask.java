package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.view.SplitTraceGraphDialog;
import com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel.SelectBinsDialog;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkCollectionTask;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;

public class SplitTraceGraphTask extends AbstractNetworkCollectionTask {

    private CyNetwork network;
    private CyServiceRegistrar registrar;

    public SplitTraceGraphTask(Collection<CyNetwork> networks, CyServiceRegistrar registrar) {
        super(networks);
        this.registrar = registrar;
        var iterator = networks.iterator();
        network = iterator.next();
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var manager = registrar.getService(TraceGraphManager.class);
        //TODO: find better solution
        TraceGraphController controller = (TraceGraphController) manager.findControllerForNetwork(network);

        SwingUtilities.invokeLater(() -> {
            SelectBinsDialog d = new SelectBinsDialog();
            d.setTitle("Split TraceGraph");
            d.setPreferredSize(new Dimension(600, 600));
            d.setContentPane(new SplitTraceGraphDialog(controller, registrar));
            d.setModalityType(APPLICATION_MODAL);
            d.showDialog();
        });
    }
}
