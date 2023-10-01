package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;
import com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.stateful.RenderDetailFlags;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2021 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

/**
 *
 */
public abstract class DingCanvas<GP extends GraphicsProvider> {

    protected GP graphicsProvider;

    public DingCanvas(GP graphicsProvider) {
        this.graphicsProvider = graphicsProvider;
    }


    public abstract void paint(ProgressMonitor pm, RenderDetailFlags flags);

    public abstract String getCanvasDebugName();


    public GP getGraphicsProvier() {
        return graphicsProvider;
    }

    public void setGraphicsProvider(GP graphicsProvider) {
        this.graphicsProvider = graphicsProvider;
    }

    public GP paintAndGet(ProgressMonitor pm, RenderDetailFlags flags) {
        pm = ProgressMonitor.notNull(pm);
        if (pm.isCancelled()) return graphicsProvider;
        pm.start(getCanvasDebugName());
        paint(pm, flags);
        pm.done();
        return graphicsProvider;
    }

    public GP getCurrent(ProgressMonitor pm) {
        pm = ProgressMonitor.notNull(pm);
        pm.emptyTask(getCanvasDebugName());
        return graphicsProvider;
    }

    public void dispose() {
    }
}
