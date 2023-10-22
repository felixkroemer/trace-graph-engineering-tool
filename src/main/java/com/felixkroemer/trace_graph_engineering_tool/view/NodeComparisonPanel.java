package com.felixkroemer.trace_graph_engineering_tool.view;


import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collection;
import java.util.Set;

public class NodeComparisonPanel extends JPanel {

    private CyServiceRegistrar reg;
    private Set<CyNode> nodes;

    private JXTable comparisonTable;

    public NodeComparisonPanel(CyServiceRegistrar reg) {
        this.reg = reg;
        this.comparisonTable = new JXTable();

        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());

        this.comparisonTable.getTableHeader().setUI(null);
        this.comparisonTable.setRowSelectionAllowed(false);
        this.comparisonTable.setColumnSelectionAllowed(false);
        this.comparisonTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.comparisonTable.setBackground(Color.MAGENTA);
        this.comparisonTable.setGridColor(Color.GRAY);
        this.comparisonTable.setTableHeader(null);
        this.comparisonTable.setBackground(UIManager.getColor("Panel.background"));
        this.setBackground(UIManager.getColor("Panel.background"));

        this.add(new JScrollPane(comparisonTable), BorderLayout.CENTER);
    }

    public void setNodes(NetworkController controller, Collection<CyNode> nodes) {
        var pdm = controller.getPDM();
        var model = new TableModel(nodes.size() + 1);

        var header = new String[nodes.size() + 1];
        for (int i = 1; i <= nodes.size(); i++) {
            header[i] = "" + i;
        }
        model.addRow(header);

        var table = controller.getNetwork().getDefaultNodeTable();

        for (var param : pdm.getParameters()) {
            String[] row = new String[nodes.size() + 1];
            row[0] = param.getName();
            int i = 1;
            for (CyNode node : nodes) {
                row[i] = "" + table.getRow(node.getSUID()).get(param.getName(), Integer.class);
                i++;
            }
            model.addRow(row);
        }

        this.comparisonTable.setModel(model);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        TableModel tableModel = (TableModel) comparisonTable.getModel();

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            this.comparisonTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        this.comparisonTable.packAll();
    }

}
