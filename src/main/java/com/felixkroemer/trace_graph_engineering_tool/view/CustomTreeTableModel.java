package com.felixkroemer.trace_graph_engineering_tool.view;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

public class CustomTreeTableModel extends AbstractTreeTableModel {

    public CustomTreeTableModel(DefaultMutableTreeTableNode root) {
        super(root);
    }

    @Override
    public int getColumnCount() {
        return 2; // Two columns
    }

    @Override
    public String getColumnName(int column) {
        return "";
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if (node instanceof CustomTreeTableNode treeTableNode) {
            if (column == 0) {
                return treeTableNode.getColumn1Value();
            } else if (column == 1) {
                return treeTableNode.getColumn2Value();
            }
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