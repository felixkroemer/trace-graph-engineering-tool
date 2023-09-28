package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.HighlightRange;
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
import java.util.ArrayList;
import java.util.List;

public class SelectBinsPanel extends JPanel {

    public static final String TOGGLE_RESET = "Click to reset";
    public static final String TOGGLE_PLACE_OR_CANCEL = "Pick range or click to disable";
    public static final String TOGGLE_SELECT = "Select Highlight range";

    private final Logger logger;

    private JXMultiThumbSlider<Void> slider;

    private JPanel buttonPanel;
    private JButton confirmButton;
    private JButton cancelButton;
    private List<Double> bins;
    private double minimum;
    private double maximum;
    private DiscreteTrackRenderer trackRenderer;
    private final CyTable sourceTable;

    private JButton toggleHighlightButton;
    private boolean selectHighlight;
    private int highlightFrom;
    private int highlightTo;

    public SelectBinsPanel(Parameter param, CyTable sourceTable) {
        this.logger = LoggerFactory.getLogger(CyUserLog.class);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 600));
        this.setBorder(new EmptyBorder(20, 20, 10, 20));
        this.confirmButton = new JButton("Confirm");
        this.cancelButton = new JButton("Cancel");
        this.toggleHighlightButton = new JButton();
        this.highlightFrom = -1;
        this.highlightTo = -1;
        this.buttonPanel = new JPanel();

        this.bins = new ArrayList<Double>(param.getBins().size());
        param.getBins().forEach(b -> this.bins.add(Double.valueOf(b)));
        this.minimum = param.getMinimum();
        this.maximum = param.getMaximum();
        this.sourceTable = sourceTable;

        this.slider = new JXMultiThumbSlider<>();
        initButtons(param);
        initSlider(param);
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
            List<Double> newBuckets =
                    positions.stream().map(p -> (p * (this.maximum - this.minimum)) + this.minimum).toList();
            if (!newBuckets.equals(this.bins)) {
                param.setBins(newBuckets);
            }
            if (this.highlightRangeIsSet()) {
                param.setHighlightRange(new HighlightRange(this.highlightFrom, this.highlightTo));
            } else {
                param.setHighlightRange(null);
            }
        });

        this.cancelButton.addActionListener(e -> {
            ((Window) getRootPane().getParent()).dispose();
        });

        if (param.getHighlightRange() != null) {
            this.selectHighlight = true;
            this.highlightFrom = param.getHighlightRange().getLowerBound();
            this.highlightTo = param.getHighlightRange().getUpperBound();
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

    public void initSlider(Parameter param) {
        this.slider.setThumbRenderer(new TriangleThumbRenderer());
        this.slider.setMinimumValue(0f);
        this.slider.setMaximumValue(1f);
        this.trackRenderer = new DiscreteTrackRenderer((float) this.minimum, (float) this.maximum, param.getName(),
                sourceTable, param.getHighlightRange());
        this.slider.setTrackRenderer(trackRenderer);
        this.add(slider, BorderLayout.CENTER);

        this.slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    double position = e.getX() * 1.0 / slider.getWidth();
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

        for (double bin : this.bins) {
            this.slider.getModel().addThumb((float) ((bin - this.minimum) / (this.maximum - this.minimum)), null);
        }
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
