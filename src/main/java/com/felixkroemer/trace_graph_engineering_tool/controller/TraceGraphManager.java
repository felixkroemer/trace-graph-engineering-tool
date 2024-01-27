package com.felixkroemer.trace_graph_engineering_tool.controller;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import com.felixkroemer.trace_graph_engineering_tool.view.TraceGraphMainPanel;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyDisposable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.CyServiceRegistrar;

import java.util.*;
import java.util.stream.Collectors;

public class TraceGraphManager implements NetworkAboutToBeDestroyedListener, CyDisposable {

    private final TraceGraphMainPanel panel;
    private CyServiceRegistrar registrar;
    private Map<ParameterDiscretizationModel, Set<NetworkController>> controllers;
    private boolean destroying;

    public TraceGraphManager(CyServiceRegistrar registrar) {
        this.registrar = registrar;
        this.panel = new TraceGraphMainPanel(registrar);
        this.controllers = new HashMap<>();
        this.destroying = false;
    }

    public void registerTraceGraph(ParameterDiscretizationModel pdm, NetworkController controller) {
        this.controllers.computeIfAbsent(pdm, k -> new HashSet<>());
        this.controllers.get(pdm).add(controller);
        CySwingApplication swingApplication = registrar.getService(CySwingApplication.class);
        if (swingApplication.getCytoPanel(CytoPanelName.WEST).indexOfComponent("TraceGraphPanel") < 0) {
            this.registrar.registerService(this.panel, CytoPanelComponent.class);
        }
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
            controller.dispose();
            var pdm = controller.getPDM();
            this.controllers.get(pdm).remove(controller);
            if (controllers.get(pdm).isEmpty()) {
                controllers.remove(pdm);
            }
        }
        if (this.controllers.isEmpty()) {
            this.registrar.unregisterService(this.panel, CytoPanelComponent.class);
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
    @Override
    public void dispose() {
        this.destroying = true;
        var networkManager = registrar.getService(CyNetworkManager.class);
        for (var entry : this.controllers.entrySet()) {
            for (var controller : entry.getValue()) {
                // destroy controller before destroying network,
                // controller may need to access network
                controller.dispose();
                networkManager.destroyNetwork(controller.getNetwork());
            }
        }
        this.controllers.clear();
        this.registrar.unregisterService(this.panel, CytoPanelComponent.class);
        this.panel.dispose();
    }

    public List<ParameterDiscretizationModel> findPDM(Collection<String> params) {
        List<ParameterDiscretizationModel> matchingPDMs = new ArrayList<>();
        for (var pdm : this.controllers.keySet()) {
            if (pdm.getParameters().stream().map(Parameter::getName).collect(Collectors.toSet())
                   .equals(new HashSet<>(params))) {
                matchingPDMs.add(pdm);
            }
        }
        return matchingPDMs;
    }

    public Set<CyTable> getTraces(ParameterDiscretizationModel pdm) {
        Set<CyTable> tables = new HashSet<>();
        var controllers = this.controllers.get(pdm);
        for (var controller : controllers) {
            if (controller instanceof TraceGraphController tgc) {
                tables.addAll(tgc.getTraceGraph().getTraces());
            }
        }
        return tables;
    }

    public String getAvailableRootNetworkName(String preferredName) {
        var names = this.controllers.keySet().stream().map(ParameterDiscretizationModel::getName)
                                    .collect(Collectors.toSet());
        int i = 1;
        String name = preferredName;
        while (names.contains(name)) {
            name = preferredName + "(" + i + ")";
            i += 1;
        }
        return name;
    }
}
