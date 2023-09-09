package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class TraceGraphPanel extends JPanel implements CytoPanelComponent2, SetCurrentNetworkListener {

    private Logger logger;

    @Override
    public Component getComponent() {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);
        return this;
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

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        if (e.getNetwork() != null && Util.isTraceGraphNetwork(e.getNetwork())) {
            logger.info("Enable TraceGraph panel");
        } else {
            logger.info("Disable TraceGraph panel");
        }
    }
}
