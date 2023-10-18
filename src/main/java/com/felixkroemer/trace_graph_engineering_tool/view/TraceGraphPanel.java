package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentComparisonControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentComparisonControllerListener;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentTraceGraphControllerEvent;
import com.felixkroemer.trace_graph_engineering_tool.events.SetCurrentTraceGraphControllerListener;
import com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel.PDMPanel;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.*;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class TraceGraphPanel extends JPanel implements CytoPanelComponent2, SelectedNodesAndEdgesListener,
        SetCurrentNetworkListener, SetCurrentTraceGraphControllerListener, SetCurrentComparisonControllerListener {

    private Logger logger;
    private TraceGraphManager manager;
    private JTabbedPane tabs;
    private PDMPanel pdmPanel;
    private TracesPanel tracesPanel;
    private InfoPanel infoPanel;
    private ComparisonPanel comparisonPanel;
    private CyServiceRegistrar reg;

    private static String PDM_TITLE = "PDM";
    private static String INFO_TITLE = "Info";
    private static String COMPARISON_TITLE = "Comparison";

    public TraceGraphPanel(CyServiceRegistrar reg, TraceGraphManager manager) {
        super(new BorderLayout());
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.manager = manager;
        this.tabs = new JTabbedPane(JTabbedPane.BOTTOM);
        this.pdmPanel = new PDMPanel(reg);
        this.tracesPanel = new TracesPanel(reg);
        this.infoPanel = new InfoPanel(reg);
        this.comparisonPanel = new ComparisonPanel();
        this.reg = reg;

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
        if (event.getSelectedNodes().size() == 1) {
            var manager = this.reg.getService(TraceGraphManager.class);
            var controller = manager.findControllerForNetwork(event.getNetwork());
            //TODO: bug, controller is null if network is a comparison network
            this.infoPanel.setNode(controller, event.getSelectedNodes().iterator().next());
            this.tabs.addTab(INFO_TITLE, this.infoPanel);
            this.tabs.setSelectedIndex(getPanelIndex(INFO_TITLE));
            this.showPanel();
        } else {
            this.hidePanel(INFO_TITLE);
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
        this.hidePanel(COMPARISON_TITLE);
    }

    @Override
    public void handleEvent(SetCurrentComparisonControllerEvent event) {
        this.comparisonPanel.setComparisonController(event.getNetworkComparisonController());
        this.tabs.addTab(COMPARISON_TITLE, this.comparisonPanel);
        this.tabs.setSelectedIndex(getPanelIndex(COMPARISON_TITLE));
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
