package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import com.felixkroemer.trace_graph_engineering_tool.controller.SelectBinsController;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyTable;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.Thumb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class SelectBinsPanel extends JPanel {

    public static final String TOGGLE_RESET = "Click to reset";
    public static final String TOGGLE_PLACE_OR_CANCEL = "Pick range or click to disable";
    public static final String TOGGLE_SELECT = "Select Highlight range";

    private final Logger logger;

    private SelectBinsController controller;
    private JXMultiThumbSlider<Void> slider;

    private JPanel buttonPanel;
    private JButton confirmButton;
    private JButton cancelButton;
    private List<Float> bins;
    private float minimum;
    private float maximum;
    private DiscreteTrackRenderer trackRenderer;

    private JButton toggleHighlightButton;
    private boolean selectHighlight;
    private int highlightFrom;
    private int highlightTo;

    public SelectBinsPanel(SelectBinsController controller) {
        this.logger = LoggerFactory.getLogger(CyUserLog.class);
        this.controller = controller;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 600));
        this.setBorder(new EmptyBorder(20, 20, 10, 20));
        this.confirmButton = new JButton("Confirm");
        this.cancelButton = new JButton("Cancel");
        this.toggleHighlightButton = new JButton();
        this.highlightFrom = -1;
        this.highlightTo = -1;
        this.buttonPanel = new JPanel();

        this.bins = new ArrayList<>(controller.getParameter().getBins().size());
        var parameter = controller.getParameter();
        this.minimum = controller.getSourceTable().getAllRows().stream().min(Comparator.comparingDouble(o -> {
            return o.get(parameter.getName(), Double.class);
        })).get().get(parameter.getName(), Double.class).floatValue();
        this.maximum = controller.getSourceTable().getAllRows().stream().max(Comparator.comparingDouble(o -> {
            return o.get(parameter.getName(), Double.class);
        })).get().get(parameter.getName(), Double.class).floatValue();

        this.slider = new JXMultiThumbSlider<>();
        initButtons(controller.getParameter());
        initSlider(controller.getParameter(), controller.getSourceTable());
    }

    public void initButtons(Parameter param) {
        BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS);
        this.buttonPanel.setLayout(layout);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(confirmButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(toggleHighlightButton);
        this.buttonPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

        this.confirmButton.addActionListener(e -> {
            ((Window) getRootPane().getParent()).dispose();
            List<Float> positions = slider.getModel().getSortedThumbs().stream().map(Thumb::getPosition).toList();
            List<Float> newBuckets =
                    positions.stream().map(p -> (p * (this.maximum - this.minimum)) + this.minimum).toList();
            if (!newBuckets.equals(this.bins)) {
                controller.setNewBins(newBuckets);
            }
            var highlightedBins = new HashSet<Integer>();
            if (this.highlightRangeIsSet()) {
                for (int i = this.highlightFrom; i <= this.highlightTo; i++) {
                    highlightedBins.add(i);
                }
            }
            controller.setNewHighlightedBins(highlightedBins);
        });

        this.cancelButton.addActionListener(e -> {
            ((Window) getRootPane().getParent()).dispose();
        });

        if (!controller.getHighlightedBins().isEmpty()) {
            this.selectHighlight = true;
            this.highlightFrom = Collections.min(controller.getHighlightedBins());
            this.highlightTo = Collections.max(controller.getHighlightedBins());
            toggleHighlightButton.setText(TOGGLE_RESET);
        } else {
            this.selectHighlight = false;
            this.highlightFrom = -1;
            this.highlightTo = -1;
            toggleHighlightButton.setText(TOGGLE_SELECT);
        }

        this.toggleHighlightButton.addActionListener(e -> {
            if (this.highlightRangeIsSet()) {
                this.trackRenderer.disableHighlight();
                this.slider.repaint();
                this.selectHighlight = false;
                this.toggleHighlightButton.setText(TOGGLE_SELECT);
                this.highlightFrom = -1;
                this.highlightTo = -1;
            } else {
                selectHighlight = !selectHighlight;
                if (selectHighlight) {
                    this.trackRenderer.disableHighlight();
                    this.toggleHighlightButton.setText(TOGGLE_RESET);
                } else {
                    this.toggleHighlightButton.setText(TOGGLE_SELECT);
                    this.highlightFrom = -1;
                    this.highlightTo = -1;
                }
            }
        });

        String key = "removeSelectedThumb";
        this.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), key);
        this.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                trackRenderer.disableHighlight();
                slider.getModel().removeThumb(slider.getSelectedIndex());
            }
        });

        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    private boolean highlightRangeIsSet() {
        return this.highlightFrom != -1 && this.highlightTo != -1;
    }

    public void initSlider(Parameter param, CyTable sourceTable) {
        this.slider.setThumbRenderer(new TriangleThumbRenderer());
        this.slider.setMinimumValue(0f);
        this.slider.setMaximumValue(1f);
        this.trackRenderer = new DiscreteTrackRenderer((float) this.minimum, (float) this.maximum, param.getName(),
                sourceTable, this.highlightFrom, this.highlightTo);
        this.slider.setTrackRenderer(trackRenderer);
        this.add(slider, BorderLayout.CENTER);

        this.slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    float position = (float) (e.getX() * 1.0 / slider.getWidth());
                    slider.getModel().addThumb((float) position, null);
                } else if (e.getClickCount() == 1) {
                    if (selectHighlight) {
                        // bin index
                        int index = getIndex(e);

                        if (highlightFrom == -1) {
                            highlightFrom = index;
                            trackRenderer.highlight(index - 1, index);
                        } else if (highlightTo == -1) {
                            highlightTo = index;
                            trackRenderer.highlight(highlightFrom - 1, index);
                            toggleHighlightButton.setText(TOGGLE_RESET);
                        }
                        slider.repaint();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
            }
        });

        for (double bin : param.getBins()) {
            this.slider.getModel().addThumb(((float) bin - this.minimum) / (this.maximum - this.minimum), null);
        }
        List<Float> positions = slider.getModel().getSortedThumbs().stream().map(Thumb::getPosition).toList();
        this.bins = positions.stream().map(p -> (p * (this.maximum - this.minimum)) + this.minimum).toList();
    }

    private int getIndex(MouseEvent e) {
        int index = -1;
        var thumbs = slider.getModel().getSortedThumbs();
        for (int i = 0; i < thumbs.size(); i++) {
            var x = (int) (slider.getWidth() * 1.0 * thumbs.get(i).getPosition());
            if (e.getX() < x) {
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
