package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DRenderingEngine;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.RenderDetailFlags;

import java.awt.*;

public class SelectionCanvas<GP extends GraphicsProvider> extends DingCanvas<GP> {

    public static final String DEBUG_NAME = "Selection";

    private final DRenderingEngine re;
    private boolean show = true;

    public SelectionCanvas(GP graphics, DRenderingEngine re) {
        super(graphics);
        this.re = re;
    }

    @Override
    public String getCanvasDebugName() {
        return DEBUG_NAME;
    }

    public void paint(ProgressMonitor pm, RenderDetailFlags flags) {
        if (!show) return;

        Graphics2D g = graphicsProvider.getGraphics(true);
        if (g == null) return;

        Graphics2D g1 = (Graphics2D) g.create();
        var annotationSelection = re.getCyAnnotator().getAnnotationSelection();
        annotationSelection.paint(g1);
        g1.dispose();

        Graphics2D g2 = (Graphics2D) g.create();
        var labelSelection = re.getLabelSelectionManager();
        labelSelection.paint(g2);
        g2.dispose();

        g.dispose();
    }

    public void show(boolean show) {
        this.show = show;
    }
}
