package com.felixkroemer.trace_graph_engineering_tool.view;

import org.cytoscape.service.util.CyServiceRegistrar;

import javax.swing.*;

public class TracesPanel extends JPanel {

    private CyServiceRegistrar reg;

    public TracesPanel(CyServiceRegistrar reg) {
        this.reg = reg;
    }
}
