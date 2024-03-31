package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SplitTraceGraphPanel extends JPanel {

    private DefaultListModel<CyTable> leftListModel;
    private DefaultListModel<CyTable> rightListModel;
    private JList<CyTable> leftList;
    private JList<CyTable> rightList;
    private JButton leftToRightButton;
    private JButton rightToLeftButton;
    private JButton confirmButton;
    private JButton cancelButton;

    public SplitTraceGraphPanel(TraceGraphController controller, CyServiceRegistrar registrar) {

        this.leftListModel = new DefaultListModel<>();
        this.rightListModel = new DefaultListModel<>();

        this.leftList = new JList<>(leftListModel);
        this.rightList = new JList<>(rightListModel);

        this.leftToRightButton = new JButton(">");
        this.rightToLeftButton = new JButton("<");

        init();

        leftToRightButton.addActionListener(e -> {
            CyTable selectedValue = leftList.getSelectedValue();
            if (selectedValue != null) {
                leftListModel.removeElement(selectedValue);
                rightListModel.addElement(selectedValue);
            }
        });

        rightToLeftButton.addActionListener(e -> {
            CyTable selectedValue = rightList.getSelectedValue();
            if (selectedValue != null) {
                rightListModel.removeElement(selectedValue);
                leftListModel.addElement(selectedValue);
            }
        });

        this.confirmButton.addActionListener(e -> {
            if (rightListModel.isEmpty() || leftListModel.isEmpty()) {
                new Thread(() -> {
                    JOptionPane.showMessageDialog(null, "Keep and Extract set may not be empty", null, JOptionPane.ERROR_MESSAGE);
                }).start();
            } else {
                List<CyTable> toRemove = new ArrayList<>(rightListModel.size());
                for (int i = 0; i < rightListModel.size(); i++) {
                    toRemove.add(rightListModel.get(i));
                }
                var taskManager = registrar.getService(TaskManager.class);
                taskManager.execute(new TaskIterator(new AbstractTask() {
                    @Override
                    public void run(TaskMonitor taskMonitor) {
                        var newController = controller.splitTraceGraph(toRemove);
                        var manager = registrar.getService(TraceGraphManager.class);
                        manager.registerTraceGraph(controller.getPDM(), newController);
                    }
                }));
            }
            ((Window) getRootPane().getParent()).dispose();
        });

        this.cancelButton.addActionListener(e -> ((Window) getRootPane().getParent()).dispose());

        var traces = controller.getTraceGraph().getTraces();
        for (CyTable trace : traces) {
            this.leftListModel.addElement(trace);
        }
    }

    private void init() {
        setLayout(new BorderLayout());

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
        this.add(innerPanel, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Keep"));
        leftList.setPreferredSize(new Dimension(150, getHeight()));
        JScrollPane leftScrollPane = new JScrollPane(leftList);
        leftScrollPane.setPreferredSize(new Dimension(200, getHeight()));
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Extract"));
        rightList.setPreferredSize(new Dimension(150, getHeight()));
        JScrollPane rightScrollPane = new JScrollPane(rightList);
        rightScrollPane.setPreferredSize(new Dimension(200, getHeight()));
        rightPanel.add(rightScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setPreferredSize(new Dimension(80, getHeight()));

        this.leftToRightButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.leftToRightButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        buttonPanel.add(this.leftToRightButton);
        buttonPanel.add(Box.createVerticalStrut(10));

        this.rightToLeftButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.rightToLeftButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        buttonPanel.add(rightToLeftButton);

        innerPanel.add(leftPanel);
        innerPanel.add(buttonPanel);
        innerPanel.add(rightPanel);

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        this.confirmButton = new JButton("Confirm");
        this.cancelButton = new JButton("Cancel");

        bottomButtonPanel.add(confirmButton);
        bottomButtonPanel.add(cancelButton);

        add(bottomButtonPanel, BorderLayout.SOUTH);
    }
}