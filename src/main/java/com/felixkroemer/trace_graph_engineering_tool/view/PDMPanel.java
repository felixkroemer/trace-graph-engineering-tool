package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class PDMPanel extends JPanel {

    private Logger logger;

    private JScrollPane scrollPane;
    private JPanel innerPanel;
    private CyServiceRegistrar reg;

    public PDMPanel(CyServiceRegistrar reg) {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);

        this.innerPanel = new JPanel();
        this.innerPanel.setLayout(new BoxLayout(this.innerPanel, BoxLayout.Y_AXIS));
        this.scrollPane = new JScrollPane(this.innerPanel);
        this.reg = reg;
        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());
        this.add(this.scrollPane, BorderLayout.CENTER);
        this.scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    public void registerCallbacks(ParameterDiscretizationModel pdm) {
        SwingUtilities.invokeLater(() -> {
            this.innerPanel.removeAll();
            pdm.forEach(p -> {
                ParameterCell cell = new ParameterCell(p, reg);
                p.addObserver(cell);
                cell.getCheckBox().addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        p.enable();
                    } else {
                        p.disable();
                    }
                });
                this.innerPanel.add(cell);
            });
        });
    }

    public void clear() {
        this.innerPanel.removeAll();
    }
}
