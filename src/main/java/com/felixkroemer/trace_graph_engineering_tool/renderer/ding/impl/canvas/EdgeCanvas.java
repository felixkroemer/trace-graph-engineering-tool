package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DRenderingEngine;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;
import com.felixkroemer.trace_graph_engineering_tool.render.immed.GraphGraphics;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.GraphRenderer;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.LabelInfoProvider;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.RenderDetailFlags;

public class EdgeCanvas<GP extends GraphicsProvider> extends DingCanvas<GP> {

    private final DRenderingEngine re;
    private final GraphGraphics graphGraphics;

    public EdgeCanvas(GP graphics, DRenderingEngine re) {
        super(graphics);
        this.re = re;
        this.graphGraphics = new GraphGraphics(graphics);
    }

    @Override
    public String getCanvasDebugName() {
        return "Edges";
    }

    @Override
    public void paint(ProgressMonitor pm, RenderDetailFlags flags) {
        var netViewSnapshot = re.getViewModelSnapshot();
        var edgeDetails = re.getEdgeDetails();
        var nodeDetails = re.getNodeDetails();
        var labelProvider = flags.has(RenderDetailFlags.OPT_LABEL_CACHE) ? re.getLabelCache() :
				LabelInfoProvider.NO_CACHE;

        graphGraphics.update(flags, true);

        GraphRenderer.renderEdges(pm, graphGraphics, netViewSnapshot, flags, nodeDetails, edgeDetails, labelProvider);
    }

}
