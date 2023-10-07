package com.felixkroemer.trace_graph_engineering_tool.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UIState {
    private Trace trace;
    private Set<TraceExtension> traceSet;
    private Map<Parameter, ObservableSet> highlightedBins;
    private PropertyChangeSupport pcs;

    public UIState(TraceGraph traceGraph) {
        this.trace = null;
        this.traceSet = null;
        this.highlightedBins = new HashMap<>();
        for (Parameter param : traceGraph.getPDM().getParameters()) {
            this.highlightedBins.put(param, new ObservableSet());
        }
        this.pcs = new PropertyChangeSupport(this);
    }

    public void setTrace(Trace trace) {
        this.trace = trace;
        this.pcs.firePropertyChange("trace", null, this.trace);
    }

    public Trace getTrace() {
        return trace;
    }

    public void setTraceSet(Set<TraceExtension> traceSet) {
        this.traceSet = traceSet;
        this.pcs.firePropertyChange("traceSet", null, this.traceSet);
    }

    public Set<TraceExtension> getTraceSet() {
        return this.traceSet;
    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener("trace", l);
        pcs.addPropertyChangeListener("traceSet", l);
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    public void addHighlightObserver(Parameter parameter, PropertyChangeListener l) {
        this.highlightedBins.get(parameter).addObserver(l);
    }

    public Set<Integer> getHighlightedBins(Parameter parameter) {
        return this.highlightedBins.get(parameter).getSet();
    }

    public void highlightBins(Parameter parameter, Set<Integer> bins) {
        this.highlightedBins.get(parameter).replace(bins != null ? bins : new HashSet<>());
    }
}