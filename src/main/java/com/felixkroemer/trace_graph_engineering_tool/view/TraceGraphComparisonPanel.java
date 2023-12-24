package com.felixkroemer.trace_graph_engineering_tool.view;

import com.felixkroemer.trace_graph_engineering_tool.controller.NetworkComparisonController;
import org.javatuples.Pair;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import static com.felixkroemer.trace_graph_engineering_tool.controller.NetworkComparisonController.*;

public class TraceGraphComparisonPanel extends TraceGraphPanel {

    private JToggleButton nodesBOButton;
    private JToggleButton nodesDOButton;
    private JToggleButton nodesBDButton;
    private JToggleButton edgesBOButton;
    private JToggleButton edgesDOButton;
    private JToggleButton edgesBDButton;
    private JPanel togglePanel;

    public TraceGraphComparisonPanel() {
        this.nodesBOButton = new JToggleButton();
        this.nodesDOButton = new JToggleButton();
        this.nodesBDButton = new JToggleButton();
        this.edgesBOButton = new JToggleButton();
        this.edgesDOButton = new JToggleButton();
        this.edgesBDButton = new JToggleButton();

        this.nodesBOButton.setSelected(true);
        this.nodesDOButton.setSelected(true);
        this.nodesBDButton.setSelected(true);
        this.edgesBOButton.setSelected(true);
        this.edgesDOButton.setSelected(true);
        this.edgesBDButton.setSelected(true);

        this.togglePanel = new JPanel();

        this.init();
    }

    static JLabel createCenteredJLabel(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        return label;
    }

    public void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        togglePanel.setLayout(new GridBagLayout());
        togglePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        this.togglePanel.setBorder(new TitledBorder("Test"));
        this.add(togglePanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.ipady = 20;

        gbc.gridx = 1;
        gbc.gridy = 0;
        togglePanel.add(createCenteredJLabel("Base Only"), gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        togglePanel.add(createCenteredJLabel("Delta Only"), gbc);

        gbc.gridx = 3;
        gbc.gridy = 0;
        togglePanel.add(createCenteredJLabel("Base + Delta"), gbc);

        gbc.ipady = 0;
        gbc.weighty = 1.0 / 2.0;

        gbc.weightx = 0;
        gbc.ipadx = 20;

        gbc.gridx = 0;
        gbc.gridy = 1;
        togglePanel.add(createCenteredJLabel("Nodes"), gbc);

        gbc.weightx = 1.0 / 3.0;
        gbc.ipadx = 0;

        gbc.gridx = 1;
        gbc.gridy = 1;
        togglePanel.add(nodesBOButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        togglePanel.add(nodesDOButton, gbc);

        gbc.gridx = 3;
        gbc.gridy = 1;
        togglePanel.add(nodesBDButton, gbc);

        gbc.weightx = 0;
        gbc.ipadx = 20;

        gbc.gridx = 0;
        gbc.gridy = 2;
        togglePanel.add(createCenteredJLabel("Edges"), gbc);

        gbc.weightx = 1.0 / 3.0;

        gbc.ipadx = 0;

        gbc.gridx = 1;
        gbc.gridy = 2;
        togglePanel.add(edgesBOButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        togglePanel.add(edgesDOButton, gbc);

        gbc.gridx = 3;
        gbc.gridy = 2;
        togglePanel.add(edgesBDButton, gbc);
    }

    public void setComparisonController(NetworkComparisonController controller) {
        var buttons = new HashMap<JToggleButton, Pair<String, Boolean>>();
        buttons.put(nodesBOButton, new Pair<>(BO, true));
        buttons.put(nodesDOButton, new Pair<>(DO, true));

        buttons.put(nodesBDButton, new Pair<>(BD, true));
        buttons.put(edgesBOButton, new Pair<>(BO, false));
        buttons.put(edgesDOButton, new Pair<>(DO, false));
        buttons.put(edgesBDButton, new Pair<>(BD, false));
        for (var entry : buttons.entrySet()) {
            // remove old listeners before settings toggle state
            for (ItemListener al : entry.getKey().getItemListeners()) {
                entry.getKey().removeItemListener(al);
            }
            entry.getKey().setSelected(controller.getGroupVisibility(entry.getValue().getValue0(),
                    entry.getValue().getValue1()));
            entry.getKey().addItemListener(e -> {
                controller.setGroupVisibility(entry.getValue().getValue0(), entry.getValue().getValue1(),
                        e.getStateChange() == ItemEvent.SELECTED);
            });
        }
    }

    @Override
    public String getTitle() {
        return "Trace Graph Comparison";
    }
}
