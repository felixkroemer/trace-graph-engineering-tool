package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.canvas;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DRenderingEngine;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;
import com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.immed.GraphGraphics;
import com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.stateful.GraphRenderer;
import com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.stateful.LabelInfoProvider;
import com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.stateful.RenderDetailFlags;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;

import java.util.Set;

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
 * Canvas to be used for drawing actual network visualization
 */
public class NodeCanvas<GP extends GraphicsProvider> extends DingCanvas<GP> {

    private final VisualMappingManager vmm;
    private final DRenderingEngine re;
    private final GraphGraphics graphGraphics;

    public NodeCanvas(GP graphics, DRenderingEngine re) {
        super(graphics);
        this.re = re;
        this.vmm = re.getServiceRegistrar().getService(VisualMappingManager.class);
        this.graphGraphics = new GraphGraphics(graphics);
    }

    @Override
    public String getCanvasDebugName() {
        return "Nodes";
    }

    private Set<VisualPropertyDependency<?>> getVPDeps() {
        CyNetworkView netView = re.getViewModel();
        return vmm.getVisualStyle(netView).getAllVisualPropertyDependencies();
    }

    @Override
    public void paint(ProgressMonitor pm, RenderDetailFlags flags) {
        var dependencies = getVPDeps();
        var snapshot = re.getViewModelSnapshot();

        var nodeDetails = re.getNodeDetails();
        var labelProvider = flags.has(RenderDetailFlags.OPT_LABEL_CACHE) ? re.getLabelCache() :
                LabelInfoProvider.NO_CACHE;

        graphGraphics.update(flags, true);

        GraphRenderer.renderNodes(pm, graphGraphics, snapshot, flags, nodeDetails, dependencies, labelProvider);
    }
}
