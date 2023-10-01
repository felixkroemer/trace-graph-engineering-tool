package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.debug;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas.CompositeImageCanvas;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;
import com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.stateful.RenderDetailFlags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DebugRootProgressMonitor implements DebugProgressMonitor {

    private final ProgressMonitor delegate;
    private final DebugProgressMonitorCallback callback;
    private final DebugFrameType type;

    private List<DebugSubProgressMonitor> subMonitors = Collections.emptyList();

    private long start, end;
    private String taskName;

    private RenderDetailFlags flags;
    private CompositeImageCanvas.PaintParameters paintParams;

    public DebugRootProgressMonitor(DebugFrameType type, ProgressMonitor delegate,
                                    DebugProgressMonitorCallback callback) {
        this.type = type;
        this.delegate = ProgressMonitor.notNull(delegate);
        this.callback = callback;
    }

    @Override
    public void start(String taskName) {
        this.taskName = taskName;
        start = System.currentTimeMillis();
        delegate.start(taskName);
    }

    @Override
    public void done() {
        delegate.done();
    }

    public void done(RenderDetailFlags flags, CompositeImageCanvas.PaintParameters paintParams) {
        this.end = System.currentTimeMillis();
        this.flags = flags;
        this.paintParams = paintParams;
        done();
        if (callback != null) {
            callback.addFrame(DebugRootFrameInfo.fromProgressMonitor(this));
        }
    }

    @Override
    public ProgressMonitor[] split(double... parts) {
        subMonitors = new ArrayList<>(parts.length);
        return DebugProgressMonitor.super.split(parts);
    }

    public String getTaskName() {
        return taskName;
    }

    @Override
    public ProgressMonitor createSubProgressMonitor(double percent) {
        var sub = new DebugSubProgressMonitor(this, percent);
        subMonitors.add(sub);
        return sub;
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public void addProgress(double progress) {
        delegate.addProgress(progress);
    }

    @Override
    public List<DebugSubProgressMonitor> getSubMonitors() {
        return subMonitors;
    }

    public DebugFrameType getType() {
        return type;
    }

    public RenderDetailFlags getRenderDetailFlags() {
        return flags;
    }

    public CompositeImageCanvas.PaintParameters getPaintParametsr() {
        return paintParams;
    }

    public long getStartTime() {
        return start;
    }

    public long getEndTime() {
        return end;
    }

    @Override
    public long getTime() {
        return end - start;
    }


}
