package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.CyServiceRegistrar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TraceGraphManager implements NetworkAboutToBeDestroyedListener, SetCurrentNetworkListener {

    private CyServiceRegistrar registrar;
    private Map<ParameterDiscretizationModel, Set<TraceGraphController>> controllers;
    private final TraceGraphPanel panel;

    public TraceGraphManager(CyServiceRegistrar registrar, TraceGraphPanel panel) {
        this.registrar = registrar;
        this.panel = panel;
        this.controllers = new HashMap<>();
    }

    public void registerTraceGraph(ParameterDiscretizationModel pdm, TraceGraph traceGraph) {
        TraceGraphController controller = new TraceGraphController(registrar, traceGraph);
        this.controllers.computeIfAbsent(pdm, k -> new HashSet<>());
        this.controllers.get(pdm).add(controller);
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
            //TODO
            this.controllers.remove(controller);
        }
        if (this.controllers.isEmpty()) {
            this.hidePanel();
        }
    }

    public TraceGraphController findControllerForNetwork(CyNetwork network) {
        for (var entry : this.controllers.entrySet()) {
            for (var controller : entry.getValue()) {
                if (controller.containsNetwork(network)) {
                    return controller;
                }
            }
        }
        return null;
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() != null && Util.isTraceGraphNetwork(e.getNetwork())) {
            var controller = findControllerForNetwork(e.getNetwork());
            this.panel.registerCallbacks(controller, controller.getUiState());
        } else {
            this.panel.clear();
        }
    }

    public void clearTraceGraphs() {
        for (var entry : this.controllers.entrySet()) {
            for (var controller : entry.getValue()) {
                controller.destroy();
            }
        }
        this.controllers.clear();
    }

}
