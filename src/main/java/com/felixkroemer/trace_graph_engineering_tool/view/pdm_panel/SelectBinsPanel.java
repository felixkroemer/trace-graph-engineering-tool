package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import com.felixkroemer.trace_graph_engineering_tool.controller.SelectBinsController;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.model.CyTable;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.Thumb;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class SelectBinsPanel extends JPanel {

    private SelectBinsController controller;
    private JXMultiThumbSlider<Void> slider;
    private JPanel buttonPanel;
    private JButton confirmButton;
    private JButton cancelButton;
    private List<Float> bins;
    private List<Boolean> visibleBins;
    private float minimum;
    private float maximum;
    private JPopupMenu popupMenu;
    private JMenuItem toggleHiddenStateMenuItem;
    private int clickXPosition;

    public SelectBinsPanel(SelectBinsController controller) {
        this.controller = controller;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 600));
        this.setBorder(new EmptyBorder(20, 20, 10, 20));
        this.confirmButton = new JButton("Confirm");
        this.cancelButton = new JButton("Cancel");
        this.buttonPanel = new JPanel();

        this.popupMenu = new JPopupMenu();
        this.toggleHiddenStateMenuItem = new JMenuItem("Toggle Visible");
        popupMenu.add(this.toggleHiddenStateMenuItem);

        this.bins = new ArrayList<>(controller.getParameter().getBins().size());
        var parameter = controller.getParameter();
        this.minimum = controller.getSourceTables().stream().flatMap(t -> t.getAllRows().stream())
                                 .min(Comparator.comparingDouble(o -> o.get(parameter.getName(), Double.class))).get()
                                 .get(parameter.getName(), Double.class).floatValue();
        this.maximum = controller.getSourceTables().stream().flatMap(t -> t.getAllRows().stream())
                                 .max(Comparator.comparingDouble(o -> o.get(parameter.getName(), Double.class))).get()
                                 .get(parameter.getName(), Double.class).floatValue();
        this.visibleBins = new ArrayList<>();
        for (int i = 0; i < parameter.getBins().size() + 1; i++) {
            this.visibleBins.add(parameter.getVisibleBins().contains(i));
        }

        this.slider = new JXMultiThumbSlider<>();
        initButtons(controller.getParameter());
        initSlider(controller.getParameter(), controller.getSourceTables());
    }

    public void initButtons(Parameter param) {
        BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS);
        this.buttonPanel.setLayout(layout);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(confirmButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalGlue());

        this.buttonPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

        this.confirmButton.addActionListener(e -> {
            ((Window) getRootPane().getParent()).dispose();
            List<Float> positions = slider.getModel().getSortedThumbs().stream().map(Thumb::getPosition).toList();
            List<Float> newBuckets = positions.stream().map(p -> (p * (this.maximum - this.minimum)) + this.minimum)
                                              .toList();
            if (!newBuckets.equals(this.bins)) {
                controller.setNewBins(newBuckets);
            }

            Set<Integer> visible = new HashSet<>();
            for (int i = 0; i < this.visibleBins.size(); i++) {
                if (visibleBins.get(i)) {
                    visible.add(i);
                }
            }
            if (!visible.equals(param.getVisibleBins())) {
                this.controller.setVisibleBins(visible);
            }
        });

        this.cancelButton.addActionListener(e -> ((Window) getRootPane().getParent()).dispose());

        String key = "removeSelectedThumb";
        this.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), key);
        this.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                var index = slider.getSelectedIndex();
                var thumbs = slider.getModel().getSortedThumbs();
                int i = 0;
                for (var thumb : thumbs) {
                    if (thumb == slider.getModel().getThumbAt(index)) {
                        break;
                    }
                    i++;
                }
                slider.getModel().removeThumb(slider.getSelectedIndex());
                visibleBins.remove(i + 1);
            }
        });

        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void initSlider(Parameter param, Collection<CyTable> sourceTables) {
        this.slider.setThumbRenderer(new TriangleThumbRenderer());
        this.slider.setMinimumValue(0f);
        this.slider.setMaximumValue(1f);
        DiscreteTrackRenderer trackRenderer = new DiscreteTrackRenderer(this.minimum, this.maximum, param.getName(), sourceTables, this.visibleBins);
        this.slider.setTrackRenderer(trackRenderer);
        this.add(slider, BorderLayout.CENTER);

        this.slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = getIndex(e.getX());
                    visibleBins.add(index + 1, visibleBins.get(index));
                    float position = (float) (e.getX() * 1.0 / slider.getWidth());
                    slider.getModel().addThumb(position, null);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mousePressed(e);
                if (e.isPopupTrigger()) {
                    clickXPosition = e.getX();
                    popupMenu.show(slider, e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.isPopupTrigger()) {
                    clickXPosition = e.getX();
                    popupMenu.show(slider, e.getX(), e.getY());
                }
            }
        });

        this.toggleHiddenStateMenuItem.addActionListener(e -> {
            int index = getIndex(clickXPosition);
            if (visibleBins.get(index)) {
                visibleBins.set(index, false);
            } else {
                visibleBins.set(index, true);
            }
            slider.repaint();
        });

        for (double bin : param.getBins()) {
            this.slider.getModel().addThumb(((float) bin - this.minimum) / (this.maximum - this.minimum), null);
        }
        List<Float> positions = slider.getModel().getSortedThumbs().stream().map(Thumb::getPosition).toList();
        this.bins = positions.stream().map(p -> (p * (this.maximum - this.minimum)) + this.minimum).toList();
    }

    private int getIndex(int clickX) {
        int index = -1;
        var thumbs = slider.getModel().getSortedThumbs();
        for (int i = 0; i < thumbs.size(); i++) {
            var x = (int) (slider.getWidth() * 1.0 * thumbs.get(i).getPosition());
            if (clickX < x) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            index = thumbs.size();
        }
        return index;
    }
}
