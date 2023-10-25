package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTable;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class NodeInfoPanel extends TraceGraphPanel {

    private CyServiceRegistrar reg;
    private CyNode node;

    private CustomTreeTable infoTreeTable;

    private JPanel nodeInfoPanel;
    private JTable nodeInfoTable;
    private DefaultTableModel nodeInfoTableModel;


    public NodeInfoPanel(CyServiceRegistrar reg) {
        this.reg = reg;
        this.node = null;

        this.nodeInfoPanel = new JPanel();
        this.nodeInfoTableModel = new TableModel(0, 2);
        this.nodeInfoTable = new JTable(this.nodeInfoTableModel);

        this.infoTreeTable = new CustomTreeTable();

        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());

        this.nodeInfoTable.getTableHeader().setUI(null);
        this.nodeInfoTable.setRowSelectionAllowed(false);
        this.nodeInfoTable.setColumnSelectionAllowed(false);
        this.nodeInfoTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.nodeInfoTable.setBackground(UIManager.getColor("Panel.background"));
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        this.nodeInfoTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        this.nodeInfoPanel.setBorder(LookAndFeelUtil.createTitledBorder("Node Information"));
        this.nodeInfoPanel.setLayout(new BorderLayout());
        this.nodeInfoPanel.add(nodeInfoTable, BorderLayout.CENTER);

        this.infoTreeTable.setBorder(LookAndFeelUtil.createTitledBorder("Source Rows"));

        this.add(nodeInfoPanel, BorderLayout.NORTH);
        this.add(infoTreeTable, BorderLayout.CENTER);
    }

    public void updateInfoPanel(NetworkController controller, CyNode node) {
        this.nodeInfoTableModel.setRowCount(0);
        this.nodeInfoTableModel.addRow(new String[]{"SUID", node.getSUID().toString()});
        var edges = controller.getNetwork().getAdjacentEdgeList(node, CyEdge.Type.DIRECTED);
        var incoming = edges.stream().filter(edge -> edge.getSource() == node).count();
        this.nodeInfoTableModel.addRow(new String[]{"Incoming Edges", "" + incoming});
        this.nodeInfoTableModel.addRow(new String[]{"Outgoing Edges", "" + (edges.size() - incoming)});

        var info = controller.getNodeInfo(node);
        for (var entry : info.entrySet()) {
            this.nodeInfoTableModel.addRow(new String[]{entry.getKey(), entry.getValue()});
        }
    }

    public void updateSourceRowPanel(NetworkController controller, CyNode node) {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("Root");
        var model = controller.createSourceRowTableModel(node, root);
        this.infoTreeTable.setModel(model);
    }

    public void setNode(NetworkController controller, CyNode node) {
        this.updateInfoPanel(controller, node);
        this.updateSourceRowPanel(controller, node);
    }

    @Override
    public String getTitle() {
        return "Node Info";
    }
}