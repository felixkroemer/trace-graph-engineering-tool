package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import java.awt.*;
import java.awt.image.BufferedImage;

// based on https://github.com/matthewbeckler/HeatMap
// and adjusted to work for one-dimensional arrays + smoothing using gaussian filter

/**
 * <p><strong>Title:</strong> HeatMap</p>
 *
 * <p>Description: HeatMap is a JPanel that displays a 2-dimensional array of
 * data using a selected color gradient scheme.</p>
 * <p>For specifying data, the first index into the double[][] array is the x-
 * coordinate, and the second index is the y-coordinate. In the constructor and
 * updateData method, the 'useGraphicsYAxis' parameter is used to control
 * whether the row y=0 is displayed at the top or bottom. Since the usual
 * graphics coordinate system has y=0 at the top, setting this parameter to
 * true will draw the y=0 row at the top, and setting the parameter to false
 * will draw the y=0 row at the bottom, like in a regular, mathematical
 * coordinate system. This parameter was added as a solution to the problem of
 * "Which coordinate system should we use? Graphics, or mathematical?", and
 * allows the user to choose either coordinate system. Because the HeatMap will
 * be plotting the data in a graphical manner, using the Java Swing framework
 * that uses the standard computer graphics coordinate system, the user's data
 * is stored internally with the y=0 row at the top.</p>
 * <p>There are a number of defined gradient types (look at the static fields),
 * but you can create any gradient you like by using either of the following
 * functions in the Gradient class:
 * <ul>
 *   <li>public static Color[] createMultiGradient(Color[] colors, int numSteps)</li>
 *   <li>public static Color[] createGradient(Color one, Color two, int numSteps)</li>
 * </ul>
 * You can then assign an arbitrary Color[] object to the HeatMap as follows:
 * <pre>myHeatMap.updateGradient(Gradient.createMultiGradient(new Color[] {Color.red, Color.white, Color.blue}, 256));</pre>
 * </p>
 *
 * <p>By default, the graph title, axis titles, and axis tick marks are not
 * displayed. Be sure to set the appropriate title before enabling them.</p>
 * <p>
 * <hr />
 * <p><strong>Copyright:</strong> Copyright (c) 2007, 2008</p>
 *
 * <p>HeatMap is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.</p>
 *
 * <p>HeatMap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.</p>
 *
 * <p>You should have received a copy of the GNU General Public License
 * along with HeatMap; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA</p>
 *
 * @author Matthew Beckler (matthew@mbeckler.org)
 * @author Josh Hayes-Sheen (grey@grevian.org), Converted to use BufferedImage.
 * @author J. Keller (jpaulkeller@gmail.com), Added transparency (alpha) support, data ordering bug fix.
 * @version 1.6
 */
public class HeatMap {

    private final double[] data;
    private int[] dataColorIndices;
    private Color[] colors;

    /**
     * @param data   The data to display, must be a complete array (non-ragged)
     * @param colors A variable of the type Color[]. See also {@link #createMultiGradient} and
     *               {@link #createGradient}.
     */
    public HeatMap(double[] data, Color[] colors) {
        this.data = smoothArray(data, 6.0);

        updateGradient(colors);
        updateDataColors();
    }

    public static Color[] createGradient(final Color one, final Color two, final int numSteps) {
        int r1 = one.getRed();
        int g1 = one.getGreen();
        int b1 = one.getBlue();
        int a1 = one.getAlpha();

        int r2 = two.getRed();
        int g2 = two.getGreen();
        int b2 = two.getBlue();
        int a2 = two.getAlpha();

        int newR;
        int newG;
        int newB;
        int newA;

        Color[] gradient = new Color[numSteps];
        double iNorm;
        for (int i = 0; i < numSteps; i++) {
            iNorm = i / (double) numSteps; //a normalized [0:1] variable
            newR = (int) (r1 + iNorm * (r2 - r1));
            newG = (int) (g1 + iNorm * (g2 - g1));
            newB = (int) (b1 + iNorm * (b2 - b1));
            newA = (int) (a1 + iNorm * (a2 - a1));
            gradient[i] = new Color(newR, newG, newB, newA);
        }

        return gradient;
    }

