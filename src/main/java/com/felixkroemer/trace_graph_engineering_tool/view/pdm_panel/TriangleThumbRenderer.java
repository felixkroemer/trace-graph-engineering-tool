package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

// based on org/cytoscape/view/vizmap/gui/internal/view/editor/mappingeditor/TriangleThumbRenderer.java

import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.ThumbRenderer;

import javax.swing.*;
import java.awt.*;

public final class TriangleThumbRenderer extends JComponent implements ThumbRenderer {

    private static final int STROKE_WIDTH = 1;
    private final Color FOCUS_COLOR = Color.GRAY;
    private final Color SELECTION_COLOR = Color.BLACK;
    private final Color DEFAULT_COLOR = Color.BLACK;
    // Keep the last selected thumb.
    private boolean selected;
    private int selectedIndex;
    private int currentIndex;

    public TriangleThumbRenderer() {
        Color BACKGROUND_COLOR = Color.yellow;
        setBackground(BACKGROUND_COLOR);
    }

    @Override
    protected void paintComponent(Graphics g) {
        var color = g.getColor();

        var g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (selected || selectedIndex == currentIndex) g2d.setColor(SELECTION_COLOR);
        else g2d.setColor(DEFAULT_COLOR);

        // Outer triangle (border)
        var p1 = new Polygon();
        p1.addPoint(0, 0);
        p1.addPoint(w, 0);
        p1.addPoint(w / 2, h);
        g2d.fillPolygon(p1);

        // Inner triangle (fill color)
        var p2 = new Polygon();
        p2.addPoint(2 * STROKE_WIDTH, STROKE_WIDTH);
        p2.addPoint(w - 2 * STROKE_WIDTH, STROKE_WIDTH);
        p2.addPoint(w / 2, h - 2 * STROKE_WIDTH);
        g2d.setColor(color);
        g2d.fillPolygon(p2);

        g2d.dispose();
    }

    @Override
    public JComponent getThumbRendererComponent(JXMultiThumbSlider slider, int index, boolean selected) {
        // Update state
        this.selected = selected;
        this.selectedIndex = slider.getSelectedIndex();
        this.currentIndex = index;

        if (selected || selectedIndex == currentIndex) setForeground(FOCUS_COLOR);
        else setForeground(DEFAULT_COLOR);

        return this;
    }
}
