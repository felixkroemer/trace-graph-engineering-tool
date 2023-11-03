package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;
import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDiscretizationModelDTO;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ParameterDiscretizationModel implements PropertyChangeListener {

    private final Logger logger;
    private PropertyChangeSupport pcs;
    private String name;
    private List<Parameter> parameters;
    private Map<Long, Long> suidHashMapping;
    private CyRootNetwork rootNetwork;
    private boolean binsFiltered;
    private Pair<String, Double> percentile;


    public ParameterDiscretizationModel(ParameterDiscretizationModelDTO dto) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.name = dto.getName();
        this.parameters = new ArrayList<>(dto.getParameterCount());
        for (ParameterDTO paramDto : dto.getParameters()) {
            Parameter parameter = new Parameter(paramDto, this);
            parameter.addObserver(this);
            this.parameters.add(parameter);
        }
        this.suidHashMapping = new HashMap<>();
        this.binsFiltered = false;
        this.percentile = null;
        this.pcs = new PropertyChangeSupport(this);

        this.setPercentile(Columns.NODE_VISITS, 50);
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
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "visibleBins" -> {
                boolean filtered = !this.parameters.stream().allMatch(p -> p.getVisibleBins().isEmpty());
                if (filtered != this.binsFiltered) {
                    this.binsFiltered = filtered;
                    pcs.firePropertyChange("filtered", null, filtered);
                }
            }
        }
    }

    public void setPercentile(String column, double percentile) {
        this.percentile = new Pair<>(column, percentile);
    }

    public Pair<String, Double> getPercentile() {
        return this.percentile;
    }
}

