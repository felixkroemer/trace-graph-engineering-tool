package com.felixkroemer.trace_graph_engineering_tool.view;


import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import com.felixkroemer.trace_graph_engineering_tool.tasks.ShowTraceTask;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collection;

public class NodeComparisonPanel extends TraceGraphPanel {

    private CyServiceRegistrar reg;

    private JPanel showTracePanel;
    private JButton showTraceButton;
    private JXTable comparisonTable;

    public NodeComparisonPanel(CyServiceRegistrar reg) {
        this.reg = reg;
        this.comparisonTable = new JXTable();
        this.showTracePanel = new JPanel();
        this.showTraceButton = new JButton("Show Trace");

        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());

        this.comparisonTable.getTableHeader().setUI(null);
        this.comparisonTable.setRowSelectionAllowed(false);
        this.comparisonTable.setColumnSelectionAllowed(false);
        this.comparisonTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.comparisonTable.setBackground(Color.MAGENTA);
        this.comparisonTable.setGridColor(Color.GRAY);
        this.comparisonTable.setTableHeader(null);
        this.comparisonTable.setBackground(UIManager.getColor("Panel.background"));
        this.comparisonTable.setHighlighters(new DifferentValueRowHighlighter());
        this.setBackground(UIManager.getColor("Panel.background"));

        this.add(new JScrollPane(comparisonTable), BorderLayout.CENTER);

        showTracePanel.add(this.showTraceButton);
        this.add(this.showTracePanel, BorderLayout.SOUTH);
    }

    public void setNodes(NetworkController controller, Collection<CyNode> nodes, CyNetwork network) {
        var pdm = controller.getPDM();
        var model = new TableModel(nodes.size() + 1);

        var header = new String[nodes.size() + 1];
        for (int i = 1; i <= nodes.size(); i++) {
            header[i] = "" + i;
        }
        model.addRow(header);

        var table = controller.getNetwork().getDefaultNodeTable();

        for (var param : pdm.getParameters()) {
            String[] row = new String[nodes.size() + 1];
            row[0] = param.getName();
            int i = 1;
            for (CyNode node : nodes) {
                row[i] = "" + table.getRow(node.getSUID()).get(param.getName(), Integer.class);
                i++;
            }
            model.addRow(row);
        }

        this.comparisonTable.setModel(model);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        TableModel tableModel = (TableModel) comparisonTable.getModel();

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            this.comparisonTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        this.comparisonTable.packAll();


        this.updateShowTraceButton(network);
    }

    private void updateShowTraceButton(CyNetwork network) {
        for (var listener : this.showTraceButton.getActionListeners()) {
            this.showTraceButton.removeActionListener(listener);
        }
        this.showTraceButton.addActionListener(e -> {
            var taskManager = reg.getService(TaskManager.class);
            taskManager.execute(new TaskIterator(new ShowTraceTask(reg, network)));
        });
    }

    @Override
    public String getTitle() {
        return "Node Comparison";
    }
}

class DifferentValueRowHighlighter extends AbstractHighlighter {

    @Override
    protected Component doHighlight(Component renderer, ComponentAdapter adapter) {

        if (adapter.row == 0) {
            return renderer;
        }
        renderer.setBackground(Color.GRAY);
        return renderer;
    }

    @Override
    public HighlightPredicate getHighlightPredicate() {
        return (renderer, adapter) -> {
            // Apply the highlighter to all rows
            for (int i = 2; i < adapter.getColumnCount(); i++) {
                if (!adapter.getValue(i).equals(adapter.getValue(i - 1))) {
                    return true;
                }
            }
            return false;
        };
    }
}
