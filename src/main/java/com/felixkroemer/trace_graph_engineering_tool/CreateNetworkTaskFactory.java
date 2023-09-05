package com.felixkroemer.trace_graph_engineering_tool;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class CreateNetworkTaskFactory extends AbstractTaskFactory {
    private final CyNetworkManager netMgr;
    private final CyNetworkFactory cnf;
    private final CyNetworkNaming namingUtil;
    private final CyApplicationManager applicationManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;

    public CreateNetworkTaskFactory(final CyNetworkManager netMgr, final CyNetworkNaming namingUtil, final CyNetworkFactory cnf, CyApplicationManager applicationManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager) {
        this.netMgr = netMgr;
        this.namingUtil = namingUtil;
        this.cnf = cnf;
        this.applicationManager = applicationManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
    }


    public TaskIterator createTaskIterator() {
        return new TaskIterator(new CreateNetworkTask(netMgr, namingUtil, cnf, applicationManager, networkViewFactory, networkViewManager));
    }
}
