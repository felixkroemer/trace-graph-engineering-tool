package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
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

    public void setPDM(ParameterDiscretizationModel pdm) {
        SwingUtilities.invokeLater(() -> {
            this.innerPanel.removeAll();
            ParameterCell cell;
            TraceGraphController controller = reg.getService(TraceGraphController.class);
            for (Parameter param : pdm.getParameters()) {
                cell = new ParameterCell(param.getName());
                param.addObserver(cell);
                cell.getCheckBox().addItemListener(e -> {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        controller.onParameterEnabled(param);
                    } else {
                        controller.onParameterDisabled(param);
                    }
                });
                this.innerPanel.add(cell);
            }
        });
    }
}
