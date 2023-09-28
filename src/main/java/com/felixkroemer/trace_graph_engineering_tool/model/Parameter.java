package com.felixkroemer.trace_graph_engineering_tool.model;

import com.felixkroemer.trace_graph_engineering_tool.model.dto.ParameterDTO;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

public class Parameter {
    private String name;
    private String type;
    private List<Double> bins;
    private boolean enabled;
    private PropertyChangeSupport pcs;
    private double minimum;
    private double maximum;
    private HighlightRange highlightRange;


    public Parameter(ParameterDTO dto, Double[] minMax) {
        this.name = dto.getName();
        this.type = dto.getType();
        this.bins = dto.getBins();
        this.pcs = new PropertyChangeSupport(this);
        this.enabled = true;
        this.maximum = minMax[0];
        this.minimum = minMax[1];
        this.highlightRange = null;
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
        pcs.addPropertyChangeListener("highlightRange", l);
    }

    public void clearObservers() {
        this.pcs = new PropertyChangeSupport(this);
    }

    public void setHighlightRange(HighlightRange range) {
        this.highlightRange = range;
        pcs.firePropertyChange("highlightRange", null, this.highlightRange);
    }

    public HighlightRange getHighlightRange() {
        return this.highlightRange;
    }

    public double getMinimum() {
        return this.minimum;
    }

    public double getMaximum() {
        return this.maximum;
    }
}
