package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentComparisonControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentComparisonControllerListener;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentTraceGraphControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentTraceGraphControllerListener;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel.PDMPanel;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class TraceGraphPanel extends JPanel implements CytoPanelComponent2, SelectedNodesAndEdgesListener,
        SetCurrentNetworkListener, SetCurrentTraceGraphControllerListener, SetCurrentComparisonControllerListener {

    private CyServiceRegistrar reg;
    private JTabbedPane tabs;

    private PDMPanel pdmPanel;
    private NodeInfoPanel nodeInfoPanel;
    private TraceGraphComparisonPanel traceGraphComparisonPanel;
    private NodeComparisonPanel nodeComparisonPanel;

    private static String PDM_TITLE = "PDM";
    private static String NODE_INFO_TITLE = "Node Info";
    private static String TRACE_GRAPH_COMPARISON_TITLE = "Comparison";
    private static String NODE_COMPARISON_TITLE = "Node Comparison";

    public TraceGraphPanel(CyServiceRegistrar reg) {
        super(new BorderLayout());
        this.reg = reg;
        this.tabs = new JTabbedPane(JTabbedPane.BOTTOM);

        this.pdmPanel = new PDMPanel(reg);
        this.nodeInfoPanel = new NodeInfoPanel(reg);
        this.traceGraphComparisonPanel = new TraceGraphComparisonPanel();
        this.nodeComparisonPanel = new NodeComparisonPanel(reg);

        this.reg.registerService(this, SelectedNodesAndEdgesListener.class, new Properties());
        this.reg.registerService(this, SetCurrentNetworkListener.class, new Properties());
        this.reg.registerService(this, SetCurrentTraceGraphControllerListener.class, new Properties());
        this.reg.registerService(this, SetCurrentComparisonControllerListener.class, new Properties());

        init();
    }

    @Override
    public Component getComponent() {
        return this;
    }

    public void init() {
        this.tabs.addTab(PDM_TITLE, this.pdmPanel);
        this.add(this.tabs, BorderLayout.CENTER);
    }

    public void destroy() {
        this.reg.unregisterService(this, SelectedNodesAndEdgesListener.class);
        this.reg.unregisterService(this, SetCurrentNetworkListener.class);
        this.reg.unregisterService(this, SetCurrentTraceGraphControllerListener.class);
        this.reg.unregisterService(this, SetCurrentComparisonControllerListener.class);
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.WEST;
    }

    @Override
    public String getTitle() {
        return "Trace Graph";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return "com.felixkroemer.TraceGraphPanel";
    }

    private int getPanelIndex(String title) {
        for (int i = 0; i < this.tabs.getTabCount(); i++) {
            if (this.tabs.getTitleAt(i).equals(title)) {
                return i;
            }
        }
        return -1;
    }

    private void hidePanel(String title) {
        var index = getPanelIndex(title);
        if (index != -1) {
            this.tabs.removeTabAt(index);
        }
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        if (!Util.isTraceGraphNetwork(event.getNetwork())) {
            this.hidePanel(NODE_INFO_TITLE);
        }
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(event.getNetwork());
        if (event.getSelectedNodes().size() == 1) {
            this.nodeInfoPanel.setNode(controller, event.getSelectedNodes().iterator().next());
            this.tabs.addTab(NODE_INFO_TITLE, this.nodeInfoPanel);
            this.tabs.setSelectedIndex(getPanelIndex(NODE_INFO_TITLE));
            this.showPanel();
        } else if (event.getSelectedNodes().size() > 1 && event.getSelectedNodes().size() < 6) {
            this.nodeComparisonPanel.setNodes(controller, event.getSelectedNodes());
            this.tabs.addTab(NODE_COMPARISON_TITLE, this.nodeComparisonPanel);
            this.tabs.setSelectedIndex(getPanelIndex(NODE_COMPARISON_TITLE));
            this.showPanel();
        } else {
            this.hidePanel(NODE_INFO_TITLE);
            this.hidePanel(NODE_COMPARISON_TITLE);
        }
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        var network = e.getNetwork();
        if (network == null) {
            this.pdmPanel.clear();
            return;
        }
        var manager = reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(network);
        if (controller != null) {
            this.pdmPanel.registerCallbacks(controller);
            this.showPanel();
        }
    }

    @Override
    public void handleEvent(SetCurrentTraceGraphControllerEvent event) {
        this.tabs.setSelectedIndex(getPanelIndex(PDM_TITLE));
        this.hidePanel(TRACE_GRAPH_COMPARISON_TITLE);
    }

    @Override
    public void handleEvent(SetCurrentComparisonControllerEvent event) {
        this.traceGraphComparisonPanel.setComparisonController(event.getNetworkComparisonController());
        this.tabs.addTab(TRACE_GRAPH_COMPARISON_TITLE, this.traceGraphComparisonPanel);
        this.tabs.setSelectedIndex(getPanelIndex(TRACE_GRAPH_COMPARISON_TITLE));
    }

    public void showPanel() {
        CySwingApplication swingApplication = reg.getService(CySwingApplication.class);
        CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.WEST);
        if (cytoPanel.getState() == CytoPanelState.HIDE) {
            cytoPanel.setState(CytoPanelState.DOCK);
        }
        cytoPanel.setSelectedIndex(cytoPanel.indexOfComponent("com.felixkroemer.TraceGraphPanel"));
    }
}
