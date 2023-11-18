package com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels;


import com.felixkroemer.trace_graph_engineering_tool.display_controller.ShortestTraceEdgeDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTable;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ShortestTracePanel extends EdgeDisplayControllerPanel implements PropertyChangeListener {

    private CyServiceRegistrar reg;

    private CustomTreeTable traceTreeTable;
    private ShortestTraceEdgeDisplayController controller;

    private JPanel returnToPreviousModePanel;
    private JButton returnToPreviousModeButton;

    public ShortestTracePanel(CyServiceRegistrar reg, ShortestTraceEdgeDisplayController controller,
                              TraceExtension trace) {
        this.reg = reg;
        this.controller = controller;
        this.traceTreeTable = new CustomTreeTable();
        this.returnToPreviousModePanel = new JPanel();
        this.returnToPreviousModeButton = new JButton("Restore previous mode");
        //TODO: fix
        this.returnToPreviousModeButton.setEnabled(false);

        this.init();

        this.updateTracePanel(trace);
        controller.addObserver(this);
    }

    private void init() {
        setLayout(new BorderLayout());
        this.setBackground(UIManager.getColor("Panel.background"));

        this.add(traceTreeTable, BorderLayout.CENTER);

        traceTreeTable.getWrappedTreeTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int row = traceTreeTable.getWrappedTreeTable().rowAtPoint(e.getPoint());
                    TreePath path = traceTreeTable.getWrappedTreeTable().getPathForRow(row);
                    var nodeNode = (CyNodeTreeTableNode) path.getLastPathComponent();
                    var networkController = controller.getRenderingController().getTraceGraphController();
                    networkController.focusNode(nodeNode.getNode());
                }
            }
        });

        returnToPreviousModePanel.add(this.returnToPreviousModeButton);
        this.add(this.returnToPreviousModePanel, BorderLayout.SOUTH);
    }

    private void updateTracePanel(TraceExtension trace) {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("Root");
        CyNode prevNode = null;
        for (var node : trace.getSequence()) {
            if (prevNode == null || prevNode != node) {
                root.add(new CyNodeTreeTableNode(node));
            }
            prevNode = node;
        }
        this.traceTreeTable.setModel(new CyNodeTreeTableModel(root));
    }

    @Override
    public String getTitle() {
        return "Trace";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case ShortestTraceEdgeDisplayController.TRACE -> {
                this.updateTracePanel((TraceExtension) evt.getNewValue());
            }
        }
    }

    @Override
    public EdgeDisplayControllerPanelLocation getDisplayLocation() {
        return EdgeDisplayControllerPanelLocation.PANEL;
    }
}

class CyNodeTreeTableNode extends DefaultMutableTreeTableNode {
    private CyNode node;

    public CyNodeTreeTableNode(CyNode node) {
        this.node = node;
    }

    public CyNode getNode() {
        return this.node;
    }
}

class CyNodeTreeTableModel extends AbstractTreeTableModel {

    public CyNodeTreeTableModel(DefaultMutableTreeTableNode root) {
        super(root);
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int column) {
        return "";
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if (node instanceof CyNodeTreeTableNode treeTableNode) {
            return treeTableNode.getNode().getSUID();
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