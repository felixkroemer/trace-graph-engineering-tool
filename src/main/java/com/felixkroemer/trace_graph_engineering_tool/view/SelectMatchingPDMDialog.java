package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.model.ParameterDiscretizationModel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SelectMatchingPDMDialog extends JDialog {

    private JButton confirmButton;
    private JButton cancelButton;
    private List<ParameterDiscretizationModel> pdms;
    private Runnable createNewNetwork;
    private Consumer<ParameterDiscretizationModel> addToExistingNetwork;
    private ButtonGroup pdmRadioButtonGroup;
    private Map<AbstractButton, ParameterDiscretizationModel> pdmRadioButtonMap;

    public SelectMatchingPDMDialog(List<ParameterDiscretizationModel> pdms, Runnable createNewNetwork,
                                   Consumer<ParameterDiscretizationModel> addToExistingNetworkRunnable,
                                   boolean allowDefault) {
        this.confirmButton = new JButton("Confirm");
        this.cancelButton = new JButton("Cancel");
        this.pdms = pdms;
        this.createNewNetwork = createNewNetwork;
        this.addToExistingNetwork = addToExistingNetworkRunnable;
        this.pdmRadioButtonGroup = new ButtonGroup();
        this.pdmRadioButtonMap = new HashMap<>();

        this.init(allowDefault);
    }

    private void init(boolean allowDefault) {
        setLayout(new BorderLayout());

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        this.add(innerPanel, BorderLayout.CENTER);

        for (ParameterDiscretizationModel pdm : this.pdms) {
            JRadioButton button = new JRadioButton(pdm.getName());
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            this.pdmRadioButtonGroup.add(button);
            this.pdmRadioButtonMap.put(button, pdm);
            innerPanel.add(button);
        }

        JRadioButton newPDMButton = new JRadioButton("Create new network");
        newPDMButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.pdmRadioButtonGroup.add(newPDMButton);
        if (allowDefault) {
            this.pdmRadioButtonMap.put(newPDMButton, null);
            innerPanel.add(newPDMButton);
        }

        this.pdmRadioButtonGroup.getElements().nextElement().setSelected(true);

        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        bottomButtonPanel.add(confirmButton);
        bottomButtonPanel.add(cancelButton);

        this.confirmButton.addActionListener((e -> {
            ((Window) getRootPane().getParent()).dispose();

            var iterator = this.pdmRadioButtonGroup.getElements();
            while (iterator.hasMoreElements()) {
                var button = iterator.nextElement();
                if (button.isSelected()) {
                    var pdm = this.pdmRadioButtonMap.get(button);
                    if (pdm != null) {
                        this.addToExistingNetwork.accept(pdm);
                    } else {
                        this.createNewNetwork.run();
                    }
                    return;
                }
            }
        }));

        this.cancelButton.addActionListener((e) -> {
            ((Window) getRootPane().getParent()).dispose();
        });

        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    public void showDialog() {
        this.pack();
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setVisible(true);
    }
}