package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.undo;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.DVisualLexicon;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DRenderingEngine;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DRenderingEngine.UpdateType;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DingNetworkViewFactory;
import com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.stateful.NodeDetails;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkViewSnapshot;
import org.cytoscape.view.model.View;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.undo.ViewChangeEdit.SavedObjs.*;

/**
 * Records the state of a view.  Used for undo by ViewChangeEdit. If it would help
 * to make this public, then please do so.
 * <p>
 * MKTODO Now that we have the snapshot feature this could be rewritten to be much simpler.
 * Don't need all the maps, just store a reference to the snapshot itself.
 */
public class ViewState {


    // MKTODO now that we have the snapshot feature
    protected double scaleFactor;
    protected Point2D center;
    protected Map<View<CyNode>, Point2D.Double> points;
    protected Map<View<CyEdge>, List> anchors;
    protected Map<View<CyEdge>, Integer> linetype;
    protected CyNetworkViewSnapshot view;
    protected DRenderingEngine re;
    protected ViewChangeEdit.SavedObjs savedObjs;

    /**
     * @param v The view whose state we're recording.
     */
    public ViewState(DRenderingEngine re, ViewChangeEdit.SavedObjs whatToSave) {
        view = re.getViewModelSnapshot();
        this.re = re;
        points = null;
        anchors = null;
        linetype = null;
        savedObjs = whatToSave;

        // record the state of the view
        center = re.getTransform().getCenter();
        scaleFactor = re.getZoom();
        NodeDetails nodeDetails = re.getNodeDetails();

        // Use nodes as keys because they are less volatile than
        // node views, which can disappear between when this edit
        // is created and when it is used.
        if (whatToSave == ALL || whatToSave == NODES) {
            points = new WeakHashMap<>();
            for (View<CyNode> n : view.getNodeViews()) {
                double x = nodeDetails.getXPosition(n);
                double y = nodeDetails.getYPosition(n);
                points.put(n, new Point2D.Double(x, y));
            }
        }

        if (whatToSave == ALL || whatToSave == EDGES) {
            anchors = new WeakHashMap<>();
            linetype = new WeakHashMap<>();
            for (View<CyEdge> e : view.getEdgeViews()) {
                // FIXME!
                //anchors.put(e, ev.getBend().getHandles());
                linetype.put(e, re.getEdgeDetails().getLineCurved(e));
            }
        }

        if (whatToSave == SELECTED || whatToSave == SELECTED_NODES) {
            points = new WeakHashMap<>();
            for (View<CyNode> n : view.getTrackedNodes(DingNetworkViewFactory.SELECTED_NODES)) {
                double x = nodeDetails.getXPosition(n);
                double y = nodeDetails.getYPosition(n);
                points.put(n, new Point2D.Double(x, y));
            }
        }


        // MKTODO add selected edges to the view model????

        //		if (whatToSave == SELECTED || whatToSave == SELECTED_EDGES ) {
        //			anchors = new WeakHashMap<>();
        //			linetype = new WeakHashMap<>();
        //
        //			Iterator<CyEdge> edgeIter = view.getSelectedEdges().iterator();
        //			while (edgeIter.hasNext()) {
        //				CyEdge e = edgeIter.next();
        //				final DEdgeView ev = view.getDEdgeView(e);
        //				if (ev == null) continue;
        //				DGraphView gView = (DGraphView) ev.getGraphView();
        //				// FIXME!
        //				//anchors.put(e, ev.getBend().getHandles());
        //				linetype.put(e, gView.m_edgeDetails.getLineCurved(e));
        //			}
        //		}
    }

    /**
     * Checks if the ViewState is the same. If scale and center are
     * equal it then begins comparing node positions.
     *
     * @param o The object to test for equality.
     */
    public boolean equals(Object o) {
        if (!(o instanceof ViewState)) {
            return false;
        }

        ViewState vs = (ViewState) o;

        if (view != vs.view) {
            return false;
        }

        if (!center.equals(vs.center)) {
            return false;
        }

        if (Double.compare(scaleFactor, vs.scaleFactor) != 0) {
            return false;
        }

        if (savedObjs != vs.savedObjs) {
            return false;
        }

        // Use nodes as keys because they are less volatile than views...
        if (points != null) {
            if (vs.points == null || points.size() != vs.points.size()) {
                return false;
            }
            for (View<CyNode> n : points.keySet()) {
                if (!points.get(n).equals(vs.points.get(n))) {
                    return false;
                }
            }
        }

        if (anchors != null) {
            if (vs.anchors == null || anchors.size() != vs.anchors.size()) return false;

            for (View<CyEdge> e : anchors.keySet()) {
                if (!anchors.get(e).equals(vs.anchors.get(e))) {
                    return false;
                }

                if (!linetype.get(e).equals(vs.linetype.get(e))) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Applies the recorded state to the view used to create
     * this object.
     */
    public void apply() {
        if (points != null) {
            // Use nodes as keys because they are less volatile than views...
            for (View<CyNode> n : points.keySet()) {
                Point2D.Double p = points.get(n);

                View<CyNode> mutableNode = view.getMutableNodeView(n.getSUID());
                if (mutableNode != null) {
                    mutableNode.setVisualProperty(DVisualLexicon.NODE_X_LOCATION, p.getX());
                    mutableNode.setVisualProperty(DVisualLexicon.NODE_Y_LOCATION, p.getY());
                }
            }
        }

        re.setZoom(scaleFactor);
        re.setCenter(center.getX(), center.getY());
        re.updateView(UpdateType.ALL_FULL);

        if (anchors != null) {
            for (View<CyEdge> e : anchors.keySet()) {
                // FIXME!
                //ev.getBend().setHandles( anchors.get(e) );
                View<CyEdge> mutableEdge = view.getMutableEdgeView(e.getSUID());
                if (mutableEdge != null) {
                    mutableEdge.setVisualProperty(DVisualLexicon.EDGE_CURVED, linetype.get(e).intValue());
                }
            }
        }
    }

}
