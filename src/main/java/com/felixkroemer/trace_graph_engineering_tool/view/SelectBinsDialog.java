package com.felixkroemer.trace_graph_engineering_tool.view;

import org.cytoscape.application.CyUserLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class SelectBinsDialog extends JDialog {

    public SelectBinsDialog() {
        this.logger = LoggerFactory.getLogger(CyUserLog.class);
    }

    private Logger logger;

    public void showDialog() {
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

}
