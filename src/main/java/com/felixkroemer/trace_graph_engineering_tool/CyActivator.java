package com.felixkroemer.trace_graph_engineering_tool;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

import java.util.Properties;


public class CyActivator extends AbstractCyActivator {
    public CyActivator() {
        super();
    }


    public void start(BundleContext bc) {

        CyNetworkManager cyNetworkManagerServiceRef = getService(bc, CyNetworkManager.class);
        CyNetworkNaming cyNetworkNamingServiceRef = getService(bc, CyNetworkNaming.class);
        CyNetworkFactory cyNetworkFactoryServiceRef = getService(bc, CyNetworkFactory.class);
        CyApplicationManager applicationManager = getService(bc, CyApplicationManager.class);
        CyNetworkViewFactory networkViewFactory = getService(bc, CyNetworkViewFactory.class);
        CyNetworkViewManager networkViewManager = getService(bc, CyNetworkViewManager.class);

        CreateNetworkTaskFactory createNetworkTaskFactory = new CreateNetworkTaskFactory(cyNetworkManagerServiceRef, cyNetworkNamingServiceRef, cyNetworkFactoryServiceRef, applicationManager, networkViewFactory, networkViewManager);

        Properties props = new Properties();
        registerService(bc, createNetworkTaskFactory, TaskFactory.class, props);
    }
}

