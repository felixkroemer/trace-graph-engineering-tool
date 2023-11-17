package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.javatuples.Pair;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.function.Consumer;

public class ParameterDiscretizationModel {

    public static final String PERCENTILE_FILTER = "percentileFilter";

    private PropertyChangeSupport pcs;
    private String name;
    private List<Parameter> parameters;
    private Map<Long, Long> suidHashMapping;
    private CyRootNetwork rootNetwork;
    private Pair<String, Double> percentile;

    public ParameterDiscretizationModel(ParameterDiscretizationModelDTO dto) {
        this.name = dto.getName();
        this.parameters = new ArrayList<>(dto.getParameterCount());
        for (ParameterDTO paramDto : dto.getParameters()) {
            Parameter parameter = new Parameter(paramDto, this);
            this.parameters.add(parameter);
        }
        this.suidHashMapping = new HashMap<>();
        this.percentile = null;
        this.pcs = new PropertyChangeSupport(this);
    }

    public ParameterDiscretizationModel(String name, List<String> parameterNames) {
        this.name = name;
        this.parameters = new ArrayList<>(parameterNames.size());
        for (String parameterName : parameterNames) {
            Parameter parameter = new Parameter(parameterName, this);
            this.parameters.add(parameter);
        }
        this.suidHashMapping = new HashMap<>();
        this.percentile = null;
        this.pcs = new PropertyChangeSupport(this);
    }

    public List<Parameter> getParameters() {
        return this.parameters;
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
        return this.name;
    }

    public Parameter getParameter(String name) {
        for (Parameter p : this.parameters) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public Map<Long, Long> getSuidHashMapping() {
        return this.suidHashMapping;
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

