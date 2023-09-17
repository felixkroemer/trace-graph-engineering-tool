package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.List;

public class Parameter {
    private String name;
    private String type;
    private List<Double> bins;
    private boolean enabled;
    private PropertyChangeSupport pcs;

    public Parameter(ParameterDTO dto) {
        this.name = dto.getName();
        this.type = dto.getType();
        this.bins = dto.getBins();
        this.pcs = new PropertyChangeSupport(this);
        this.enabled = true;
    }

    public String getName() {
        return name;
    }

    public List<Double> getBins() {
        return bins;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void enable() {
        this.enabled = true;
        pcs.firePropertyChange("parameter", false, true);
    }

    public void disable() {
        this.enabled = false;
        pcs.firePropertyChange("parameter", true, false);
        this.setBins(Arrays.asList(new Double[]{Double.MAX_VALUE}));
    }

    public void setBins(List<Double> bins) {

    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener("enabled", l);
        pcs.addPropertyChangeListener("bins", l);
    }

    public void clearObservers() {
        this.pcs = new PropertyChangeSupport(this);
    }
}
