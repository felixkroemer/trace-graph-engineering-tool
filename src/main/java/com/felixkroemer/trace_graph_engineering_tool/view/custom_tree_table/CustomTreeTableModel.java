package com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

public class CustomTreeTableModel extends AbstractTreeTableModel {
    private int columns;

    public CustomTreeTableModel(DefaultMutableTreeTableNode root, int columns) {
        super(root);
        this.columns = columns;
    }

    @Override
    public int getColumnCount() {
        return this.columns;
    }

    @Override
    public String getColumnName(int column) {
        return "";
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if (node instanceof DefaultMutableTreeTableNode treeTableNode) {
            return treeTableNode.getValueAt(column);
        }
        return null;
    }

    @Override
    public Object getChild(Object parent, int index) {
        var node = (DefaultMutableTreeTableNode) parent;
        return node.getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
        var node = (DefaultMutableTreeTableNode) parent;
        return node.getChildCount();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        var node = (DefaultMutableTreeTableNode) parent;
        var childNode = (DefaultMutableTreeTableNode) parent;
        return node.getIndex(childNode);
    }

}