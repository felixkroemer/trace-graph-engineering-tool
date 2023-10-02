package com.felixkroemer.trace_graph_engineering_tool;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMappingFactory;
import com.felixkroemer.trace_graph_engineering_tool.tasks.*;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

import java.util.Map;
import java.util.Properties;

import static com.felixkroemer.trace_graph_engineering_tool.controller.RenderingController.*;
import static org.cytoscape.work.ServiceProperties.*;


public class CyActivator extends AbstractCyActivator {

    public CyActivator() {
        super();
    }

    public void start(BundleContext bundleContext) {

        CyServiceRegistrar reg = getService(bundleContext, CyServiceRegistrar.class);

        TraceGraphPanel panel = new TraceGraphPanel(reg);
        TraceGraphManager manager = new TraceGraphManager(reg, panel);

        registerService(bundleContext, manager, TraceGraphManager.class, new Properties());
        registerService(bundleContext, manager, NetworkAboutToBeDestroyedListener.class, new Properties());
        registerService(bundleContext, manager, SetCurrentNetworkListener.class, new Properties());

        LoadNetworkTaskFactory loadNetworkTaskFactory = new LoadNetworkTaskFactory(reg);
        registerService(bundleContext, loadNetworkTaskFactory, TaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "File.Import", TITLE, "Import Trace Graph",
                        INSERT_SEPARATOR_BEFORE, "true")));

        TooltipMappingFactory tooltipMappingFactory = new TooltipMappingFactory(reg);
        registerService(bundleContext, tooltipMappingFactory, VisualMappingFunctionFactory.class,
                Util.genProperties(Map.of("service.type", "factory", "mapping.type", "tooltip")));

        RenderingModeTaskFactory selectedModeTaskFactory = new RenderingModeTaskFactory(reg, RENDERING_MODE_SELECTED);
        registerService(bundleContext, selectedModeTaskFactory, NetworkViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Selected Mode")));

        RenderingModeTaskFactory fullModeTaskFactory = new RenderingModeTaskFactory(reg, RENDERING_MODE_FULL);
        registerService(bundleContext, fullModeTaskFactory, NetworkViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Full Mode")));

        RenderingModeTaskFactory tracesModeTaskFactory = new RenderingModeTaskFactory(reg, RENDERING_MODE_TRACES);
        registerService(bundleContext, tracesModeTaskFactory, NetworkViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Traces Mode")));

        var showTraceDetailsNodeTaskFactory = new ShowTraceDetailsNodeTaskFactory(reg);
        registerService(bundleContext, showTraceDetailsNodeTaskFactory, NodeViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph", TITLE, "Show Trace Details")));

        var showTraceDetailsEdgeTaskFactory = new ShowTraceDetailsEdgeTaskFactory(reg);
        registerService(bundleContext, showTraceDetailsEdgeTaskFactory, EdgeViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph", TITLE, "Show Trace Details")));

        var viewDefaultViewNetworkTaskFactory = new ViewDefaultViewNetworkTaskFactory(reg);
        registerService(bundleContext, viewDefaultViewNetworkTaskFactory, NetworkViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph", TITLE, "Return to default network view")));

        var viewDefaultViewNodeTaskFactory = new ViewDefaultViewNodeTaskFactory(reg);
        registerService(bundleContext, viewDefaultViewNodeTaskFactory, NodeViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph", TITLE, "Return to selected node")));

        registerServiceListener(bundleContext, this, "handleControllerRegistration", "handleControllerDeregistration"
                , TraceGraphManager.class);

        new com.felixkroemer.trace_graph_engineering_tool.renderer.ding.CyActivator().start(bundleContext);

    }

    public void handleControllerDeregistration(TraceGraphManager manager, Map<Object, Object> serviceProps) {
        manager.clearTraceGraphs();
    }

    public void handleControllerRegistration(TraceGraphManager manager, Map<Object, Object> serviceProps) {
    }

}

