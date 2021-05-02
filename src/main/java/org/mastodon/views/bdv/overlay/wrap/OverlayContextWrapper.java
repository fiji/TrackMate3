package org.mastodon.views.bdv.overlay.wrap;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.views.bdv.overlay.OverlayContext;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;

/**
 * Wraps an {@link OverlayContext} (a {@code Context} on
 * {@code OverlayVertexWrapper}) as a {@code Context} on model vertices
 * {@code V}. Passes on {@code contextChanged} notifications to a listener.
 *
 * @param <V>
 *            the type of model vertex wrapped.
 * @param <E>
 *            the type of model edge wrapped.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class OverlayContextWrapper< V extends Vertex< E >, E extends Edge< V > >
		implements Context< V >
{
	private final OverlayContext< OverlayVertexWrapper< V, E > > context;

	/**
	 * @param context
	 *            {@link OverlayContext} to wrap
	 * @param contextListener
	 *            {@code contextChanged} of {@code context} are translated an
	 *            send to this listeners.
	 */
	public OverlayContextWrapper(
			final OverlayContext< OverlayVertexWrapper< V, E > > context,
			final ContextListener< V > contextListener )
	{
		this.context = context;
		context.setContextListener( c -> contextListener.contextChanged( this ) );
	}

	@Override
	public Lock readLock()
	{
		return context.readLock();
	}

	@Override
	public Iterable< V > getInsideVertices( final int timepoint )
	{
		final Iterable< OverlayVertexWrapper< V, E > > insideVertices = context.getInsideVertices( timepoint );
		return new Iterable< V >()
		{
			@Override
			public Iterator< V > iterator()
			{
				final Iterator< OverlayVertexWrapper< V, E > > iter = insideVertices.iterator();
				return new Iterator< V >()
				{
					@Override
					public boolean hasNext()
					{
						return iter.hasNext();
					}

					@Override
					public V next()
					{
						return iter.next().wv;
					}
				};
			}
		};
	}

	@Override
	public int getTimepoint()
	{
		return context.getTimepoint();
	}
}
