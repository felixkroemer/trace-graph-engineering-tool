package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.debug;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas.CompositeImageCanvas;
import com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.stateful.RenderDetailFlags;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugRootFrameInfo extends DebugFrameInfo {

    private static AtomicInteger frameCounter = new AtomicInteger();

    private final DebugFrameType type;
    private final boolean cancelled;
    private final int frameNumber;
    private final long start;
    private final long end;

    private final RenderDetailFlags flags;
    private final CompositeImageCanvas.PaintParameters paintParams;


    private DebugRootFrameInfo(String task, long start, long end, DebugFrameType type, boolean cancelled,
                               RenderDetailFlags flags, CompositeImageCanvas.PaintParameters paintParams,
                               List<DebugFrameInfo> subFrames) {
        super(task, end - start, subFrames);
        this.type = type;
        this.cancelled = cancelled;
        this.flags = flags;
        this.paintParams = paintParams;
        this.start = start;
        this.end = end;
        this.frameNumber = frameCounter.incrementAndGet();
    }


    public DebugFrameType getType() {
        return type;
    }

    public long getStartTime() {
        return start;
    }

    public long getEndTime() {
        return end;
    }

    public RenderDetailFlags getRenderDetailFlags() {
        return flags;
    }

    public CompositeImageCanvas.PaintParameters getPaintParameters() {
        return paintParams;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public static DebugRootFrameInfo fromProgressMonitor(DebugRootProgressMonitor pm) {
        DebugFrameType type = pm.getType();
        boolean cancelled = pm.isCancelled();
        long start = pm.getStartTime();
        long end = pm.getEndTime();
        RenderDetailFlags flags = pm.getRenderDetailFlags();
        CompositeImageCanvas.PaintParameters paintParams = pm.getPaintParametsr();
        String task = pm.getTaskName();
        var subInfos = DebugUtil.map(pm.getSubMonitors(), x -> fromSubPM(x));
        return new DebugRootFrameInfo(task, start, end, type, cancelled, flags, paintParams, subInfos);
    }


}
