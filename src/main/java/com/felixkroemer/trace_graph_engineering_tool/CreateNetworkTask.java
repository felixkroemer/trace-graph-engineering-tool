package com.felixkroemer.trace_graph_engineering_tool;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class CreateNetworkTask extends AbstractTask {

    private final CyNetworkManager netMgr;
    private final CyNetworkFactory cnf;
    private final CyNetworkNaming namingUtil;
    private final CyApplicationManager applicationManager;
    private final CyNetworkViewManager networkViewManager;
    private final CyNetworkViewFactory networkViewFactory;

    public CreateNetworkTask(final CyNetworkManager netMgr, final CyNetworkNaming namingUtil, final CyNetworkFactory cnf, final CyApplicationManager applicationManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager) {
        this.netMgr = netMgr;
        this.cnf = cnf;
        this.namingUtil = namingUtil;
        this.applicationManager = applicationManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
    }

    public void run(TaskMonitor monitor) {
    }
}
