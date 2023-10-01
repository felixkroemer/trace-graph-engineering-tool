package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.debug;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.work.ProgressMonitor;

public class DebugProgressMonitorFactory {

	private final DingDebugMediator mediator;
	
	public DebugProgressMonitorFactory(DingDebugMediator mediator) {
		this.mediator = mediator;
	}
	
	public DebugRootProgressMonitor create(DebugFrameType type, ProgressMonitor delegate) {
		return new DebugRootProgressMonitor(type, delegate, mediator);
	}
}
