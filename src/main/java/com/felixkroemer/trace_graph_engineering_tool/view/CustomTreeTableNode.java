package com.felixkroemer.trace_graph_engineering_tool.view;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

public class CustomTreeTableNode extends DefaultMutableTreeTableNode {
    private Object column1Value;
    private Object column2Value;

    public CustomTreeTableNode(Object column1Value, Object column2Value) {
        this.column1Value = column1Value;
        this.column2Value = column2Value;
    }

    public Object getColumn1Value() {
        return column1Value;
    }

    public void setColumn1Value(Object column1Value) {
        this.column1Value = column1Value;
    }

    public Object getColumn2Value() {
        return column2Value;
    }

    public void setColumn2Value(Object column2Value) {
        this.column2Value = column2Value;
    }

    // You may also need to implement methods related to the hierarchical structure

    // ...
}