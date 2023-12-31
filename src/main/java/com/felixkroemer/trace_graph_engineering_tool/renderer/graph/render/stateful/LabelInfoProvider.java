package com.felixkroemer.trace_graph_engineering_tool.renderer.graph.render.stateful;

import java.awt.*;
import java.awt.font.FontRenderContext;

public interface LabelInfoProvider {

    public static LabelInfoProvider NO_CACHE = new LabelInfoProvider() {
    };

    public default LabelInfo getLabelInfo(String text, Font font, double labelWidth, FontRenderContext frc) {
        return new LabelInfo(text, font, frc, false, labelWidth);
    }

    public default String getStats() {
        return "no stats";
    }

}
