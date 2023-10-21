package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.jdesktop.swingx.JXTreeTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;

public class PDMPanel extends JPanel implements PropertyChangeListener {

    private Logger logger;

    private JPanel infoPanel;
    private JXTreeTable table;


    private JScrollPane scrollPane;
    private JPanel innerPanel;
    private JPanel resetFilterPanel;
    private JButton resetFilterButton;
    private CyServiceRegistrar reg;
    private NetworkController controller;

    public PDMPanel(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.reg = reg;

        this.infoPanel = new JPanel();
        this.innerPanel = new JPanel();
        this.innerPanel.setLayout(new BoxLayout(this.innerPanel, BoxLayout.Y_AXIS));
        this.scrollPane = new JScrollPane(this.innerPanel);
        this.resetFilterPanel = new JPanel();
        this.resetFilterButton = new JButton("Reset Filters");
        resetFilterPanel.add(this.resetFilterButton);
        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());
        this.add(this.scrollPane, BorderLayout.CENTER);
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        this.resetFilterButton.addActionListener(e -> {
            this.controller.getPDM().getParameters().forEach(p -> {
                if (!p.getVisibleBins().isEmpty()) {
                    p.setVisibleBins(Collections.emptySet());
                }
            });
        });
    }

    public void registerCallbacks(NetworkController controller) {
        if (this.controller != null) {
            this.controller.getPDM().removeObserver(this);
        }
        controller.getPDM().addObserver(this);
        SwingUtilities.invokeLater(() -> {
            this.innerPanel.removeAll();
            controller.getPDM().forEach(parameter -> {
                ParameterCell cell = new ParameterCell(reg, parameter, controller);
                parameter.addObserver(cell);
                this.innerPanel.add(cell);
            });
        });
        this.controller = controller;

    }

    public void clear() {
        this.innerPanel.removeAll();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "filtered" -> {
                if ((boolean) evt.getNewValue()) {
                    this.add(this.resetFilterPanel, BorderLayout.SOUTH);
                } else {
                    this.remove(this.resetFilterPanel);
                }
                this.revalidate();
            }
        }
    }
}
