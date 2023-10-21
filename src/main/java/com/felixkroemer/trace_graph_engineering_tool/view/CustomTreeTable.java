package com.felixkroemer.trace_graph_engineering_tool.view;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CustomTreeTable extends JPanel {
    private JXTreeTable table;

    public CustomTreeTable() {
        setLayout(new BorderLayout());
        this.table = new JXTreeTable();
        this.table.setTableHeader(null);
        this.table.setLeafIcon(null);
        this.table.setClosedIcon(null);
        this.table.setOpenIcon(null);
        this.table.setBackground(UIManager.getColor("Panel.background"));
        this.add(new JScrollPane(this.table), BorderLayout.CENTER);
    }

    public void setModel(TreeTableModel model) {
        this.table.setTreeTableModel(model);
        if (model.getColumnCount() > 1) {
            DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
            table.getColumn(1).setCellRenderer(rightRenderer);
            rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        }
    }


}
