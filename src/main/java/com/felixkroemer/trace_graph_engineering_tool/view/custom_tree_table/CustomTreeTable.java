package com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CustomTreeTable extends JPanel {
    private JXTreeTable table;
    private JScrollPane scrollPane;

    public CustomTreeTable() {
        setLayout(new BorderLayout());
        this.table = new JXTreeTable();
        this.table.setTableHeader(null);
        this.table.setLeafIcon(null);
        this.table.setClosedIcon(null);
        this.table.setOpenIcon(null);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        this.table.setBackground(UIManager.getColor("Panel.background"));
        this.scrollPane = new JScrollPane(this.table);
        this.add(this.scrollPane, BorderLayout.CENTER);
    }

    public void setModel(TreeTableModel model) {
        if (model == null) {
            this.table.setTreeTableModel(new DefaultTreeTableModel());
            return;
        }
        this.table.setTreeTableModel(model);
        this.table.expandAll();
        if (model.getColumnCount() > 1) {
            DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
            table.getColumn(1).setCellRenderer(rightRenderer);
            rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        }
        this.table.packAll();
        Dimension d = table.getPreferredSize();
        this.scrollPane.setPreferredSize(new Dimension(d.width, table.getRowHeight() * table.getRowCount() + 10));
    }

}
