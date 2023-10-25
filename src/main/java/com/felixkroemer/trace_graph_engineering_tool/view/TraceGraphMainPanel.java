package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.events.*;
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

public class TraceGraphMainPanel extends JPanel implements CytoPanelComponent2, SelectedNodesAndEdgesListener,
        SetCurrentNetworkListener, SetCurrentTraceGraphControllerListener, SetCurrentComparisonControllerListener,
        ShowTraceEventListener, ClearTraceEventListener {

    private CyServiceRegistrar reg;
    private JTabbedPane tabs;

    private PDMPanel pdmPanel;
    private NodeInfoPanel nodeInfoPanel;
    private TraceGraphComparisonPanel traceGraphComparisonPanel;
    private NodeComparisonPanel nodeComparisonPanel;
    private TracePanel tracePanel;

    public TraceGraphMainPanel(CyServiceRegistrar reg) {
        super(new BorderLayout());
        this.reg = reg;
        this.tabs = new JTabbedPane(JTabbedPane.BOTTOM);

        this.pdmPanel = new PDMPanel(reg);
        this.nodeInfoPanel = new NodeInfoPanel(reg);
        this.traceGraphComparisonPanel = new TraceGraphComparisonPanel();
        this.nodeComparisonPanel = new NodeComparisonPanel(reg);
        this.tracePanel = new TracePanel(reg);

        this.reg.registerService(this, SelectedNodesAndEdgesListener.class, new Properties());
        this.reg.registerService(this, SetCurrentNetworkListener.class, new Properties());
        this.reg.registerService(this, SetCurrentTraceGraphControllerListener.class, new Properties());
        this.reg.registerService(this, SetCurrentComparisonControllerListener.class, new Properties());
        this.reg.registerService(this, ShowTraceEventListener.class, new Properties());
        this.reg.registerService(this, ClearTraceEventListener.class, new Properties());

        init();
    }

    @Override
    public Component getComponent() {
        return this;
    }

    public void init() {
        this.tabs.addTab(this.pdmPanel.getTitle(), this.pdmPanel);
        this.add(this.tabs, BorderLayout.CENTER);
    }

    public void destroy() {
        this.reg.unregisterService(this, SelectedNodesAndEdgesListener.class);
        this.reg.unregisterService(this, SetCurrentNetworkListener.class);
        this.reg.unregisterService(this, SetCurrentTraceGraphControllerListener.class);
        this.reg.unregisterService(this, SetCurrentComparisonControllerListener.class);
        this.reg.unregisterService(this, ShowTraceEventListener.class);
        this.reg.unregisterService(this, ClearTraceEventListener.class);
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

    private void hidePanel(TraceGraphPanel panel) {
        var index = getPanelIndex(panel.getTitle());
        if (index != -1) {
            this.tabs.removeTabAt(index);
        }
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(event.getNetwork());
        if (controller == null) {
            this.hideControllerSpecificPanels();
            return;
        }
        if (event.getSelectedNodes().size() == 1) {
            this.nodeInfoPanel.setNode(controller, event.getSelectedNodes().iterator().next());
            this.showPanel(this.nodeInfoPanel);
            this.showMainPanel();
        } else if (event.getSelectedNodes().size() > 1 && event.getSelectedNodes().size() < 6) {
            this.nodeComparisonPanel.setNodes(controller, event.getSelectedNodes(), event.getNetwork());
            this.showPanel(this.nodeComparisonPanel);
            this.showMainPanel();
        } else {
            this.hideTemporaryPanels();
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
            this.showMainPanel();
        }
    }

    @Override
    public void handleEvent(SetCurrentTraceGraphControllerEvent event) {
        this.showPanel(this.pdmPanel);
        var trace = event.getTraceGraphController().getTraceGraph().getTrace();
        if (trace == null) {
            this.hidePanel(tracePanel);
        } else {
            this.showPanel(tracePanel);
        }
        this.hidePanel(this.traceGraphComparisonPanel);
    }

    @Override
    public void handleEvent(SetCurrentComparisonControllerEvent event) {
        this.traceGraphComparisonPanel.setComparisonController(event.getNetworkComparisonController());
        this.showPanel(this.traceGraphComparisonPanel);
        this.hidePanel(this.tracePanel);
    }

    public void showMainPanel() {
        CySwingApplication swingApplication = reg.getService(CySwingApplication.class);
        CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.WEST);
        if (cytoPanel.getState() == CytoPanelState.HIDE) {
            cytoPanel.setState(CytoPanelState.DOCK);
        }
        cytoPanel.setSelectedIndex(cytoPanel.indexOfComponent("com.felixkroemer.TraceGraphPanel"));
    }

    private void showPanel(TraceGraphPanel panel) {
        this.hideTemporaryPanels();
        if (panel != pdmPanel && getPanelIndex(panel.getTitle()) == -1) {
            this.tabs.addTab(panel.getTitle(), panel);
        }
        this.tabs.setSelectedIndex(getPanelIndex(panel.getTitle()));
    }

    private void hideTemporaryPanels() {
        this.hidePanel(this.nodeInfoPanel);
        this.hidePanel(this.nodeComparisonPanel);
    }

    private void hideControllerSpecificPanels() {
        this.hideTemporaryPanels();
        this.hidePanel(this.traceGraphComparisonPanel);
        this.hidePanel(this.tracePanel);
    }

    @Override
    public void handleEvent(ShowTraceEvent e) {
        this.showPanel(this.tracePanel);
    }

    @Override
    public void handleEvent(ClearTraceEvent e) {
        this.hidePanel(this.tracePanel);
    }
}
