package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl;

import java.awt.Image;

@FunctionalInterface
public interface ThumbnailChangeListener {

	void thumbnailChanged(Image image);
}
