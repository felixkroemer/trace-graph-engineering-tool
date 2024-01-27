package com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels;

import com.felixkroemer.trace_graph_engineering_tool.display_controller.TracesEdgeDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.model.SubTraceExtension;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTable;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.CustomTreeTableModel;
import com.felixkroemer.trace_graph_engineering_tool.view.custom_tree_table.MultiObjectTreeTableNode;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TracesPanel extends EdgeDisplayControllerPanel implements PropertyChangeListener {

    private final ScheduledExecutorService scheduler;
    private TracesEdgeDisplayController controller;
    private JSlider numberSlider;
    private JSlider lengthSlider;
    private ScheduledFuture<?> future;
    private CustomTreeTable tracesInfoTable;
    private boolean loading;

    public TracesPanel(TracesEdgeDisplayController controller) {
        this.controller = controller;
        this.numberSlider = new JSlider();
        this.lengthSlider = new JSlider();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.tracesInfoTable = new CustomTreeTable();
        this.loading = false;

        this.init();

        controller.addObserver(this);
    }

    private void updateNumberSlider(List<SubTraceExtension> traces) {
        if (traces != null) {
            this.numberSlider.setEnabled(true);
            var displayRange = this.controller.getDisplayRange();
            this.numberSlider.setMinimum(0);
            this.numberSlider.setMaximum(traces.size());
            this.numberSlider.setValue(displayRange.getValue1());
            this.numberSlider.setLabelTable(this.numberSlider.createStandardLabels(Math.max(traces.size() / 10, 1)));
            this.numberSlider.setMajorTickSpacing(traces.size() / 10);
            this.numberSlider.setMinorTickSpacing(traces.size() / 40);
        } else {
            this.numberSlider.setEnabled(false);
        }
    }

    private void initNumberSlider(List<SubTraceExtension> traces) {
        this.updateNumberSlider(traces);
        this.numberSlider.setPaintTrack(true);
        this.numberSlider.setPaintTicks(true);
        this.numberSlider.setPaintLabels(true);
        this.numberSlider.setBorder(LookAndFeelUtil.createTitledBorder("Number of Traces"));

        this.numberSlider.addChangeListener(e -> {
            if (!loading) {
                var number = numberSlider.getValue();
                schedule(() -> controller.setDisplayRange(0, number));
            }
        });
    }

    private void schedule(Runnable runnable) {
        if (this.future != null) {
            this.future.cancel(false);
        }
        this.future = this.scheduler.schedule(runnable, 200, TimeUnit.MILLISECONDS);
    }

    private void updateLengthSlider(List<SubTraceExtension> traces) {
        if (traces != null) {
            this.lengthSlider.setEnabled(true);
            this.lengthSlider.setValue(controller.getLength());
        } else {
            this.lengthSlider.setEnabled(false);
        }
    }

    private void initLengthSlider(List<SubTraceExtension> traces) {
        this.updateLengthSlider(traces);
        this.lengthSlider.setMinimum(1);
        this.lengthSlider.setMaximum(10);
        this.lengthSlider.setMajorTickSpacing(4);
        this.lengthSlider.setMinorTickSpacing(1);
        this.lengthSlider.setSnapToTicks(true);
        this.lengthSlider.setPaintTrack(true);
        this.lengthSlider.setPaintTicks(true);
        this.lengthSlider.setPaintLabels(true);
        this.lengthSlider.setBorder(LookAndFeelUtil.createTitledBorder("Trace Length"));

        this.lengthSlider.addChangeListener(e -> {
            if (!loading) {
                var length = lengthSlider.getValue();
                schedule(() -> controller.setLength(length));
            }
        });
    }

    public void init() {
        this.setLayout(new BorderLayout());

        var traces = controller.getTraces();

        this.initNumberSlider(traces);
        this.initLengthSlider(traces);
        this.initInfoTable(traces);

        this.add(tracesInfoTable, BorderLayout.CENTER);
        var panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(numberSlider, BorderLayout.CENTER);
        panel.add(lengthSlider, BorderLayout.SOUTH);
        this.add(panel, BorderLayout.NORTH);

        this.tracesInfoTable.getWrappedTreeTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int row = tracesInfoTable.getWrappedTreeTable().rowAtPoint(e.getPoint());
                    TreePath path = tracesInfoTable.getWrappedTreeTable().getPathForRow(row);
                    if (path != null) {
                        var tableNode = (MultiObjectTreeTableNode) path.getLastPathComponent();
                        var value = tableNode.getValueAt(0);
                        var networkController = controller.getRenderingController().getTraceGraphController();
                        if (value instanceof CyNode node) {
                            networkController.focusNode(node, false);
                        } else if (value instanceof SubTraceExtension trace) {
                            controller.highlightTrace(trace);
                        }
                    }
                }
            }
        });
    }

    private void initInfoTable(List<SubTraceExtension> traces) {
        this.tracesInfoTable.setBorder(LookAndFeelUtil.createTitledBorder("Traces"));
        this.updateInfoTable(traces);
    }

    private void updateInfoTable(List<SubTraceExtension> traces) {
        if (traces != null) {
            Map<CyTable, MultiObjectTreeTableNode> nodeMap = new HashMap<>();
            DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("Root");
            for (var trace : traces) {
                MultiObjectTreeTableNode tableNode = nodeMap.get(trace.getTable());
                if (tableNode == null) {
                    tableNode = new MultiObjectTreeTableNode(trace.getTable());
                    nodeMap.put(trace.getTable(), tableNode);
                    root.add(tableNode);
                }
                var traceNode = new MultiObjectTreeTableNode(trace);
                tableNode.add(traceNode);
                for (var element : trace.getIndices()) {
                    traceNode.add(new MultiObjectTreeTableNode(element.getValue0(), element.getValue1()));
                }
                tableNode.add(traceNode);
            }
            this.tracesInfoTable.setModel(new CustomTreeTableModel(root, 2));
        } else {
            this.tracesInfoTable.setModel(new DefaultTreeTableModel());
        }
    }

    @Override
    public String getTitle() {
        return "Traces";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(TracesEdgeDisplayController.TRACES)) {
            this.loading = true;
            var traces = (List<SubTraceExtension>) evt.getNewValue();
            this.updateNumberSlider(traces);
            this.updateLengthSlider(traces);
            this.updateInfoTable(traces);
            this.loading = false;
        }
    }

    @Override
    public EdgeDisplayControllerPanelLocation getDisplayLocation() {
        return EdgeDisplayControllerPanelLocation.PANEL;
    }
}
