package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

public class Parameter {

    public static final String ENABLED = "enabled";
    public static final String BINS = "bins";
    public static final String VISIBLE_BINS = "visible bins";
    private String name;
    private List<Double> bins;
    // set of bins that are visible, if empty all bins are displayed
    private Set<Integer> visibleBins;
    private boolean enabled;
    private PropertyChangeSupport pcs;
    private ParameterDiscretizationModel pdm;

    public Parameter(ParameterDTO dto, ParameterDiscretizationModel pdm) {
        this.name = dto.getName();
        this.bins = dto.getBins();
        this.visibleBins = new HashSet<>();
        this.pcs = new PropertyChangeSupport(this);
        this.enabled = true;
        this.pdm = pdm;
    }

    public Parameter(String name, ParameterDiscretizationModel pdm) {
        this.name = name;
        this.bins = new ArrayList<>();
        this.visibleBins = new HashSet<>();
        this.pcs = new PropertyChangeSupport(this);
        this.enabled = true;
        this.pdm = pdm;
    }

    public String getName() {
        return name;
    }

    public List<Double> getBins() {
        return new ArrayList<>(bins);
    }

    public void setBins(List<Double> bins) {
        Collections.sort(bins);
        this.bins = bins;
        pcs.firePropertyChange(Parameter.BINS, null, this.bins);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void enable() {
        this.enabled = true;
        pcs.firePropertyChange(Parameter.ENABLED, false, true);
    }

    public ParameterDiscretizationModel getPdm() {
        return this.pdm;
    }

    public void disable() {
        this.enabled = false;
        pcs.firePropertyChange(Parameter.ENABLED, true, false);
    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(Parameter.ENABLED, l);
        pcs.addPropertyChangeListener(Parameter.BINS, l);
        pcs.addPropertyChangeListener(Parameter.VISIBLE_BINS, l);
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(Parameter.ENABLED, l);
        pcs.removePropertyChangeListener(Parameter.BINS, l);
        pcs.removePropertyChangeListener(Parameter.VISIBLE_BINS, l);
    }

    public Set<Integer> getVisibleBins() {
        return this.visibleBins;
    }

    public void setVisibleBins(Set<Integer> bins) {
        this.visibleBins = bins;
        this.pcs.firePropertyChange(Parameter.VISIBLE_BINS, null, this.visibleBins);
    }
}
