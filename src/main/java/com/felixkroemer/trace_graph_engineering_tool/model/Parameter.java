package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

public class Parameter {
    private String name;
    private List<Double> bins;
    private boolean enabled;
    private PropertyChangeSupport pcs;
    private ParameterDiscretizationModel pdm;


    public Parameter(ParameterDTO dto, ParameterDiscretizationModel pdm) {
        this.name = dto.getName();
        this.bins = dto.getBins();
        this.pcs = new PropertyChangeSupport(this);
        this.enabled = true;
        this.pdm = pdm;
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
        pcs.firePropertyChange("enabled", false, true);
    }

    public ParameterDiscretizationModel getPdm() {
        return this.pdm;
    }

    public void disable() {
        this.enabled = false;
        pcs.firePropertyChange("enabled", true, false);
    }

    public void setBins(List<Double> bins) {
        this.bins = bins;
        pcs.firePropertyChange("bins", null, this.bins);
    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener("enabled", l);
        pcs.addPropertyChangeListener("bins", l);
    }

}
