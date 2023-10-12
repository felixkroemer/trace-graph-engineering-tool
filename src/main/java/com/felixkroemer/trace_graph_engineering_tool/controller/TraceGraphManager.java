package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
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
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.table.CyTableViewManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class TraceGraphManager implements NetworkAboutToBeDestroyedListener,
        PropertyChangeListener {

    private CyServiceRegistrar registrar;
    private Map<ParameterDiscretizationModel, Set<TraceGraphController>> controllers;
    private final TraceGraphPanel panel;

    public TraceGraphManager(CyServiceRegistrar registrar) {
        this.registrar = registrar;
        this.panel = new TraceGraphPanel(registrar, this);
        registrar.registerService(panel, SelectedNodesAndEdgesListener.class, new Properties());
        registrar.registerService(panel, SetCurrentNetworkListener.class, new Properties());
        this.controllers = new HashMap<>();
    }

    public void registerTraceGraph(ParameterDiscretizationModel pdm, TraceGraphController controller) {
        if (this.controllers.get(pdm) == null) {
            this.controllers.put(pdm, new HashSet<>());
            pdm.getParameters().forEach(p -> p.addObserver(this));
        }
        this.controllers.get(pdm).add(controller);
        controller.registerNetwork();
        this.showPanel();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        ParameterDiscretizationModel pdm = ((Parameter) evt.getSource()).getPdm();
        switch (evt.getPropertyName()) {
            case "enabled" -> {
                var tableViewManager = registrar.getService(CyTableViewManager.class);
                for (TraceGraphController controller : this.controllers.get(pdm)) {
                    var nodeTableView =
                            tableViewManager.getTableView(controller.getTraceGraph().getNetwork().getDefaultNodeTable());
                    Parameter param = (Parameter) evt.getSource();
                    var columnView = nodeTableView.getColumnView(param.getName());
                    columnView.setVisualProperty(COLUMN_VISIBLE, evt.getNewValue());
                }
                this.updateTraceGraph(pdm, (Parameter) evt.getSource());
            }
            case "bins" -> {
                this.updateTraceGraph(pdm, (Parameter) evt.getSource());
            }
        }
    }

    private void updateTraceGraph(ParameterDiscretizationModel pdm, Parameter changedParameter) {
        for (TraceGraphController controller : controllers.get(pdm)) {
            controller.updateTraceGraph(pdm, changedParameter);
            controller.applyStyleAndLayout();
        }
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
        TraceGraphController controller = findControllerForNetwork(e.getNetwork());
        if (controller != null) {
            controller.destroy();
            var pdm = findPDMForNetwork(controller.getNetwork());
            this.controllers.get(pdm).remove(controller);
            if (controllers.get(pdm).isEmpty()) {
                controllers.remove(pdm);
            }
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

    public ParameterDiscretizationModel findPDMForNetwork(CyNetwork network) {
        for(var entry : this.controllers.entrySet()) {
            for(var controller : entry.getValue()) {
                if(controller.getNetwork() == network) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    // call to destroy networks, everything else is handled in
    // handleEvent(NetworkAboutToBeDestroyedEvent e)
    public void clearTraceGraphs() {
        for (var entry : this.controllers.entrySet()) {
            for (var controller : entry.getValue()) {
                var networkManager = registrar.getService(CyNetworkManager.class);
                networkManager.destroyNetwork(controller.getNetwork());
            }
        }
        this.controllers.clear();
    }

    public ParameterDiscretizationModel findPDM(List<String> params) {
        for (var pdm : this.controllers.keySet()) {
            if (pdm.getParameterCount() != params.size()) {
                return null;
            }
            for(int i = 0; i<pdm.getParameters().size(); i++) {
                if(!pdm.getParameters().get(i).getName().equals(params.get(i))) {
                    return null;
                }
            }
            return pdm;
        }
        return null;
    }

}
