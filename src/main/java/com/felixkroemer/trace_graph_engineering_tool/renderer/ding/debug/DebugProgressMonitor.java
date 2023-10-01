package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.debug;

import java.util.List;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;

public interface DebugProgressMonitor extends ProgressMonitor {

	List<DebugSubProgressMonitor> getSubMonitors();
	
	long getTime();
	
}
