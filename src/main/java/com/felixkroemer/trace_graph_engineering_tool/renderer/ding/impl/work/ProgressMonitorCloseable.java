package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work;

public interface ProgressMonitorCloseable extends AutoCloseable {

	@Override
	void close();
}
