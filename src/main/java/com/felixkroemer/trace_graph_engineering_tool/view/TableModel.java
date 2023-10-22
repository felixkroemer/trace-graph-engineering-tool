package com.felixkroemer.trace_graph_engineering_tool.view;

import javax.swing.table.DefaultTableModel;

public class TableModel extends DefaultTableModel {
    public TableModel(int rows, int cols) {
        super(rows, cols);
    }

    public TableModel(int cols) {
        super(0, cols);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
}