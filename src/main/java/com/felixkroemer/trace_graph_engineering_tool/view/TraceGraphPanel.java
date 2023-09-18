package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.TraceGraph;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class TraceGraphPanel extends JPanel implements CytoPanelComponent2 {

    private Logger logger;
    private JTabbedPane tabs;
    private PDMPanel pdmPanel;
    private CyServiceRegistrar reg;

    public TraceGraphPanel(CyServiceRegistrar reg) {
        super(new BorderLayout());
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        this.tabs = new JTabbedPane(JTabbedPane.BOTTOM);
        this.pdmPanel = new PDMPanel(reg);
        this.reg = reg;
        init();
    }

    @Override
    public Component getComponent() {
        return this;
    }

    public void init() {
        this.tabs.add("PDM", this.pdmPanel);
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

    public void registerCallbacks(TraceGraph tg) {
        this.pdmPanel.registerCallbacks(tg.getPDM());
    }

    public void clear() {
        this.pdmPanel.clear();
    }
}
