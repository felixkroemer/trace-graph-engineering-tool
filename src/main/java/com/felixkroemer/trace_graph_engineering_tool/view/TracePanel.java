package com.felixkroemer.trace_graph_engineering_tool.view;


import org.cytoscape.service.util.CyServiceRegistrar;

import javax.swing.*;
import java.awt.*;

public class TracePanel extends TraceGraphPanel {

    private CyServiceRegistrar reg;

    private JPanel returnToPreviousModePanel;
    private JButton returnToPreviousModeButton;

    public TracePanel(CyServiceRegistrar reg) {
        this.reg = reg;
        this.returnToPreviousModePanel = new JPanel();
        this.returnToPreviousModeButton = new JButton("Show Trace");

        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());
        this.setBackground(UIManager.getColor("Panel.background"));

        returnToPreviousModePanel.add(this.returnToPreviousModeButton);
        this.add(this.returnToPreviousModePanel, BorderLayout.SOUTH);
    }

    @Override
    public String getTitle() {
        return "Trace";
    }
}
