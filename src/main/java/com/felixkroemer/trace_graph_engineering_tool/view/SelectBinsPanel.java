package com.felixkroemer.trace_graph_engineering_tool.view;

import org.jdesktop.swingx.JXMultiThumbSlider;

import javax.swing.*;
import java.awt.*;

public class SelectBinsPanel extends JPanel {

    private JXMultiThumbSlider slider;

    public SelectBinsPanel() {
        setLayout(new BorderLayout());
        this.slider = new JXMultiThumbSlider<Integer>();
        this.slider.setMinimumSize(new Dimension(200, 200));
        init();
    }

    public void init() {
        TriangleThumbRenderer thumbRend = new TriangleThumbRenderer();
        slider.setThumbRenderer(thumbRend);
        DiscreteTrackRenderer trackRenderer = new DiscreteTrackRenderer();
        slider.setTrackRenderer(trackRenderer);
        slider.setBackground(Color.MAGENTA);
        this.add(slider, BorderLayout.CENTER);
        slider.getModel().addThumb(0.2f, 1);
        slider.getModel().addThumb(0.5f, 4);
        slider.getModel().addThumb(0.7f, 10);
        slider.getParent().repaint();
        slider.repaint();
        repaint();
    }
}
