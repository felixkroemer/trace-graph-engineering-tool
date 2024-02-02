package com.felixkroemer.trace_graph_engineering_tool.model;

import java.util.Collections;

/*
 * Auxiliary node/edge information that is not contained in the local tables
 */
public class NodeAuxiliaryInformation extends AuxiliaryInformation {

    private int visitDuration;
    private int frequency;

    public NodeAuxiliaryInformation() {
        super();
        this.visitDuration = 0;
        this.frequency = 0;
    }

    public NodeAuxiliaryInformation(NodeAuxiliaryInformation information) {
        super(information);
        this.visitDuration = information.visitDuration;
        this.frequency = information.frequency;
    }

    public void incrementVisitDuration() {
        this.visitDuration += 1;
    }

    public int getVisitDuration() {
        return this.visitDuration;
    }

    public void incrementFrequency() {
        this.frequency += 1;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void fixVisitDurationAndFrequency() {
        int visitDuration = 0;
        int frequency = 0;

        for (var pair : this.source_rows) {
            Collections.sort(pair.getValue1());
            int prev = Integer.MIN_VALUE;
            for (int i : pair.getValue1()) {
                if (i == prev + 1) {
                    visitDuration += 1;
                } else {
                    frequency += 1;
                }
                prev = i;
            }
        }
        this.visitDuration = visitDuration;
        this.frequency = frequency;
    }
}
