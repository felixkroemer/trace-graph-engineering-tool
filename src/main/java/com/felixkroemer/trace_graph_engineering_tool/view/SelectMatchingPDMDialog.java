package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;

import javax.swing.*;
import java.awt.*;

public class SelectMatchingPDMDialog extends JDialog {

    private JButton confirmButton;
    private JButton cancelButton;

    private ParameterDiscretizationModel selectedPDM;

    public SelectMatchingPDMDialog() {
        this.confirmButton = new JButton("Confirm");
        this.cancelButton = new JButton("Cancel");
        this.selectedPDM = null;

        this.init();
    }

    private void init() {
        setLayout(new BorderLayout());

        JPanel innerPanel = new JPanel();
        this.add(innerPanel, BorderLayout.CENTER);

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        bottomButtonPanel.add(confirmButton);
        bottomButtonPanel.add(cancelButton);

        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    public ParameterDiscretizationModel showDialog() {
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        return selectedPDM;
    }
}