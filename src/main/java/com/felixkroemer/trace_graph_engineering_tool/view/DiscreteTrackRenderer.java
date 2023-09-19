package com.felixkroemer.trace_graph_engineering_tool.view;

// based on org/cytoscape/view/vizmap/gui/internal/view/editor/mappingeditor/DiscreteTrackRenderer.java

import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.TrackRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;


public class DiscreteTrackRenderer extends JComponent implements TrackRenderer {

    private static final int ICON_SIZE = 32;
    private static final int THUMB_WIDTH = 12;

    static final BasicStroke STROKE1 = new BasicStroke(1.0f);
    static final Color BORDER_COLOR = Color.BLACK;
    static final Color LABEL_COLOR = Color.BLACK;
    static final Color BACKGROUND_COLOR = Color.WHITE;

    private float minValue;
    private float maxValue;

    private JXMultiThumbSlider<Integer> slider;

    public DiscreteTrackRenderer(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        paintComponent(g);
    }

    @Override
    protected void paintComponent(Graphics gfx) {
        // Turn AA on
        final Graphics2D g = (Graphics2D) gfx;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //private int smallIconSize = 20;
        int trackHeight = slider.getHeight() - 100;
        int arrowBarYPosition = trackHeight + 50;

        final int trackWidth = slider.getWidth() - THUMB_WIDTH;
        g.translate(THUMB_WIDTH / 2, 12);

        final List<Thumb<Integer>> stops = slider.getModel().getSortedThumbs();
        final int numPoints = stops.size();

        // set up the data for the gradient
        final float[] fractions = new float[numPoints];
        int i = 0;

        for (Thumb<Integer> thumb : stops) {
            fractions[i] = thumb.getPosition();
            i++;
        }

        // Draw arrow
        g.setStroke(STROKE1);
        g.setColor(BORDER_COLOR);
        g.drawLine(0, arrowBarYPosition, trackWidth, arrowBarYPosition);

        final Polygon arrow = new Polygon();
        arrow.addPoint(trackWidth, arrowBarYPosition);
        arrow.addPoint(trackWidth - 20, arrowBarYPosition - 8);
        arrow.addPoint(trackWidth - 20, arrowBarYPosition);
        g.fill(arrow);

        g.setColor(LABEL_COLOR);
        g.drawLine(0, arrowBarYPosition, 15, arrowBarYPosition - 30);
        g.drawLine(15, arrowBarYPosition - 30, 25, arrowBarYPosition - 30);

        //g.setFont(SMALL_FONT);
        g.drawString("Min=" + minValue, 28, arrowBarYPosition - 25);

        g.drawLine(trackWidth, arrowBarYPosition, trackWidth - 15, arrowBarYPosition + 30);
        g.drawLine(trackWidth - 15, arrowBarYPosition + 30, trackWidth - 25, arrowBarYPosition + 30);

        final String maxStr = "Max=" + maxValue;
        int strWidth = SwingUtilities.computeStringWidth(g.getFontMetrics(), maxStr);
        g.drawString(maxStr, trackWidth - strWidth - 26, arrowBarYPosition + 35);

        if (numPoints == 0) {
            g.setColor(BORDER_COLOR);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(0, 5, trackWidth, trackHeight);

            return;
        }

        g.setStroke(STROKE1);

        // Fill background
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 5, trackWidth, trackHeight);

        final Point2D p1 = new Point2D.Float(0, 5);
        final Point2D p2 = new Point2D.Float(0, 5);

        int iconLocX;
        int iconLocY;

        // Draw Icons
        for (i = 0; i < stops.size(); i++) {
            int newX = (int) (trackWidth * fractions[i]);

            p2.setLocation(newX, 5);
            g.setColor(LABEL_COLOR);
            g.setStroke(STROKE1);

            g.drawLine(newX, 5, newX, trackHeight + 4);

            g.setColor(LABEL_COLOR);
            //g.setFont(SMALL_FONT);

            final float valueRange = maxValue - minValue;
            final Float curPositionValue = ((Number) ((fractions[i] * valueRange) + minValue)).floatValue();
            final String valueString = String.format("%.5f", curPositionValue);

            int flipLimit = 90;
            int borderVal = trackWidth - newX;

            if (((i % 2) == 0) && (flipLimit < borderVal)) {
                g.drawLine(newX, arrowBarYPosition, newX + 20, arrowBarYPosition - 15);
                g.drawLine(newX + 20, arrowBarYPosition - 15, newX + 30, arrowBarYPosition - 15);
                g.setColor(LABEL_COLOR);
                g.drawString(valueString, newX + 33, arrowBarYPosition - 11);
            } else if (((i % 2) == 1) && (flipLimit < borderVal)) {
                g.drawLine(newX, arrowBarYPosition, newX + 20, arrowBarYPosition + 15);
                g.drawLine(newX + 20, arrowBarYPosition + 15, newX + 30, arrowBarYPosition + 15);
                g.setColor(LABEL_COLOR);
                g.drawString(valueString, newX + 33, arrowBarYPosition + 19);
            } else if (((i % 2) == 0) && (flipLimit >= borderVal)) {
                g.drawLine(newX, arrowBarYPosition, newX - 20, arrowBarYPosition - 15);
                g.drawLine(newX - 20, arrowBarYPosition - 15, newX - 30, arrowBarYPosition - 15);
                g.setColor(LABEL_COLOR);
                g.drawString(valueString, newX - 90, arrowBarYPosition - 11);
            } else {
                g.drawLine(newX, arrowBarYPosition, newX - 20, arrowBarYPosition + 15);
                g.drawLine(newX - 20, arrowBarYPosition + 15, newX - 30, arrowBarYPosition + 15);
                g.setColor(LABEL_COLOR);
                g.drawString(valueString, newX - 90, arrowBarYPosition + 19);
            }

            g.setColor(LABEL_COLOR);
            g.fillOval(newX - 3, arrowBarYPosition - 3, 6, 6);

            iconLocX = (int) (p2.getX() - ((p2.getX() - p1.getX()) / 2 + ICON_SIZE / 2));
            iconLocY = (int) (trackHeight / 2 - ICON_SIZE / 2 + p2.getY());

            p1.setLocation(p2);
        }

        // Draw last region (above region)
        p2.setLocation(trackWidth, 5);

        iconLocX = (int) (p2.getX() - ((p2.getX() - p1.getX()) / 2 + ICON_SIZE / 2));
        iconLocY = (int) (trackHeight / 2 - ICON_SIZE / 2 + p2.getY());


        g.setColor(BORDER_COLOR);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(0, 5, trackWidth, trackHeight);

        g.translate(-THUMB_WIDTH / 2, -12);
    }

    public JComponent getRendererComponent(JXMultiThumbSlider slider) {
        this.slider = slider;
        return this;
    }

}
