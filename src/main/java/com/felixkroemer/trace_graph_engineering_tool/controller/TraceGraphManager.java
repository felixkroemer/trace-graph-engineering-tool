package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.tasks.ShowTraceDetailsTaskFactory;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkViewTaskFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

public class TraceGraphManager implements NetworkAboutToBeDestroyedListener, SetCurrentNetworkListener {

    private CyServiceRegistrar registrar;
    private Set<TraceGraphController> controllers;
    private final TraceGraphPanel panel;
    private final ShowTraceDetailsTaskFactory showTraceDetailsTaskFactory;

    public TraceGraphManager(CyServiceRegistrar registrar, TraceGraphPanel panel) {
        this.registrar = registrar;
        this.panel = panel;
        this.controllers = new HashSet<>();
        this.showTraceDetailsTaskFactory = new ShowTraceDetailsTaskFactory(registrar);
        this.registrar.registerService(showTraceDetailsTaskFactory, NetworkViewTaskFactory.class,
                Util.genProperties(Map.of(PREFERRED_MENU, "Trace Graph.Tasks", TITLE, "Show Trace Details")));
    }

    public void registerTraceGraph(TraceGraph traceGraph) {
        TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
        this.controllers.add(controller);
        controller.registerNetwork();
        this.showPanel();
    }

    private void showPanel() {
        CySwingApplication swingApplication = registrar.getService(CySwingApplication.class);
        if (swingApplication.getCytoPanel(CytoPanelName.WEST).indexOfComponent("TraceGraphPanel") < 0) {
            this.registrar.registerService(this.panel, CytoPanelComponent.class);
        }
    }

    private void hidePanel() {
        this.registrar.unregisterService(this.panel, CytoPanelComponent.class);
    }

    @Override
    public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
        // TODO: find way to refer from CyNetwork to TraceGraph
        TraceGraphController controller = findControllerForNetwork(e.getNetwork());
        if (controller != null) {
            controller.unregister();
            this.controllers.remove(controller);
        }
        if (this.controllers.isEmpty()) {
            this.hidePanel();
        }
    }

    public TraceGraphController findControllerForNetwork(CyNetwork network) {
        for (var controller : this.controllers) {
            if (controller.getTraceGraph().getNetwork() == network) {
                return controller;
            }
        }
        return null;
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() != null && Util.isTraceGraphNetwork(e.getNetwork())) {
            var controller = findControllerForNetwork(e.getNetwork());
            this.panel.registerCallbacks(controller);
        } else {
            this.panel.clear();
        }
    }

    public void clearTraceGraphs() {
        for (var controller : this.controllers) {
            //network destroyed handler will delete it from this.traceGraphs
            var networkManager = registrar.getService(CyNetworkManager.class);
            networkManager.destroyNetwork(controller.getTraceGraph().getNetwork());
        }
    }

    public ShowTraceDetailsTaskFactory getShowTraceDetailsTaskFactory() {
        return this.showTraceDetailsTaskFactory;
    }

}
