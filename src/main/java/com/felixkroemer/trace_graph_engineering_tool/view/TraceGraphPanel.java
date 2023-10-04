package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class TraceGraphPanel extends JPanel implements CytoPanelComponent2, SelectedNodesAndEdgesListener {

    private Logger logger;
    private JTabbedPane tabs;
    private PDMPanel pdmPanel;
    private TracesPanel tracesPanel;
    private InfoPanel infoPanel;
    private CyServiceRegistrar reg;

    private static String TRACES_TITLE = "traces";
    private static String PDM_TITLE = "PDM";
    private static String INFO_TITLE = "Info";

    public TraceGraphPanel(CyServiceRegistrar reg) {
        super(new BorderLayout());
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
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

    public void registerCallbacks(TraceGraphController controller) {
        this.pdmPanel.registerCallbacks(controller.getTraceGraph().getPDM(), controller);
    }

    public void showTracesPanel() {
        this.tabs.addTab(TRACES_TITLE, this.tracesPanel);
    }

    public void showInfoPanel(CyNode node) {
        this.infoPanel.setNode(node);
        this.tabs.addTab(INFO_TITLE, this.infoPanel);
        this.tabs.setSelectedIndex(getPanelIndex(INFO_TITLE));
    }

    private int getPanelIndex(String title) {
        for (int i = 0; i < this.tabs.getTabCount(); i++) {
            if (this.tabs.getTitleAt(i).equals(title)) {
                return i;
            }
        }
        return -1;
    }

    public void hidePanel(String title) {
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
            this.showInfoPanel(event.getSelectedNodes().iterator().next());
        } else {
            this.hidePanel(INFO_TITLE);
        }
    }
}
