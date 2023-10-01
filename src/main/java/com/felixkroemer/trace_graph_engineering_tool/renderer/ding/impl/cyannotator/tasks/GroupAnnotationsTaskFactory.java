package com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.cyannotator.tasks;

import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DRenderingEngine;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.DingRenderer;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.cyannotator.AnnotationTree;
import com.felixkroemer.trace_graph_engineering_tool.renderer.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class GroupAnnotationsTaskFactory implements NetworkViewTaskFactory,TaskFactory {
  private final AnnotationManager annotationManager;
  private final CyNetworkViewManager viewManager;
  private final RenderingEngineManager reManager;
	private final DingRenderer dingRenderer;

	public GroupAnnotationsTaskFactory(
			AnnotationManager annotationManager,
      RenderingEngineManager reManager,
      CyNetworkViewManager viewManager
	) {
    this.annotationManager = annotationManager;
    this.viewManager = viewManager;
    this.reManager = reManager;
    this.dingRenderer = null;
  }

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new GroupAnnotationsTask(annotationManager, reManager, viewManager));
	}

	@Override
	public boolean isReady() {
    return true;
	}
	
	public GroupAnnotationsTaskFactory(DingRenderer dingRenderer) {
		this.dingRenderer = dingRenderer;
    this.annotationManager = null;
    this.viewManager = null;
    this.reManager = null;
	}
	
	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView) {
		DRenderingEngine re = dingRenderer.getRenderingEngine(networkView);
		if(re == null)
			return null;
		return new TaskIterator(new GroupAnnotationsTask(re));

	}

	@Override
	public boolean isReady(CyNetworkView networkView) {
		DRenderingEngine re = dingRenderer.getRenderingEngine(networkView);
		if(re == null)
			return false;
		CyAnnotator cyAnnotator = re.getCyAnnotator();
		return AnnotationTree.hasSameParent(cyAnnotator.getAnnotationSelection().getSelectedAnnotations());
	}
}
