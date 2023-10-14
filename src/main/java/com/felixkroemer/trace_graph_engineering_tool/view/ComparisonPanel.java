package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkComparisonController;
import org.cytoscape.service.util.CyServiceRegistrar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ComparisonPanel extends JPanel {

    private CyServiceRegistrar reg;
    private JButton tempButton;

    public ComparisonPanel(CyServiceRegistrar reg) {
        this.reg = reg;
        this.tempButton = new JButton();
        this.init();
    }

    public void init() {
        this.setLayout(new BorderLayout());
        this.add(this.tempButton, BorderLayout.CENTER);
    }

    public void setComparisonController(NetworkComparisonController controller) {
        for (ActionListener al : tempButton.getActionListeners()) {
            tempButton.removeActionListener(al);
        }
        this.tempButton.addActionListener(e -> {
            controller.hideBO();
        });
    }

}