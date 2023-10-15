package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class PDMPanel extends JPanel {

    private Logger logger;

    private JScrollPane scrollPane;
    private JPanel innerPanel;
    private CyServiceRegistrar reg;

    public PDMPanel(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.reg = reg;

        this.innerPanel = new JPanel();
        this.innerPanel.setLayout(new BoxLayout(this.innerPanel, BoxLayout.Y_AXIS));
        this.scrollPane = new JScrollPane(this.innerPanel);
        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());
        this.add(this.scrollPane, BorderLayout.CENTER);
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    public void registerCallbacks(NetworkController controller) {
        SwingUtilities.invokeLater(() -> {
            this.innerPanel.removeAll();
            controller.getPDM().forEach(parameter -> {
                ParameterCell cell = new ParameterCell(reg, parameter, controller);
                parameter.addObserver(cell);
                this.innerPanel.add(cell);
            });
        });
    }

    public void clear() {
        this.innerPanel.removeAll();
    }
}
