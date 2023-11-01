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
    private JSlider slider;

    public TracesPanel(CyServiceRegistrar registrar, TracesEdgeDisplayController controller) {
        this.registrar = registrar;
        this.controller = controller;
        this.slider = new JSlider();

        this.init();

        controller.addObserver(this);
    }

    private void updateSliderRange() {
        var displayRange = this.controller.getDisplayRange();
        this.slider.setMinimum(0);
        this.slider.setMaximum(controller.getTraces().size());
        this.slider.setValue(displayRange.getValue1());
    }

    private void init() {
        this.setLayout(new BorderLayout());

        this.updateSliderRange();
        this.slider.setMajorTickSpacing(10);
        this.slider.setMinorTickSpacing(1);
        this.slider.setSnapToTicks(true);
        this.slider.setPaintTrack(true);
        this.slider.setPaintTicks(true);
        this.slider.setPaintLabels(true);
        this.slider.setBorder(LookAndFeelUtil.createTitledBorder("Number of Traces"));

        this.slider.addChangeListener(e -> {
            var value = slider.getValue();
            controller.setDisplayRange(0, value);
        });

        var panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(slider, BorderLayout.CENTER);
        this.add(panel, BorderLayout.NORTH);
    }

    @Override
    public String getTitle() {
        return "Traces";
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "traces" -> {
                this.updateSliderRange();
            }
        }
    }
}