    /**
     * Creates an array of Color objects for use as a gradient, using an array of Color objects. It uses a linear
     * interpolation between each pair of points. The parameter numSteps defines the total number of colors in the
     * returned array, not the number of colors per segment.
     *
     * @param colors   An array of Color objects used for the gradient. The Color at index 0 will be the lowest color.
     * @param numSteps The number of steps in the gradient. 250 is a good number.
     */
    public static Color[] createMultiGradient(Color[] colors, int numSteps) {
        //we assume a linear gradient, with equal spacing between colors
        //The final gradient will be made up of n 'sections', where n = colors.length - 1
        int numSections = colors.length - 1;
        int gradientIndex = 0; //points to the next open spot in the final gradient
        Color[] gradient = new Color[numSteps];

        if (numSections <= 0) {
            throw new IllegalArgumentException("You must pass in at least 2 colors in the array!");
        }

        for (int section = 0; section < numSections; section++) {
            //we divide the gradient into (n - 1) sections, and do a regular gradient for each
            for (Color color : createGradient(colors[section], colors[section + 1], numSteps / numSections)) {
                //copy the sub-gradient into the overall gradient
                gradient[gradientIndex++] = color;
            }
        }

        if (gradientIndex < numSteps) {
            //The rounding didn't work out in our favor, and there is at least
            // one unfilled slot in the gradient[] array.
            //We can just copy the final color there
            for (/* nothing to initialize */; gradientIndex < numSteps; gradientIndex++) {
                gradient[gradientIndex] = colors[colors.length - 1];
            }
        }

        return gradient;
    }

    public static double[] smoothArray(double[] inputArray, double sigma) {
        if (inputArray == null || inputArray.length == 0 || sigma <= 0) {
            // Handle invalid input
            return new double[0];
        }

        int n = inputArray.length;
        double[] smoothedArray = new double[n];

        double[] kernel = generateGaussianKernel(sigma);

        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j < kernel.length; j++) {
                int index = i + j - kernel.length / 2;
                if (index >= 0 && index < n) {
                    sum += inputArray[index] * kernel[j];
                }
            }
            smoothedArray[i] = sum;
        }

        return smoothedArray;
    }

    private static double[] generateGaussianKernel(double sigma) {
        int size = (int) Math.ceil(sigma * 6);
        if (size % 2 == 0) {
            size++; // Ensure the size is odd
        }

        double[] kernel = new double[size];
        double sum = 0.0;

        for (int i = 0; i < size; i++) {
            double x = i - size / 2;
            kernel[i] = Math.exp(-0.5 * (x * x) / (sigma * sigma));
            sum += kernel[i];
        }

        // Normalize the kernel
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }

    /**
     * Updates the gradient used to display the data. Calls drawData() and
     * repaint() when finished.
     *
     * @param colors A variable of type Color[]
     */
    public void updateGradient(Color[] colors) {
        this.colors = colors.clone();

        if (data != null) {
            updateDataColors();
        }
    }

    /**
     * This uses the current array of colors that make up the gradient, and
     * assigns a color index to each data point, stored in the dataColorIndices
     * array, which is used by the drawData() method to plot the points.
     */
    private void updateDataColors() {
        //We need to find the range of the data values,
        // in order to assign proper colors.
        double largest = Double.MIN_VALUE;
        double smallest = Double.MAX_VALUE;
        for (double datum : data) {
            largest = Math.max(datum, largest);
            smallest = Math.min(datum, smallest);
        }
        double range = largest - smallest;

        // dataColorIndices is the same size as the data array
        // It stores an int index into the color array
        dataColorIndices = new int[data.length];

        //assign a Color to each data point
        for (int x = 0; x < data.length; x++) {
            double norm = (data[x] - smallest) / range; // 0 < norm < 1
            int colorIndex = (int) Math.floor(norm * (colors.length - 1));
            dataColorIndices[x] = colorIndex;
        }
    }

    /**
     * Creates a BufferedImage of the actual data plot.
     * <p>
     * After doing some profiling, it was discovered that 90% of the drawing
     * time was spend drawing the actual data (not on the axes or tick marks).
     * Since the Graphics2D has a drawImage method that can do scaling, we are
     * using that instead of scaling it ourselves. We only need to draw the
     * data into the bufferedImage on startup, or if the data or gradient
     * changes. This saves us an enormous amount of time. Thanks to
     * Josh Hayes-Sheen (grey@grevian.org) for the suggestion and initial code
     * to use the BufferedImage technique.
     * <p>
     * Since the scaling of the data plot will be handled by the drawImage in
     * paintComponent, we take the easy way out and draw our bufferedImage with
     * 1 pixel per data point. Too bad there isn't a setPixel method in the
     * Graphics2D class, it seems a bit silly to fill a rectangle just to set a
     * single pixel...
     * <p>
     * This function should be called whenever the data or the gradient changes.
     */
    public BufferedImage drawData() {
        var bufferedImage = new BufferedImage(data.length, 1, BufferedImage.TYPE_INT_ARGB);
        var bufferedGraphics = bufferedImage.createGraphics();

        for (int x = 0; x < data.length; x++) {
            bufferedGraphics.setColor(colors[dataColorIndices[x]]);
            bufferedGraphics.fillRect(x, 0, 1, 1);
        }
        return bufferedImage;
    }
}
