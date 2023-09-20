package com.felixkroemer.trace_graph_engineering_tool.view;

// based on org/cytoscape/view/vizmap/gui/internal/view/editor/mappingeditor/DiscreteTrackRenderer.java

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.model.CyTable;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.TrackRenderer;

import javax.swing.*;
import java.awt.*;
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
    private CyTable sourceTable;
    private Parameter param;
    private int[] initialDistribution;

    private JXMultiThumbSlider<Void> slider;

    public DiscreteTrackRenderer(float minValue, float maxValue, CyTable sourceTable, Parameter param) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.sourceTable = sourceTable;
        this.param = param;
        this.initialDistribution = getDistribution(param.getBins());
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        paintComponent(g);
    }

    private int[] getDistribution(List<Double> bins) {
        int[] dist = new int[bins.size() + 1];
        this.sourceTable.getAllRows().forEach(row -> {
            double value = row.get(param.getName(), Double.class);
            int i = 0;
            for (double d : bins) {
                if (value <= d) {
                    dist[i]++;
                    break;
                }
                i++;
            }
            if (value > bins.get(bins.size() - 1)) {
                dist[bins.size()]++;
            }
        });
        return dist;
    }

    @Override
    protected void paintComponent(Graphics gfx) {
        // Turn AA on
        final Graphics2D g = (Graphics2D) gfx;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int trackHeight = slider.getHeight() - 100;
        int arrowBarYPosition = trackHeight + 50;

        final int trackWidth = slider.getWidth() - THUMB_WIDTH;
        g.translate(THUMB_WIDTH / 2, 12);

        final List<Thumb<Void>> stops = slider.getModel().getSortedThumbs();

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

        g.drawString("Min=" + minValue, 28, arrowBarYPosition - 25);

        g.drawLine(trackWidth, arrowBarYPosition, trackWidth - 15, arrowBarYPosition + 30);
        g.drawLine(trackWidth - 15, arrowBarYPosition + 30, trackWidth - 25, arrowBarYPosition + 30);

        final String maxStr = "Max=" + maxValue;
        int strWidth = SwingUtilities.computeStringWidth(g.getFontMetrics(), maxStr);
        g.drawString(maxStr, trackWidth - strWidth - 26, arrowBarYPosition + 35);

        if (stops.isEmpty()) {
            g.setColor(BORDER_COLOR);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(0, 5, trackWidth, trackHeight);

            return;
        }

        g.setStroke(STROKE1);

        // Fill background
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 5, trackWidth, trackHeight);

        g.setColor(LABEL_COLOR);
        g.setStroke(STROKE1);

        var positions = slider.getModel().getSortedThumbs().stream().map(Thumb::getPosition).toList();
        var newDist =
                getDistribution(positions.stream().map(f -> f * (param.getMaximum() - param.getMinimum()) + param.getMinimum()).toList());

        String frequency = "" + newDist[0];
        var width = g.getFontMetrics().stringWidth(frequency);
        float pos = (trackWidth * stops.get(0).getPosition()) / 2 - width / 2;
        if (width < (int) (trackWidth * stops.get(0).getPosition())) {
            g.drawString(frequency, pos, (float) trackHeight / 2);
        }
/*        int diff = newDist[0] - initialDistribution[0];
        g.setColor(diff > 0 ? Color.GREEN : Color.RED);
        g.drawString("" + diff, pos, (float) trackHeight / 2 + 20);*/

        // Draw Icons
        for (int i = 0; i < stops.size(); i++) {
            int x = (int) (trackWidth * stops.get(i).getPosition());
            int nextX = x;
            if (i < stops.size() - 1) {
                nextX = (int) (trackWidth * stops.get(i + 1).getPosition());
            } else {
                nextX = trackWidth;
            }

            g.drawLine(x, 5, x, trackHeight + 4);

            frequency = "" + newDist[i + 1];
            width = g.getFontMetrics().stringWidth(frequency);
            pos = x + (nextX - x) / 2 - width / 2;
            if (width < nextX - x) {
                g.drawString(frequency, pos, trackHeight / 2);
            }
/*            diff = newDist[i + 1] - initialDistribution[i + 1];
            g.setColor(diff > 0 ? Color.GREEN : Color.red);
            g.drawString("" + diff, pos, (float) trackHeight / 2 + 20);
            g.setColor(LABEL_COLOR);*/

            final float valueRange = maxValue - minValue;
            final Float curPositionValue =
                    ((Number) ((stops.get(i).getPosition() * valueRange) + minValue)).floatValue();
            final String valueString = String.format("%.5f", curPositionValue);

            int flipLimit = 90;
            int borderVal = trackWidth - x;

            if (((i % 2) == 0) && (flipLimit < borderVal)) {
                g.drawLine(x, arrowBarYPosition, x + 20, arrowBarYPosition - 15);
                g.drawLine(x + 20, arrowBarYPosition - 15, x + 30, arrowBarYPosition - 15);
                g.setColor(LABEL_COLOR);
                g.drawString(valueString, x + 33, arrowBarYPosition - 11);
            } else if (((i % 2) == 1) && (flipLimit < borderVal)) {
                g.drawLine(x, arrowBarYPosition, x + 20, arrowBarYPosition + 15);
                g.drawLine(x + 20, arrowBarYPosition + 15, x + 30, arrowBarYPosition + 15);
                g.setColor(LABEL_COLOR);
                g.drawString(valueString, x + 33, arrowBarYPosition + 19);
            } else if (((i % 2) == 0) && (flipLimit >= borderVal)) {
                g.drawLine(x, arrowBarYPosition, x - 20, arrowBarYPosition - 15);
                g.drawLine(x - 20, arrowBarYPosition - 15, x - 30, arrowBarYPosition - 15);
                g.setColor(LABEL_COLOR);
                g.drawString(valueString, x - 90, arrowBarYPosition - 11);
            } else {
                g.drawLine(x, arrowBarYPosition, x - 20, arrowBarYPosition + 15);
                g.drawLine(x - 20, arrowBarYPosition + 15, x - 30, arrowBarYPosition + 15);
                g.setColor(LABEL_COLOR);
                g.drawString(valueString, x - 90, arrowBarYPosition + 19);
            }

            g.setColor(LABEL_COLOR);
            g.fillOval(x - 3, arrowBarYPosition - 3, 6, 6);
        }

        g.setColor(BORDER_COLOR);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(0, 5, trackWidth, trackHeight);

        g.translate(-THUMB_WIDTH / 2, -12);
    }

    @Override
    public JComponent getRendererComponent(JXMultiThumbSlider slider) {
        this.slider = slider;
        return this;
    }

}
