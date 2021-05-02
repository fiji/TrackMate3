package org.mastodon.views.bdv.overlay;

import java.util.concurrent.locks.Lock;

import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;

public class OverlayContext< V extends OverlayVertex< V, ? > > implements
		Context< V >,
		TransformListener< AffineTransform3D >
{
	private final OverlayGraph< V, ? > graph;

	private final SpatioTemporalIndex< V > index;

	private final OverlayGraphRenderer< V, ? > renderer;

	private ContextListener< V > contextListener = null;

	public OverlayContext(
			final OverlayGraph< V, ? > overlayGraph,
			final OverlayGraphRenderer< V, ? > renderer )
	{
		this.graph = overlayGraph;
		this.index = graph.getIndex();
		this.renderer = renderer;
	}

	@Override
	public Lock readLock()
	{
		return index.readLock();
	}

	@Override
	public Iterable< V > getInsideVertices( final int timepoint )
	{
//		final ConvexPolytope visiblePolytope = renderer.getVisiblePolytopeGlobal( transform, timepoint );
//		final ClipConvexPolytope< V > ccp = index.getSpatialIndex( timepoint ).getClipConvexPolytope();
//		ccp.clip( visiblePolytope );
//		return ccp.getInsideValues();
		return renderer.getVisibleVertices( transform, timepoint );
	}

	@Override
	public int getTimepoint()
	{
		return renderer.getCurrentTimepoint();
	}

	private final AffineTransform3D transform = new AffineTransform3D();

	@Override
	public void transformChanged( final AffineTransform3D t )
	{
		transform.set( t );
		if ( contextListener != null )
			contextListener.contextChanged( this );
	}

	public void setContextListener( final ContextListener< V > contextListener )
	{
		this.contextListener = contextListener;
	}
}
