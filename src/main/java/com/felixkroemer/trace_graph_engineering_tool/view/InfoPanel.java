package com.felixkroemer.trace_graph_engineering_tool.view;

import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;

import javax.swing.*;
import java.awt.*;

public class InfoPanel extends JPanel {

    private CyServiceRegistrar reg;
    private JLabel text;
    private CyNode node;

    public InfoPanel(CyServiceRegistrar reg) {
        this.reg = reg;
        this.node = null;
        this.text = new JLabel("abc");
        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());
        this.add(text, BorderLayout.CENTER);
    }

    public void setNode(CyNode node) {
        this.text.setText(node.getSUID().toString());
    }

}
