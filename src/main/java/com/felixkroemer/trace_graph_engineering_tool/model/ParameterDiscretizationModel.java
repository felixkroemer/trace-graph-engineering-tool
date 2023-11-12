package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.javatuples.Pair;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.function.Consumer;

public class ParameterDiscretizationModel implements PropertyChangeListener {

    private PropertyChangeSupport pcs;
    private String name;
    private List<Parameter> parameters;
    private Map<Long, Long> suidHashMapping;
    private CyRootNetwork rootNetwork;
    private boolean filtered;
    private Pair<String, Double> percentile;


    public ParameterDiscretizationModel(ParameterDiscretizationModelDTO dto) {
        this.name = dto.getName();
        this.parameters = new ArrayList<>(dto.getParameterCount());
        for (ParameterDTO paramDto : dto.getParameters()) {
            Parameter parameter = new Parameter(paramDto, this);
            parameter.addObserver(this);
            this.parameters.add(parameter);
        }
        this.suidHashMapping = new HashMap<>();
        this.filtered = false;
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
        pcs.addPropertyChangeListener("filtered", l);
        pcs.addPropertyChangeListener("percentileFilter", l);
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener("filtered", l);
        pcs.removePropertyChangeListener("percentileFilter", l);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "visibleBins" -> {
                this.updateFilteredState();
            }
        }
    }

    public void setPercentile(String column, double percentile) {
        this.percentile = new Pair<>(column, percentile);
        this.pcs.firePropertyChange("percentileFilter", null, this.percentile);
        this.updateFilteredState();
    }

    public void resetPercentile() {
        this.percentile = null;
        this.pcs.firePropertyChange("percentileFilter", null, null);
        this.updateFilteredState();
    }

    public Pair<String, Double> getPercentile() {
        return this.percentile;
    }

    private void updateFilteredState() {
        boolean parameterFiltered = !this.parameters.stream().allMatch(p -> p.getVisibleBins().isEmpty());
        boolean percentileFiltered = this.percentile != null;
        boolean filtered = parameterFiltered || percentileFiltered;
        if (filtered != this.filtered) {
            this.filtered = filtered;
            pcs.firePropertyChange("filtered", null, this.filtered);
        }
    }

    public boolean isFiltered() {
        return this.filtered;
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

