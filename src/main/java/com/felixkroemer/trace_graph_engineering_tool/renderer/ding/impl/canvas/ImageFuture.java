package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;
import com.felixkroemer.trace_graph_engineering_tool.render.stateful.RenderDetailFlags;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ImageFuture {

    private final CompletableFuture<Image> future;
    private final RenderDetailFlags lastRenderDetail;
    private final ProgressMonitor progressMonitor;


    public ImageFuture(CompletableFuture<Image> future, RenderDetailFlags lastRenderDetail,
					   ProgressMonitor progressMonitor) {
        this.future = Objects.requireNonNull(future);
        this.lastRenderDetail = Objects.requireNonNull(lastRenderDetail);
        this.progressMonitor = ProgressMonitor.notNull(progressMonitor);
    }

    public ImageFuture(CompletableFuture<Image> future, RenderDetailFlags lastRenderDetail) {
        this(future, lastRenderDetail, null);
    }

    public ImageFuture(Image image, RenderDetailFlags lastRenderDetail, ProgressMonitor progressMonitor) {
        this(CompletableFuture.completedFuture(image), lastRenderDetail, progressMonitor);
    }

    public ImageFuture(Image image, RenderDetailFlags lastRenderDetail) {
        this(CompletableFuture.completedFuture(image), lastRenderDetail, null);
    }

    public void cancel() {
        progressMonitor.cancel();
    }

    public boolean isCancelled() {
        return progressMonitor.isCancelled();
    }

    public boolean isCompletedExceptionally() {
        return future.isCompletedExceptionally();
    }

    public Image join() {
        return future.join();
    }

    public void thenRun(Runnable r) {
        future.thenRun(() -> {
            if (!progressMonitor.isCancelled()) {
                r.run();
            }
        });
    }

    public void thenAccept(Consumer<ImageFuture> c) {
        future.thenRun(() -> {
            if (!progressMonitor.isCancelled()) {
                c.accept(this);
            }
        });
    }

    public RenderDetailFlags getLastRenderDetail() {
        return lastRenderDetail;
    }

    public boolean isReady() {
        return !progressMonitor.isCancelled() && future.isDone();
    }
}
