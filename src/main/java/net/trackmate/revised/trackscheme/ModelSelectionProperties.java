package net.trackmate.revised.trackscheme;

import net.trackmate.revised.ui.selection.SelectionListener;


public interface ModelSelectionProperties
{
	public void setVertexSelected( int vertexId, boolean selected );

	public void setEdgeSelected( int edgeId, boolean selected );

	public void toggleVertexSelected( int vertexId );

	public void toggleEdgeSelected( int edgeId );

	public boolean isVertexSelected( int vertexId );

	public boolean isEdgeSelected( int edgeId );

	public boolean addSelectionListener( final SelectionListener l );

	public boolean removeSelectionListener( final SelectionListener l );

	public void clearSelection();

	public void resumeListeners();

	public void pauseListeners();
}
