package com.felixkroemer.trace_graph_engineering_tool.view.pdm_panel;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkController;
import com.felixkroemer.trace_graph_engineering_tool.model.Parameter;
import com.felixkroemer.trace_graph_engineering_tool.util.Util;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import static org.cytoscape.util.swing.IconManager.ICON_EDIT;

public class ParameterCell extends JPanel implements PropertyChangeListener {

    private JLabel nameLabel;
    private JButton editButton;
    private JLabel filterIndicator;

    public ParameterCell(CyServiceRegistrar reg, Parameter parameter, NetworkController controller) {
        IconManager iconManager = reg.getService(IconManager.class);

        setLayout(new BorderLayout());
        JCheckBox checkBox = new JCheckBox();
        this.add(checkBox, BorderLayout.WEST);
        checkBox.setSelected(parameter.isEnabled());
        this.nameLabel = new JLabel(parameter.getName());
        this.add(this.nameLabel, BorderLayout.CENTER);
        this.nameLabel.setHorizontalAlignment(JLabel.CENTER);

        var containerPanel = new JPanel();
        this.filterIndicator = new JLabel();
        containerPanel.add(filterIndicator);
        this.editButton = new JButton(ICON_EDIT);
        containerPanel.add(this.editButton);
        this.add(containerPanel, BorderLayout.EAST);

        this.editButton.setFont(iconManager.getIconFont(18.0f));
        this.editButton.setMargin(new Insets(5, 5, 5, 5));
        this.editButton.addActionListener(e -> SwingUtilities.invokeLater(() -> Util.showDialog(new SelectBinsPanel(controller.createSelectBinsController(parameter)), "Select bins")));
        checkBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                parameter.enable();
            } else {
                parameter.disable();
            }
        });
        this.setHighlightIndicator(parameter.getVisibleBins());

        this.nameLabel.setEnabled(parameter.isEnabled());
        this.editButton.setEnabled(parameter.isEnabled());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case Parameter.ENABLED -> {
                this.nameLabel.setEnabled((boolean) evt.getNewValue());
                this.editButton.setEnabled((boolean) evt.getNewValue());
            }
            case Parameter.BINS -> {
            }
            case Parameter.VISIBLE_BINS -> this.setHighlightIndicator((Set<Integer>) evt.getNewValue());
        }
    }

    public void setHighlightIndicator(Set<Integer> bins) {
        this.filterIndicator.setText(bins.isEmpty() ? " " : "⬤");
    }
}
