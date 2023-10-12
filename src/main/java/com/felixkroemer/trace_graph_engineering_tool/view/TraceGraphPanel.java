package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphManager;
import com.felixkroemer.trace_graph_engineering_tool.model.UIState;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel.PDMPanel;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class TraceGraphPanel extends JPanel implements CytoPanelComponent2, SelectedNodesAndEdgesListener, SetCurrentNetworkListener {

    private Logger logger;
    private TraceGraphManager manager;
    private JTabbedPane tabs;
    private PDMPanel pdmPanel;
    private TracesPanel tracesPanel;
    private InfoPanel infoPanel;
    private CyServiceRegistrar reg;

    private static String TRACES_TITLE = "traces";
    private static String PDM_TITLE = "PDM";
    private static String INFO_TITLE = "Info";

    public TraceGraphPanel(CyServiceRegistrar reg, TraceGraphManager manager) {
        super(new BorderLayout());
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.manager = manager;
        this.tabs = new JTabbedPane(JTabbedPane.BOTTOM);
        this.pdmPanel = new PDMPanel(reg);
        this.tracesPanel = new TracesPanel(reg);
        this.infoPanel = new InfoPanel(reg);
        this.reg = reg;
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
        return "TraceGraphPanel";
    }

    public void registerCallbacks(TraceGraphController controller, UIState uiState) {
        this.pdmPanel.registerCallbacks(controller, uiState);
    }

    public void showTracesPanel() {
        this.tabs.addTab(TRACES_TITLE, this.tracesPanel);
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

    public void clear() {
        this.pdmPanel.clear();
    }

    @Override
    public void handleEvent(SelectedNodesAndEdgesEvent event) {
        if (event.getSelectedNodes().size() == 1) {
            this.infoPanel.setNode(event.getNetwork(), event.getSelectedNodes().iterator().next());
            this.tabs.addTab(INFO_TITLE, this.infoPanel);
            this.tabs.setSelectedIndex(getPanelIndex(INFO_TITLE));
        } else {
            this.hidePanel(INFO_TITLE);
        }
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() != null && Util.isTraceGraphNetwork(e.getNetwork())) {
            var controller = this.manager.findControllerForNetwork(e.getNetwork());
            //TODO: distinction between regular trace graphs and comparison graphs
            if (controller != null) {
                this.registerCallbacks(controller, controller.getUiState());
            }
        } else {
            this.clear();
        }
    }
}
