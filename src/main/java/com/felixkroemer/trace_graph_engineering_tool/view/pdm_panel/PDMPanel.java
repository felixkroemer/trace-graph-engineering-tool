package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class PDMPanel extends TraceGraphPanel implements PropertyChangeListener {

    private CustomTreeTable infoTreeTable;
    private JPanel pdmPanel;
    private JPanel resetFilterPanel;
    private JButton resetFilterButton;
    private JLabel hiddenNodesCountLabel;
    private CyServiceRegistrar reg;
    private NetworkController controller;

    public PDMPanel(CyServiceRegistrar reg) {
        this.reg = reg;

        this.infoTreeTable = new CustomTreeTable();

        this.pdmPanel = new JPanel();
        this.pdmPanel.setLayout(new BoxLayout(this.pdmPanel, BoxLayout.Y_AXIS));
        this.resetFilterPanel = new JPanel();
        this.resetFilterButton = new JButton("Reset Filters");
        this.hiddenNodesCountLabel = new JLabel();
        this.resetFilterPanel.add(this.resetFilterButton);
        this.resetFilterPanel.add(this.hiddenNodesCountLabel);
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

        this.resetFilterButton.addActionListener(e -> this.controller.getPDM().resetFilters());
    }

    public void registerCallbacks(NetworkController controller) {
        if (this.controller != null && this.controller instanceof TraceGraphController tgc) {
            tgc.getFilteredState().removeObserver(this);
        }

        this.controller = controller;

        this.updatePDMPanel();
        this.updateInfoTreeTable();
        this.updateResetPanel();

        if (this.controller instanceof TraceGraphController tgc) {
            tgc.getFilteredState().addObserver(this);
        }
    }

    private void updatePDMPanel() {
        this.pdmPanel.removeAll();
        this.controller.getPDM().forEach(parameter -> {
            ParameterCell cell = new ParameterCell(reg, parameter, controller);
            parameter.addObserver(cell);
            this.pdmPanel.add(cell);
        });
    }

    private void updateInfoTreeTable() {
        var model = this.controller.createNetworkTableModel();
        this.infoTreeTable.setModel(model);
    }

    private void updateResetPanel() {
        if (this.controller instanceof TraceGraphController tgc) {
            var filteredState = tgc.getFilteredState();
            if (filteredState.getHiddenNodeCount() != 0) {
                var visibleNodeCount = filteredState.getTotalNodeCount() - filteredState.getHiddenNodeCount();
                this.hiddenNodesCountLabel.setText(visibleNodeCount + " / " + filteredState.getTotalNodeCount() + " " + "nodes shown");
                this.add(this.resetFilterPanel, BorderLayout.SOUTH);
            } else {
                this.remove(this.resetFilterPanel);
            }
        } else {
            this.remove(this.resetFilterPanel);
        }
        this.revalidate();
    }

    public void clear() {
        this.pdmPanel.removeAll();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("filteredState")) {
            this.updateResetPanel();
        }
    }

    @Override
    public String getTitle() {
        return "PDM";
    }
}
