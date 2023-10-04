package com.felixkroemer.trace_graph_engineering_tool.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Set;

public class UIState {
    private Trace trace;
    private Set<TraceExtension> traceSet;
    private PropertyChangeSupport pcs;

    public UIState() {
        this.trace = null;
        this.traceSet = null;
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
}
