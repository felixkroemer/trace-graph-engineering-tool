package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;
import org.cytoscape.application.CyUserLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class PDMPanel extends JPanel {

    private Logger logger;

    private JScrollPane scrollPane;
    private JPanel innerPanel;
    private JList<Parameter> pdmList;

    public PDMPanel() {
        this.logger = LoggerFactory.getLogger(CyUserLog.NAME);

        this.pdmList = new JList<>();
        this.pdmList.setLayoutOrientation(JList.VERTICAL);
        this.innerPanel = new JPanel(new BorderLayout());
        this.scrollPane = new JScrollPane(this.innerPanel);
        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());
        this.scrollPane.add(pdmList);
        this.add(this.scrollPane, BorderLayout.CENTER);
        this.innerPanel.add(this.pdmList, BorderLayout.CENTER);
    }

    public void setPDM(ParameterDiscretizationModel pdm) {

        this.pdmList.setModel(pdm);
    }


}
