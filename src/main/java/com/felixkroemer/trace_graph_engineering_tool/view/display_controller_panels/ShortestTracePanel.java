package com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels;


import com.felixkroemer.trace_graph_engineering_tool.display_controller.ShortestTraceEdgeDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTable;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTableModel;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.MultiObjectTreeTableNode;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
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

    private CustomTreeTable traceInfoTable;
    private CustomTreeTable traceSequenceTable;
    private ShortestTraceEdgeDisplayController controller;

    public ShortestTracePanel(CyServiceRegistrar reg, ShortestTraceEdgeDisplayController controller,
                              TraceExtension trace) {
        this.reg = reg;
        this.controller = controller;
        this.traceInfoTable = new CustomTreeTable();
        this.traceSequenceTable = new CustomTreeTable();

        this.init();

        this.updatePanels(trace);
        controller.addObserver(this);
    }

    private void init() {
        setLayout(new BorderLayout());
        this.setBackground(UIManager.getColor("Panel.background"));

        this.traceInfoTable.setBorder(LookAndFeelUtil.createTitledBorder("Trace Info"));
        this.add(this.traceInfoTable, BorderLayout.NORTH);

        this.traceSequenceTable.setBorder(LookAndFeelUtil.createTitledBorder("Trace"));
        this.add(traceSequenceTable, BorderLayout.CENTER);

        this.traceSequenceTable.getWrappedTreeTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int row = traceSequenceTable.getWrappedTreeTable().rowAtPoint(e.getPoint());
                    TreePath path = traceSequenceTable.getWrappedTreeTable().getPathForRow(row);
                    if (path != null) {
                        var nodeNode = (MultiObjectTreeTableNode) path.getLastPathComponent();
                        var networkController = controller.getRenderingController().getTraceGraphController();
                        var node = (CyNode) nodeNode.getValueAt(0);
                        networkController.focusNode(node);
                    }
                }
            }
        });
    }

    private void updatePanels(TraceExtension trace) {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("Root");
        for (var x : trace.getIndices()) {
            root.add(new MultiObjectTreeTableNode(x.getValue0(), x.getValue1()));
        }
        this.traceSequenceTable.setModel(new CustomTreeTableModel(root, 2));

        root = new DefaultMutableTreeTableNode("Root");
        root.add(new MultiObjectTreeTableNode("Source table", trace.getSourceTable()));
        root.add(new MultiObjectTreeTableNode("Length", trace.getSequence().size()));
        root.add(new MultiObjectTreeTableNode("Unique nodes", trace.getUniqueSequence().size()));
        this.traceInfoTable.setModel(new CustomTreeTableModel(root, 2));
    }

    @Override
    public String getTitle() {
        return "Trace";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case ShortestTraceEdgeDisplayController.TRACE -> {
                this.updatePanels((TraceExtension) evt.getNewValue());
            }
        }
    }

    @Override
    public EdgeDisplayControllerPanelLocation getDisplayLocation() {
        return EdgeDisplayControllerPanelLocation.PANEL;
    }
}
