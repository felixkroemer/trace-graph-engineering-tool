package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;

import javax.swing.*;
import java.awt.*;

public class SetPercentileFilterPanel extends JPanel {

    private NetworkController controller;
    private JRadioButton visitDurationRadioButton;
    private JRadioButton frequencyRadioButton;
    private JTextField percentileField;
    private JButton confirmButton;
    private JButton cancelButton;

    public SetPercentileFilterPanel(NetworkController controller) {
        this.controller = controller;

        this.visitDurationRadioButton = new JRadioButton("Visit Duration");
        this.frequencyRadioButton = new JRadioButton("Frequency");
        this.percentileField = new JTextField(20);
        this.confirmButton = new JButton("Confirm");
        this.cancelButton = new JButton("Cancel");

        this.init();

        this.confirmButton.addActionListener(e -> {
            ((Window) getRootPane().getParent()).dispose();

            String column;
            if (this.visitDurationRadioButton.isSelected()) {
                column = "visitDuration";
            } else {
                column = "frequency";
            }

            try {
                int percentile = Integer.parseInt(this.percentileField.getText());
                this.controller.getPDM().setPercentile(column, percentile);
            } catch (NumberFormatException err) {
                JOptionPane.showMessageDialog(this, "Input must be integer", null, JOptionPane.ERROR_MESSAGE);
            }
        });

        this.cancelButton.addActionListener(e -> {
            ((Window) getRootPane().getParent()).dispose();
        });
    }

    public void init() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(10, 10, 10, 10);

        JLabel typeLabel = new JLabel("Type:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        this.add(typeLabel, constraints);

        constraints.gridx = 1;
        this.add(this.visitDurationRadioButton, constraints);

        constraints.gridx = 2;
        this.add(this.frequencyRadioButton, constraints);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.visitDurationRadioButton);
        buttonGroup.add(this.frequencyRadioButton);
        this.visitDurationRadioButton.setSelected(true);

        JLabel percentileLabel = new JLabel("Percentile:");
        constraints.gridx = 0;
        constraints.gridy = 1;
        this.add(percentileLabel, constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 2;
        this.add(this.percentileField, constraints);

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        this.add(this.confirmButton, constraints);

        constraints.gridx = 2;
        this.add(this.cancelButton, constraints);
    }
}