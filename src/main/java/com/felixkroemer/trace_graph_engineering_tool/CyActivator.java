package com.felixkroemer.trace_graph_engineering_tool;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.tasks.LoadNetworkTaskFactory;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

import java.util.Map;
import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;


public class CyActivator extends AbstractCyActivator {

    public CyActivator() {
        super();
    }

    public void start(BundleContext bundleContext) {

        CyServiceRegistrar reg = getService(bundleContext, CyServiceRegistrar.class);
        TraceGraphController manager = new TraceGraphController(reg);

        registerService(bundleContext, manager, TraceGraphController.class, new Properties());

        LoadNetworkTaskFactory loadNetworkTaskFactory = new LoadNetworkTaskFactory(reg);
        registerService(bundleContext, loadNetworkTaskFactory, TaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Apps.STRING", TITLE, "Import Trace Graph")));
    }

}

