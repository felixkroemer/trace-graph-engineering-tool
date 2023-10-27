package com.felixkroemer.trace_graph_engineering_tool;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.tasks.*;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

import static com.felixkroemer.trace_graph_engineering_tool.display_controller.DefaultDisplayController.RENDERING_MODE_FULL;
import static com.felixkroemer.trace_graph_engineering_tool.display_controller.FollowDisplayController.RENDERING_MODE_FOLLOW;
import static com.felixkroemer.trace_graph_engineering_tool.display_controller.TracesDisplayController.RENDERING_MODE_TRACES;
import static org.cytoscape.work.ServiceProperties.*;


public class CyActivator extends AbstractCyActivator {

    private Logger logger;
    private TraceGraphManager manager;

    public CyActivator() {
        super();
    }

    public void start(BundleContext bundleContext) {

        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);

        CyServiceRegistrar reg = getService(bundleContext, CyServiceRegistrar.class);

        this.manager = new TraceGraphManager(reg);

        registerService(bundleContext, manager, TraceGraphManager.class, new Properties());
        registerService(bundleContext, manager, NetworkAboutToBeDestroyedListener.class, new Properties());

        LoadPDMTaskFactory loadPDMTaskFactory = new LoadPDMTaskFactory(reg);
        registerService(bundleContext, loadPDMTaskFactory, TaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "File.Import", TITLE, "Import PDM", INSERT_SEPARATOR_BEFORE
                        , "true")));

        LoadTraceTaskFactory loadTraceTaskFactory = new LoadTraceTaskFactory(reg);
        registerService(bundleContext, loadTraceTaskFactory, TaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "File.Import", TITLE, "Import Trace",
                        INSERT_SEPARATOR_BEFORE, "true")));

        RenderingModeTaskFactory followModeTaskFactory = new RenderingModeTaskFactory(reg, RENDERING_MODE_FOLLOW);
        registerService(bundleContext, followModeTaskFactory, NetworkViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Follow Mode")));

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

        var showTraceNodeTaskFactory = new ShowTraceNodeTaskFactory(reg);
        registerService(bundleContext, showTraceNodeTaskFactory, NodeViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph", TITLE, "Show Trace")));


        var compareTraceGraphsTaskFactory = new CompareTraceGraphsTaskFactory(reg);
        registerService(bundleContext, compareTraceGraphsTaskFactory, NetworkCollectionTaskFactory.class,
                Util.genProperties(Map.of(TITLE, "Compare Trace Graphs", IN_NETWORK_PANEL_CONTEXT_MENU, "true")));

        var splitTraceGraphTaskFactory = new SplitTraceGraphTaskFactory(reg);
        registerService(bundleContext, splitTraceGraphTaskFactory, NetworkCollectionTaskFactory.class,
                Util.genProperties(Map.of(TITLE, "Split Trace Graph", IN_NETWORK_PANEL_CONTEXT_MENU, "true")));

        var combineTraceGraphsTaskFactory = new CombineTraceGraphsTaskFactory(reg);
        registerService(bundleContext, combineTraceGraphsTaskFactory, NetworkCollectionTaskFactory.class,
                Util.genProperties(Map.of(TITLE, "Combine Trace Graphs", IN_NETWORK_PANEL_CONTEXT_MENU, "true")));

        new com.felixkroemer.trace_graph_engineering_tool.renderer.ding.CyActivator().start(bundleContext);

    }

    @Override
    public void shutDown() {
        this.manager.destroy();
    }

}

