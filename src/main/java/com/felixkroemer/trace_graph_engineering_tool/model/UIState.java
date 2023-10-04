package com.felixkroemer.trace_graph_engineering_tool.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class UIState {
    private Trace trace;
    private PropertyChangeSupport pcs;

    public UIState() {
        this.trace = null;
        this.pcs = new PropertyChangeSupport(this);
    }

    public void setTrace(Trace trace) {
        this.trace = trace;
        this.pcs.firePropertyChange("trace", null, this.trace);
    }

    public Trace getTrace() {
        return trace;
    }

    public void addObserver(PropertyChangeListener l) {
        pcs.addPropertyChangeListener("trace", l);
    }

    public void removeObserver(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
