package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTable;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;

public class PDMPanel extends JPanel implements PropertyChangeListener {

    private Logger logger;

    private CustomTreeTable infoTreeTable;

    private JPanel pdmPanel;
    private JPanel resetFilterPanel;
    private JButton resetFilterButton;
    private CyServiceRegistrar reg;
    private NetworkController controller;

    public PDMPanel(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.reg = reg;

        this.infoTreeTable = new CustomTreeTable();

        this.pdmPanel = new JPanel();
        this.pdmPanel.setLayout(new BoxLayout(this.pdmPanel, BoxLayout.Y_AXIS));
        this.resetFilterPanel = new JPanel();
        this.resetFilterButton = new JButton("Reset Filters");
        resetFilterPanel.add(this.resetFilterButton);
        this.init();
    }

    private void initInfoPanel() {
        this.infoTreeTable.setBorder(LookAndFeelUtil.createTitledBorder("Network Information"));
        this.add(this.infoTreeTable, BorderLayout.NORTH);
    }

    private void initPDMPanel() {
        var scrollPane = new JScrollPane(this.pdmPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    private void init() {
        setLayout(new BorderLayout());

        this.initInfoPanel();
        this.initPDMPanel();

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

        this.updatePDMPanel(controller);
        this.updateInfoTreeTable(controller);

        this.controller = controller;
        this.controller.getPDM().addObserver(this);
    }

    private void updatePDMPanel(NetworkController controller) {
        this.pdmPanel.removeAll();
        controller.getPDM().forEach(parameter -> {
            ParameterCell cell = new ParameterCell(reg, parameter, controller);
            parameter.addObserver(cell);
            this.pdmPanel.add(cell);
        });
    }

    private void updateInfoTreeTable(NetworkController controller) {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("Root");
        var model = controller.createNetworkTableModel(root);
        this.infoTreeTable.setModel(model);
    }

    public void clear() {
        this.pdmPanel.removeAll();
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
