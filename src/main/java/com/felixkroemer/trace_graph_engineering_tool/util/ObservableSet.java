package com.felixkroemer.trace_graph_engineering_tool.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashSet;
import java.util.Set;

public class ObservableSet {
    private Set<Integer> set;
    private PropertyChangeSupport pcs;

    public ObservableSet() {
        this.set = new HashSet<>();
        this.pcs = new PropertyChangeSupport(this);
    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener("highlightedBins", l);
    }

    public void replace(Set<Integer> newBins) {
        var oldBins = this.set;
        this.set = newBins;
        this.pcs.firePropertyChange("highlightedBins", oldBins, newBins);
    }

    public int size() {
        return this.set.size();
    }

    public Set<Integer> getSet() {
        return this.set;
    }

}