package com.felixkroemer.trace_graph_engineering_tool;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.tasks.*;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

import java.util.Map;
import java.util.Properties;

import static com.felixkroemer.trace_graph_engineering_tool.display_controller.DefaultEdgeDisplayController.RENDERING_MODE_FULL;
import static com.felixkroemer.trace_graph_engineering_tool.display_controller.FollowEdgeDisplayController.RENDERING_MODE_FOLLOW;
import static com.felixkroemer.trace_graph_engineering_tool.display_controller.TracesEdgeDisplayController.RENDERING_MODE_TRACES;
import static org.cytoscape.work.ServiceProperties.*;

@SuppressWarnings("unused")
public class CyActivator extends AbstractCyActivator {

    private TraceGraphManager manager;

    public void start(BundleContext bundleContext) {
        CyServiceRegistrar reg = getService(bundleContext, CyServiceRegistrar.class);

        this.manager = new TraceGraphManager(reg);

        registerService(bundleContext, manager, TraceGraphManager.class, new Properties());
        registerService(bundleContext, manager, NetworkAboutToBeDestroyedListener.class, new Properties());

        ProfilingTaskFactory profilingTaskFactory = new ProfilingTaskFactory(reg);
        registerService(bundleContext, profilingTaskFactory, TaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Profile", TITLE, "Profile application")));

        LoadPDMTaskFactory loadPDMTaskFactory = new LoadPDMTaskFactory(reg);
        registerService(bundleContext, loadPDMTaskFactory, TaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "File.Import", TITLE, "Import PDM or trace", INSERT_SEPARATOR_BEFORE, "true")));

        ExportPDMTaskFactory exportPDMTaskFactory = new ExportPDMTaskFactory(reg);
        registerService(bundleContext, exportPDMTaskFactory, NetworkViewTaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "File.Export", TITLE, "Export PDM to json")));

        RenderingModeTaskFactory followModeTaskFactory = new RenderingModeTaskFactory(reg, RENDERING_MODE_FOLLOW);
        registerService(bundleContext, followModeTaskFactory, NetworkViewTaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Follow Mode")));

        RenderingModeTaskFactory fullModeTaskFactory = new RenderingModeTaskFactory(reg, RENDERING_MODE_FULL);
        registerService(bundleContext, fullModeTaskFactory, NetworkViewTaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Full Mode")));

        RenderingModeTaskFactory tracesModeTaskFactory = new RenderingModeTaskFactory(reg, RENDERING_MODE_TRACES);
        registerService(bundleContext, tracesModeTaskFactory, NetworkViewTaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Traces Mode")));

        var showTraceNodeTaskFactory = new ShowTraceNodeTaskFactory(reg);
        registerService(bundleContext, showTraceNodeTaskFactory, NodeViewTaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph", TITLE, "Show Trace")));

        var compareTraceGraphsTaskFactory = new CompareTraceGraphsTaskFactory(reg);
        registerService(bundleContext, compareTraceGraphsTaskFactory, NetworkCollectionTaskFactory.class, Util.genProperties(Map.of(TITLE, "Compare Trace Graphs", IN_NETWORK_PANEL_CONTEXT_MENU, "true", MENU_GRAVITY, "1.")));

        var splitTraceGraphTaskFactory = new SplitTraceGraphTaskFactory(reg);
        registerService(bundleContext, splitTraceGraphTaskFactory, NetworkCollectionTaskFactory.class, Util.genProperties(Map.of(TITLE, "Split Trace Graph", IN_NETWORK_PANEL_CONTEXT_MENU, "true", MENU_GRAVITY, "1.3")));

        var combineTraceGraphsTaskFactory = new CombineTraceGraphsTaskFactory(reg);
        registerService(bundleContext, combineTraceGraphsTaskFactory, NetworkCollectionTaskFactory.class, Util.genProperties(Map.of(TITLE, "Combine Trace Graphs", IN_NETWORK_PANEL_CONTEXT_MENU, "true", MENU_GRAVITY, "1.2")));

        var setPercentileFilterTaskFactory = new SetPercentileFilterTaskFactory(reg);
        registerService(bundleContext, setPercentileFilterTaskFactory, NetworkViewTaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph", TITLE, "Set Percentile Filter")));

        var customTaskFactory = new CustomTaskFactory(reg);
        registerService(bundleContext, customTaskFactory, NetworkViewTaskFactory.class, Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph", TITLE, "Custom Task")));

        new com.felixkroemer.trace_graph_engineering_tool.renderer.ding.CyActivator().start(bundleContext);
    }

    @Override
    public void shutDown() {
        this.manager.dispose();
    }
}

