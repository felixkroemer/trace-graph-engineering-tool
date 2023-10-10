package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import com.felixkroemer.trace_graph_engineering_tool.controller.TraceGraphController;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;
import static org.cytoscape.util.swing.IconManager.ICON_EDIT;

public class ParameterCell extends JPanel implements PropertyChangeListener {

    private JCheckBox checkBox;
    private JLabel label;
    private JButton editButton;
    private JLabel highlightIndicator;

    public ParameterCell(CyServiceRegistrar reg, Parameter parameter, TraceGraphController controller) {
        IconManager iconManager = reg.getService(IconManager.class);

        setLayout(new BorderLayout());
        this.checkBox = new JCheckBox();
        this.add(this.checkBox, BorderLayout.WEST);
        this.checkBox.setSelected(parameter.isEnabled());
        this.label = new JLabel(parameter.getName());
        this.add(this.label, BorderLayout.CENTER);
        this.label.setHorizontalAlignment(JLabel.CENTER);

        var containerPanel = new JPanel();
        this.highlightIndicator = new JLabel();
        containerPanel.add(highlightIndicator);
        this.editButton = new JButton(ICON_EDIT);
        containerPanel.add(this.editButton);
        this.add(containerPanel, BorderLayout.EAST);

        this.editButton.setFont(iconManager.getIconFont(14.0f));
        this.editButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                SelectBinsDialog d = new SelectBinsDialog();
                d.setTitle("Select Bins");
                d.setContentPane(new SelectBinsPanel(controller.createSelectBinsController(parameter)));
                d.setModalityType(APPLICATION_MODAL);
                d.showDialog();
            });
        });
        this.checkBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                parameter.enable();
            } else {
                parameter.disable();
            }
        });
        this.highlightIndicator.setText(" ");

        this.label.setEnabled(parameter.isEnabled());
        this.editButton.setEnabled(parameter.isEnabled());
    }

    public JCheckBox getCheckBox() {
        return this.checkBox;
    }

    public JLabel getLabel() {
        return this.label;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "enabled" -> {
                this.label.setEnabled((boolean) evt.getNewValue());
                this.editButton.setEnabled((boolean) evt.getNewValue());
            }
            case "bins" -> {
            }
            case "highlightedBins" -> {
                Set<Integer> bins = (Set<Integer>) evt.getNewValue();
                this.highlightIndicator.setText(bins.isEmpty() ? " " : "⬤");
            }
        }
    }
}
