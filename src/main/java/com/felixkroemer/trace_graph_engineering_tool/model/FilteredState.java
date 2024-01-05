package com.felixkroemer.trace_graph_engineering_tool.model;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

// needs to be updated whenever the network changes, in order to keep the clear filter button updated
public class FilteredState {

    private int hiddenNodeCount;
    private CyNetworkView networkView;
    private PropertyChangeSupport pcs;

    public FilteredState(CyNetworkView networkView) {
        this.networkView = networkView;
        this.pcs = new PropertyChangeSupport(this);
    }

    // from org/cytoscape/internal/view/util/ViewUtil.java
    public static int getHiddenNodeCount(CyNetworkView view) {
        int count = 0;

        if (view != null) {
            for (var nv : view.getNodeViews()) {
                if (nv.getVisualProperty(BasicVisualLexicon.NODE_VISIBLE) == Boolean.FALSE)
                    count++;
            }
        }

        return count;
    }

    public int getHiddenNodeCount() {
        return this.hiddenNodeCount;
    }

    public int getTotalNodeCount() {
        return networkView.getModel().getNodeCount();
    }

    public void addObserver(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener("filteredState", listener);
    }

    public void removeObserver(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener("filteredState", listener);
    }

    public void update() {
        this.hiddenNodeCount = getHiddenNodeCount(networkView);
        this.pcs.firePropertyChange("filteredState", null, this);
    }
}
