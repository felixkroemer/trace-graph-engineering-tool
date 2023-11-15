package com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels;

import com.felixkroemer.trace_graph_engineering_tool.display_controller.TracesEdgeDisplayController;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TracesPanel extends TraceGraphPanel implements PropertyChangeListener {
    private CyServiceRegistrar registrar;
    private TracesEdgeDisplayController controller;
    private JSlider numberSlider;
    private JSlider lengthSlider;

    public TracesPanel(CyServiceRegistrar registrar, TracesEdgeDisplayController controller) {
        this.registrar = registrar;
        this.controller = controller;
        this.numberSlider = new JSlider();
        this.lengthSlider = new JSlider();

        this.init();

        controller.addObserver(this);
    }

    private void updateNumberSliderRange() {
        var displayRange = this.controller.getDisplayRange();
        this.numberSlider.setMinimum(0);
        this.numberSlider.setMaximum(controller.getTraces().size());
        this.numberSlider.setValue(displayRange.getValue1());
    }

    private void initNumberSlider() {
        this.updateNumberSliderRange();
        this.numberSlider.setMajorTickSpacing(10);
        this.numberSlider.setMinorTickSpacing(1);
        this.numberSlider.setSnapToTicks(true);
        this.numberSlider.setPaintTrack(true);
        this.numberSlider.setPaintTicks(true);
        this.numberSlider.setPaintLabels(true);
        this.numberSlider.setBorder(LookAndFeelUtil.createTitledBorder("Number of Traces"));

        this.numberSlider.addChangeListener(e -> {
            var number = numberSlider.getValue();
            controller.setDisplayRange(0, number);
        });
    }

    private void initLengthSlider() {
        this.lengthSlider.setMinimum(1);
        this.lengthSlider.setMaximum(10);
        this.lengthSlider.setValue(3);
        this.lengthSlider.setMajorTickSpacing(4);
        this.lengthSlider.setMinorTickSpacing(1);
        this.lengthSlider.setSnapToTicks(true);
        this.lengthSlider.setPaintTrack(true);
        this.lengthSlider.setPaintTicks(true);
        this.lengthSlider.setPaintLabels(true);
        this.lengthSlider.setBorder(LookAndFeelUtil.createTitledBorder("Trace Length"));

        this.lengthSlider.addChangeListener(e -> {
            var length = lengthSlider.getValue();
            controller.setLength(length);
        });
    }

    private void init() {
        this.setLayout(new BorderLayout());

        this.initNumberSlider();
        this.initLengthSlider();

        var panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(numberSlider, BorderLayout.CENTER);
        panel.add(lengthSlider, BorderLayout.SOUTH);
        this.add(panel, BorderLayout.NORTH);
    }

    @Override
    public String getTitle() {
        return "Traces";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case TracesEdgeDisplayController.TRACES -> {
                this.updateNumberSliderRange();
            }
        }
    }
}
