package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.model.CyNetwork;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Set;

public class UIState {
    private Set<TraceExtension> traceSet;
    private CyNetwork traceSetNetwork;
    private PropertyChangeSupport pcs;

    public UIState(ParameterDiscretizationModel pdm) {
        this.traceSet = null;
        this.pcs = new PropertyChangeSupport(this);
    }

    public void setTraceSet(Set<TraceExtension> traceSet, CyNetwork network) {
        this.traceSet = traceSet;
        this.traceSetNetwork = network;
        this.pcs.firePropertyChange("traceSet", null, this.traceSet);
    }

    public Set<TraceExtension> getTraceSet() {
        return this.traceSet;
    }

    public CyNetwork getTraceSetNetwork() {
        return this.traceSetNetwork;
    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener("traceSet", l);
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}