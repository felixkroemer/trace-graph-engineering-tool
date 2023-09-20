package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.Thumb;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SelectBinsPanel extends JPanel {

    private JXMultiThumbSlider<Void> slider;

    private JPanel buttonPanel;
    private JButton confirmButton;
    private JButton cancelButton;

    private Parameter param;
    private List<Double> bins;

    public SelectBinsPanel(Parameter param) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(500, 300));
        this.setBorder(new EmptyBorder(20, 20, 10, 20));
        this.confirmButton = new JButton("Confirm");
        this.cancelButton = new JButton("Cancel");
        this.buttonPanel = new JPanel();

        this.bins = new ArrayList<Double>(param.getBins().size());
        param.getBins().forEach(b -> this.bins.add(Double.valueOf(b)));
        this.param = param;

        this.slider = new JXMultiThumbSlider<Void>();
        initButtons();
        initSlider();
    }

    public void initButtons() {
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
            List<Double> newBuckets =
                    positions.stream().map(p -> (p * (param.getMaximum() - param.getMinimum())) + param.getMinimum()).toList();
            this.param.setBins(newBuckets);
        });

        this.cancelButton.addActionListener(e -> {
            ((Window) getRootPane().getParent()).dispose();
        });

        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void initSlider() {
        this.slider.setThumbRenderer(new TriangleThumbRenderer());
        this.slider.setMinimumValue(0f);
        this.slider.setMaximumValue(1f);
        this.slider.setTrackRenderer(new DiscreteTrackRenderer((float) param.getMinimum(), (float) param.getMaximum()));
        this.add(slider, BorderLayout.CENTER);

        for (double bin : this.bins) {
            this.slider.getModel().addThumb((float) ((bin - param.getMinimum()) / (param.getMaximum() - param.getMinimum())), null);
        }
    }
}
