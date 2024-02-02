package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.events.*;
import com.felixkroemer.trace_graph_engineering_tool.view.display_controller_panels.EdgeDisplayControllerPanel;
import com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel.PDMPanel;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.*;
import org.cytoscape.model.CyDisposable;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class TraceGraphMainPanel extends JPanel implements CytoPanelComponent2, SelectedNodesAndEdgesListener,
        SetCurrentNetworkListener, SetCurrentTraceGraphControllerListener, SetCurrentComparisonControllerListener,
        SetCurrentEdgeDisplayControllerEventListener, CyDisposable {

    private CyServiceRegistrar reg;
    private JTabbedPane tabs;
    private PDMPanel pdmPanel;
    private NodeInfoPanel nodeInfoPanel;
    private TraceGraphComparisonPanel traceGraphComparisonPanel;
    private NodeComparisonPanel nodeComparisonPanel;
    private EdgeDisplayControllerPanel edgeDisplayControllerPanel;

    public TraceGraphMainPanel(CyServiceRegistrar reg) {
        super(new BorderLayout());
        this.reg = reg;
        this.tabs = new JTabbedPane(JTabbedPane.BOTTOM);

        this.pdmPanel = new PDMPanel(reg);
        this.nodeInfoPanel = new NodeInfoPanel();
        this.traceGraphComparisonPanel = new TraceGraphComparisonPanel();
        this.nodeComparisonPanel = new NodeComparisonPanel(reg);
        this.edgeDisplayControllerPanel = null;

        this.reg.registerService(this, SelectedNodesAndEdgesListener.class, new Properties());
        this.reg.registerService(this, SetCurrentNetworkListener.class, new Properties());
        this.reg.registerService(this, SetCurrentTraceGraphControllerListener.class, new Properties());
        this.reg.registerService(this, SetCurrentComparisonControllerListener.class, new Properties());
        this.reg.registerService(this, SetCurrentEdgeDisplayControllerEventListener.class, new Properties());

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

    @Override
    public void dispose() {
        this.reg.unregisterService(this, SelectedNodesAndEdgesListener.class);
        this.reg.unregisterService(this, SetCurrentNetworkListener.class);
        this.reg.unregisterService(this, SetCurrentTraceGraphControllerListener.class);
        this.reg.unregisterService(this, SetCurrentComparisonControllerListener.class);
        this.reg.unregisterService(this, SetCurrentEdgeDisplayControllerEventListener.class);
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
        int index;
        if (panel != null && (index = getPanelIndex(panel.getTitle())) != -1) {
            this.tabs.removeTabAt(index);
        }
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        if (!event.nodesChanged()) {
            return;
        }
        var manager = this.reg.getService(TraceGraphManager.class);
        var controller = manager.findControllerForNetwork(event.getNetwork());
        if (event.getSelectedNodes().size() == 1) {
            this.nodeInfoPanel.setNode(controller, event.getSelectedNodes().iterator().next());
            this.showPanel(this.nodeInfoPanel);
            this.hidePanel(this.nodeComparisonPanel);
            this.showMainPanel();
        } else if (event.getSelectedNodes().size() > 1 && event.getSelectedNodes().size() < 6) {
            this.nodeComparisonPanel.setNodes(controller, event.getSelectedNodes(), event.getNetwork());
            this.showPanel(this.nodeComparisonPanel);
            this.hidePanel(this.nodeInfoPanel);
            this.showMainPanel();
        } else {
            this.hidePanel(this.nodeInfoPanel);
            this.hidePanel(this.nodeComparisonPanel);
        }
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        var network = e.getNetwork();
        if (network == null) {
            this.pdmPanel.clear();
        }
        this.hidePanel(this.nodeInfoPanel);
        this.hidePanel(this.nodeComparisonPanel);
        this.hidePanel(this.traceGraphComparisonPanel);
    }

    @Override
    public void handleEvent(SetCurrentTraceGraphControllerEvent event) {
        this.showPanel(this.pdmPanel);
        this.pdmPanel.registerCallbacks(event.getTraceGraphController());
        var settingsPanel = event.getTraceGraphController().getSettingsPanel();
        this.hidePanel(this.traceGraphComparisonPanel);
        this.replaceEdgeDisplayControllerPanel(settingsPanel);
    }

    private void replaceEdgeDisplayControllerPanel(EdgeDisplayControllerPanel newPanel) {
        if (this.edgeDisplayControllerPanel != null) {
            var displayMode = this.edgeDisplayControllerPanel.getDisplayLocation();
            switch (displayMode) {
                case PANEL -> this.hidePanel(this.edgeDisplayControllerPanel);
                case NORTH -> {
                    BorderLayout layout = (BorderLayout) this.getLayout();
                    this.remove(layout.getLayoutComponent(BorderLayout.NORTH));
                }
                case SOUTH -> {
                }
            }
        }
        this.edgeDisplayControllerPanel = newPanel;
        if (newPanel != null) {
            var displayMode = this.edgeDisplayControllerPanel.getDisplayLocation();
            switch (displayMode) {
                case PANEL -> this.showPanel(newPanel);
                case NORTH -> this.add(newPanel, BorderLayout.NORTH);
            }
        }
    }

    @Override
    public void handleEvent(SetCurrentComparisonControllerEvent event) {
        this.traceGraphComparisonPanel.setComparisonController(event.getNetworkComparisonController());
        this.pdmPanel.registerCallbacks(event.getNetworkComparisonController());
        this.showPanel(this.traceGraphComparisonPanel);
        if (this.edgeDisplayControllerPanel != null) {
            this.hidePanel(this.edgeDisplayControllerPanel);
        }
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
        if (panel != pdmPanel && getPanelIndex(panel.getTitle()) == -1) {
            this.tabs.addTab(panel.getTitle(), panel);
        }
        this.tabs.setSelectedIndex(getPanelIndex(panel.getTitle()));
    }

    @Override
    public void handleEvent(SetCurrentEdgeDisplayControllerEvent e) {
        var current = e.getCurrentController();
        this.replaceEdgeDisplayControllerPanel(current.getSettingsPanel());
    }
}
