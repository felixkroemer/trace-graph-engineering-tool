package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DNodeDetails;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;
import com.felixkroemer.trace_graph_engineering_tool.render.immed.GraphGraphics;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.GraphRenderer;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.LabelInfoProvider;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.RenderDetailFlags;
import org.cytoscape.view.model.CyNetworkViewSnapshot;

public class NodeThumbnailCanvas<GP extends GraphicsProvider> extends DingCanvas<GP> {

    private final CyNetworkViewSnapshot snapshot;

    public NodeThumbnailCanvas(GP graphics, CyNetworkViewSnapshot snapshot) {
        super(graphics);
        this.snapshot = snapshot;
    }

    @Override
    public String getCanvasDebugName() {
        return "Node";
    }

    @Override
    public void paint(ProgressMonitor pm, RenderDetailFlags flags) {
        var graphics = new GraphGraphics(graphicsProvider);
        var nodeDetails = new DNodeDetails(null);
        var labelInfoCache = LabelInfoProvider.NO_CACHE;

        GraphRenderer.renderNodes(pm, graphics, snapshot, flags, nodeDetails, null, labelInfoCache);
    }

}
