package com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

public class MultiObjectTreeTableNode extends DefaultMutableTreeTableNode {

    private Object[] objects;

    public MultiObjectTreeTableNode(Object... objects) {
        this.objects = objects;
    }

    @Override
    public Object getValueAt(int column) {
        if (column < this.objects.length) {
            return this.objects[column];
        } else {
            return "";
        }
    }
}