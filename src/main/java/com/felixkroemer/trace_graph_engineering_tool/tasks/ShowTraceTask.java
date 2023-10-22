package com.felixkroemer.trace_graph_engineering_tool.tasks;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.events.ShowTraceEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceExtension;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShowTraceTask extends AbstractTask {
    private CyServiceRegistrar registrar;
    private Logger logger;
    private CyNetworkView networkView;

    public ShowTraceTask(CyServiceRegistrar reg, CyNetworkView networkView) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.registrar = reg;
        this.networkView = networkView;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        var nodes = CyTableUtil.getNodesInState(networkView.getModel(), CyNetwork.SELECTED, true);
        CyEventHelper helper = registrar.getService(CyEventHelper.class);

        var manager = registrar.getService(TraceGraphManager.class);
        var controller = (TraceGraphController) manager.findControllerForNetwork(networkView.getModel());

        if (controller != null) {
            var trace = controller.getTraceGraph().findTrace(nodes);
            if (trace != null) {
                TraceExtension extension = new TraceExtension(trace);
                helper.fireEvent(new ShowTraceEvent(this, extension, networkView.getModel()));
            }
        }
    }

}
