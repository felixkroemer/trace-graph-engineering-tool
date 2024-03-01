package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.events.UpdatedPDMEvent;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.javatuples.Pair;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.function.Consumer;

public class ParameterDiscretizationModel {

    public static final String PERCENTILE_FILTER = "percentileFilter";
    private CyServiceRegistrar registrar;
    private PropertyChangeSupport pcs;
    private List<Parameter> parameters;
    private Map<Long, Long> hashSuidMapping;
    private CyRootNetwork rootNetwork;
    private Pair<String, Double> percentile;
    private boolean updating;

    public ParameterDiscretizationModel(CyServiceRegistrar registrar, ParameterDiscretizationModelDTO dto) {
        this.parameters = new ArrayList<>(dto.getParameterCount());
        for (ParameterDTO paramDto : dto.getParameters()) {
            Parameter parameter = new Parameter(paramDto, this);
            this.parameters.add(parameter);
        }
        this.hashSuidMapping = new HashMap<>();
        this.percentile = null;
        this.pcs = new PropertyChangeSupport(this);
        this.registrar = registrar;
    }

    public ParameterDiscretizationModel(CyServiceRegistrar registrar, List<String> parameterNames) {
        this.parameters = new ArrayList<>(parameterNames.size());
        for (String parameterName : parameterNames) {
            Parameter parameter = new Parameter(parameterName, this);
            this.parameters.add(parameter);
        }
        this.hashSuidMapping = new HashMap<>();
        this.percentile = null;
        this.pcs = new PropertyChangeSupport(this);
        this.registrar = registrar;
    }

    public List<Parameter> getParameters() {
        return this.parameters;
    }

    public Parameter getParameter(String name) {
        for (var param : parameters) {
            if (param.getName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    public void setParameterBins(List<ParameterDTO> parameters) {
        this.updating = true;
        for (var paramDTO : parameters) {
            var parameter = this.getParameter(paramDTO.getName());
            if (parameter != null) {
                parameter.setBins(paramDTO.getBins());
                parameter.setVisibleBins(new HashSet<>());
            }
        }
        this.updating = false;
        var eventHelper = this.registrar.getService(CyEventHelper.class);
        eventHelper.fireEvent(new UpdatedPDMEvent(this, this));
    }

    public boolean isUpdating() {
        return this.updating;
    }

    public int getParameterCount() {
        return this.parameters.size();
    }

    public void forEach(Consumer<Parameter> consumer) {
        for (Parameter param : this.parameters) {
            consumer.accept(param);
        }
    }

    public String getName() {
        if (this.rootNetwork != null) {
            return rootNetwork.getDefaultNetworkTable().getRow(rootNetwork.getSUID()).get(CyNetwork.NAME, String.class);
        } else {
            return null;
        }
    }

    public Map<Long, Long> getHashSuidMapping() {
        return this.hashSuidMapping;
    }

    public CyRootNetwork getRootNetwork() {
        return this.rootNetwork;
    }

    // can't be set in constructor because rootnetwork can't be created via api without subnetwork
    public void setRootNetwork(CyRootNetwork rootNetwork) {
        this.rootNetwork = rootNetwork;
    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(ParameterDiscretizationModel.PERCENTILE_FILTER, l);
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(ParameterDiscretizationModel.PERCENTILE_FILTER, l);
    }

    public void setPercentile(String column, double percentile) {
        this.percentile = new Pair<>(column, percentile);
        this.pcs.firePropertyChange(ParameterDiscretizationModel.PERCENTILE_FILTER, null, this.percentile);
    }

    public void resetPercentile() {
        this.percentile = null;
        this.pcs.firePropertyChange(ParameterDiscretizationModel.PERCENTILE_FILTER, null, null);
    }

    public Pair<String, Double> getPercentile() {
        return this.percentile;
    }

    public void resetFilters() {
        forEach(p -> {
            if (!p.getVisibleBins().isEmpty()) {
                p.setVisibleBins(Collections.emptySet());
            }
        });
        if (this.percentile != null) {
            this.resetPercentile();
        }
    }
}

