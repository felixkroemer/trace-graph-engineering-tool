package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphPanel;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.table.CyTableViewManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.COLUMN_VISIBLE;

public class TraceGraphManager implements NetworkAboutToBeDestroyedListener, PropertyChangeListener {

    private CyServiceRegistrar registrar;
    private Map<ParameterDiscretizationModel, Set<NetworkController>> controllers;
    private final TraceGraphPanel panel;
    private final TraceDetailsController traceDetailsController;
    private boolean destroying;

    public TraceGraphManager(CyServiceRegistrar registrar) {
        this.registrar = registrar;
        this.panel = new TraceGraphPanel(registrar, this);
        this.controllers = new HashMap<>();
        this.traceDetailsController = new TraceDetailsController(registrar);
        this.destroying = false;
    }

    public void registerTraceGraph(ParameterDiscretizationModel pdm, NetworkController controller) {
        if (this.controllers.isEmpty()) {
            this.traceDetailsController.createAndRegisterNetwork();
        }
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
                for (NetworkController controller : this.controllers.get(pdm)) {
                    var nodeTableView = tableViewManager.getTableView(controller.getNetwork().getDefaultNodeTable());
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
        for (NetworkController controller : controllers.get(pdm)) {
            controller.updateNetwork(changedParameter);
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
        if (this.destroying) {
            return;
        }
        NetworkController controller = findControllerForNetwork(e.getNetwork());
        if (controller != null) {
            destroyNetwork(controller);
        }
    }

    public void destroyNetwork(NetworkController controller) {
        if (controller != null) {
            controller.destroy();
            var pdm = controller.getPDM();
            this.controllers.get(pdm).remove(controller);
            if (controllers.get(pdm).isEmpty()) {
                controllers.remove(pdm);
            }
        }
        if (this.controllers.isEmpty()) {
            this.hidePanel();
        }
    }

    public NetworkController findControllerForNetwork(CyNetwork network) {
        for (var entry : this.controllers.entrySet()) {
            for (var controller : entry.getValue()) {
                if (controller.getNetwork() == network) {
                    return controller;
                }
            }
        }
        return null;
    }

    public ParameterDiscretizationModel findPDMForNetwork(CyNetwork network) {
        for (var entry : this.controllers.entrySet()) {
            for (var controller : entry.getValue()) {
                if (controller.getNetwork() == network) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    // call to destroy networks, everything else is handled in
    // handleEvent(NetworkAboutToBeDestroyedEvent e)
    public void destroy() {
        this.destroying = true;
        var networkManager = registrar.getService(CyNetworkManager.class);
        for (var entry : this.controllers.entrySet()) {
            for (var controller : entry.getValue()) {
                networkManager.destroyNetwork(controller.getNetwork());
            }
        }
        this.controllers.clear();
        traceDetailsController.destroy();
        CyNetwork traceDetailsNetwork;
        if ((traceDetailsNetwork = traceDetailsController.getNetwork()) != null) {
            networkManager.destroyNetwork(traceDetailsNetwork);
        }
        this.registrar.unregisterService(this.panel, CytoPanelComponent.class);
        this.panel.destroy();
    }

    public ParameterDiscretizationModel findPDM(List<String> params) {
        for (var pdm : this.controllers.keySet()) {
            if (pdm.getParameterCount() != params.size()) {
                return null;
            }
            for (int i = 0; i < pdm.getParameters().size(); i++) {
                if (!pdm.getParameters().get(i).getName().equals(params.get(i))) {
                    return null;
                }
            }
            return pdm;
        }
        return null;
    }

    public void showTraceDetailsNetwork() {
        traceDetailsController.update();
        var manager = this.registrar.getService(CyApplicationManager.class);
        manager.setCurrentNetwork(this.traceDetailsController.getNetwork());
    }

    public void focusNode(CyNode node) {
        var manager = this.registrar.getService(CyApplicationManager.class);
        var network = this.traceDetailsController.getCorrespondingNetwork();
        var controller = findControllerForNetwork(network);
        manager.setCurrentNetwork(network);
        if (node != null) {
            var defaultNetworkNode = this.traceDetailsController.findCorrespondingNode(node);
            controller.focusNode(defaultNetworkNode);
        }
    }

    public Set<CyTable> getSourceTables(ParameterDiscretizationModel pdm) {
        Set<CyTable> tables = new HashSet<>();
        var controllers = this.controllers.get(pdm);
        for (var controller : controllers) {
            if (controller instanceof TraceGraphController tgc) {
                tables.addAll(tgc.getTraceGraph().getSourceTables());
            }
        }
        return tables;
    }

}
