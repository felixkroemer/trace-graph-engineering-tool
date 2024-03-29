package com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels;

import com.felixkroemer.trace_graph_engineering_tool.display_controller.DefaultEdgeDisplayController;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.javatuples.Pair;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class DefaultEdgeDisplayControllerPanel extends EdgeDisplayControllerPanel implements PropertyChangeListener {

    private DefaultEdgeDisplayController controller;
    private JSlider slider;

    public DefaultEdgeDisplayControllerPanel(DefaultEdgeDisplayController controller) {
        this.controller = controller;
        this.slider = new JSlider();

        this.init();

        this.controller.addObserver(this);
    }

    @Override
    public String getTitle() {
        return "Edge Display";
    }

    private void updateSliderRange() {
        var displayRange = this.controller.getDisplayRange();
        this.slider.setMinimum(0);
        this.slider.setMaximum(controller.getMaxTraversals());
        this.slider.setValue(displayRange.getValue0());
        this.slider.setLabelTable(this.slider.createStandardLabels(Math.max(controller.getMaxTraversals() / 10, 1)));
        this.slider.setMajorTickSpacing(controller.getMaxTraversals() / 10);
        this.slider.setMinorTickSpacing(controller.getMaxTraversals() / 40);
    }

    private void initNumberSlider() {
        this.updateSliderRange();
        this.slider.setPaintTrack(true);
        this.slider.setPaintTicks(true);
        this.slider.setPaintLabels(true);
        this.slider.setBorder(LookAndFeelUtil.createTitledBorder("Edge Traversals"));

        this.slider.addChangeListener(e -> {
            var traversals = slider.getValue();
            controller.setDisplayRange(traversals, controller.getMaxTraversals());
        });
    }

    private void init() {
        this.setLayout(new BorderLayout());

        this.initNumberSlider();

        var panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(slider, BorderLayout.CENTER);
        this.add(panel, BorderLayout.NORTH);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case DefaultEdgeDisplayController.MAX_TRAVERSALS -> this.slider.setMaximum((int) evt.getNewValue());
            case DefaultEdgeDisplayController.DISPLAY_RANGE -> {
                var range = (Pair<Integer, Integer>) evt.getNewValue();
                this.slider.setMinimum(range.getValue0());
                this.slider.setValue(range.getValue0());
            }
        }
    }

    @Override
    public EdgeDisplayControllerPanelLocation getDisplayLocation() {
        return EdgeDisplayControllerPanelLocation.NORTH;
    }
}
