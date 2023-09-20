package com.felixkroemer.trace_graph_engineering_tool;

import com.felixkroemer.trace_graph_engineering_tool.controller.RenderingMode;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.mappings.TooltipMappingFactory;
import com.felixkroemer.trace_graph_engineering_tool.tasks.LoadNetworkTaskFactory;
import com.felixkroemer.trace_graph_engineering_tool.tasks.ManualTaskFactory;
import com.felixkroemer.trace_graph_engineering_tool.tasks.RenderingModeTaskFactory;
import com.felixkroemer.trace_graph_engineering_tool.tasks.ResetTaskFactory;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

import java.util.Map;
import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.*;


public class CyActivator extends AbstractCyActivator implements SetCurrentNetworkListener {

    public CyActivator() {
        super();
    }

    public void start(BundleContext bundleContext) {

        CyServiceRegistrar reg = getService(bundleContext, CyServiceRegistrar.class);

        TraceGraphPanel panel = new TraceGraphPanel(reg);
        TraceGraphController controller = new TraceGraphController(reg, panel);

        registerService(bundleContext, controller, TraceGraphController.class, new Properties());
        registerService(bundleContext, controller, NetworkAboutToBeDestroyedListener.class, new Properties());
        registerService(bundleContext, controller, SetCurrentNetworkListener.class, new Properties());
        registerService(bundleContext, controller, SelectedNodesAndEdgesListener.class, new Properties());

        LoadNetworkTaskFactory loadNetworkTaskFactory = new LoadNetworkTaskFactory(reg);
        registerService(bundleContext, loadNetworkTaskFactory, TaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "File.Import", TITLE, "Import Trace Graph",
                        INSERT_SEPARATOR_BEFORE, "true")));


        ResetTaskFactory resetTaskFactory = new ResetTaskFactory(reg);
        registerService(bundleContext, resetTaskFactory, TaskFactory.class, Util.genProperties(Map.of(TITLE, "RESET",
                IN_TOOL_BAR, "true", MENU_GRAVITY, "4.10", LARGE_ICON_ID, "cy::IMPORT_NET")));

        ManualTaskFactory manualTaskFactory = new ManualTaskFactory(reg);
        registerService(bundleContext, manualTaskFactory, TaskFactory.class, Util.genProperties(Map.of(TITLE, "MANUAL"
                , IN_TOOL_BAR, "true", MENU_GRAVITY, "4.11", LARGE_ICON_ID, "cy::IMPORT_NET")));

        TooltipMappingFactory tooltipMappingFactory = new TooltipMappingFactory(reg);
        registerService(bundleContext, tooltipMappingFactory, VisualMappingFunctionFactory.class,
                Util.genProperties(Map.of("service.type", "factory", "mapping.type", "tooltip")));

        RenderingModeTaskFactory selectedModeTaskFactory = new RenderingModeTaskFactory(reg, RenderingMode.SELECTED);
        registerService(bundleContext, selectedModeTaskFactory, NetworkViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Selected Mode")));

        RenderingModeTaskFactory fullModeTaskFactory = new RenderingModeTaskFactory(reg, RenderingMode.FULL);
        registerService(bundleContext, fullModeTaskFactory, NetworkViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Modes", TITLE, "Use Full Mode")));

        registerServiceListener(bundleContext, this, "handleControllerRegistration", "handleControllerDeregistration"
                , TraceGraphController.class);

    }

    public void handleControllerDeregistration(TraceGraphController controller, Map<Object, Object> serviceProps) {
        controller.clearTraceGraphs();
    }

    public void handleControllerRegistration(TraceGraphController controller, Map<Object, Object> serviceProps) {
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {

    }
}

