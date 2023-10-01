package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DEdgeDetails;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DNodeDetails;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;
import com.felixkroemer.trace_graph_engineering_tool.render.immed.GraphGraphics;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.GraphRenderer;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.LabelInfoProvider;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.RenderDetailFlags;
import org.cytoscape.view.model.CyNetworkViewSnapshot;

public class EdgeThumbnailCanvas<GP extends GraphicsProvider> extends DingCanvas<GP> {

    private final CyNetworkViewSnapshot snapshot;

    public EdgeThumbnailCanvas(GP graphics, CyNetworkViewSnapshot snapshot) {
        super(graphics);
        this.snapshot = snapshot;
    }

    @Override
    public String getCanvasDebugName() {
        return "Edge";
    }

    @Override
    public void paint(ProgressMonitor pm, RenderDetailFlags flags) {
        var graphics = new GraphGraphics(graphicsProvider);
        var edgeDetails = new DEdgeDetails(null);
        var nodeDetails = new DNodeDetails(null);
        var labelInfoCache = LabelInfoProvider.NO_CACHE;

        GraphRenderer.renderEdges(pm, graphics, snapshot, flags, nodeDetails, edgeDetails, labelInfoCache);
    }

}
