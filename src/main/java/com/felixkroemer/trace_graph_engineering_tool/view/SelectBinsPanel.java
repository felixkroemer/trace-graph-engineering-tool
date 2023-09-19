package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.jdesktop.swingx.JXMultiThumbSlider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SelectBinsPanel extends JPanel {

    private JXMultiThumbSlider slider;

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
        param.getBins().forEach(b -> Double.valueOf(b));
        this.param = param;

        this.slider = new JXMultiThumbSlider<Integer>();
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
            this.param.setBins(this.bins);
        });

        this.cancelButton.addActionListener(e -> {
            ((Window) getRootPane().getParent()).dispose();
        });

        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void initSlider() {
        this.slider.setThumbRenderer(new TriangleThumbRenderer());
        this.slider.setMinimumValue(0);
        this.slider.setMaximumValue(1);
        this.slider.setTrackRenderer(new DiscreteTrackRenderer());
        this.add(slider, BorderLayout.CENTER);

        slider.getModel().addThumb(0.2f, 0.5f);
        slider.getModel().addThumb(0.5f, 0.7f);
        slider.getModel().addThumb(0.7f, 0.1f);
    }
}
