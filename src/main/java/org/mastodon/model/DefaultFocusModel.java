package org.mastodon.model;

import org.mastodon.graph.Edge;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.GraphListener;
import org.mastodon.graph.Vertex;
import org.mastodon.util.Listeners;

/**
 * Class to manage the focus of a model vertex in a graph, regardless of how
 * this focus is used.
 *
 * @param <V>
 *            type of model vertices in the graph.
 * @param <E>
 *            type of model edges in the graph.
 */
public class DefaultFocusModel< V extends Vertex< E >, E extends Edge< V > >
		implements FocusModel< V >, GraphListener< V, E >
{
	private final GraphIdBimap< V, E > idmap;

	private int focusVertexId;

	private final Listeners.List< FocusListener > listeners;

	public DefaultFocusModel( final GraphIdBimap< V, E > idmap )
	{
		this.idmap = idmap;
		focusVertexId = -1;
		listeners = new Listeners.SynchronizedList<>();
	}

	@Override
	public synchronized void focus( final V vertex )
	{
		final int id = ( vertex == null ) ? - 1 : idmap.getVertexId( vertex );
		if ( focusVertexId != id )
		{
			focusVertexId = id;
			notifyListeners();
		}
	}

	@Override
	public synchronized V getFocused( final V ref )
	{
		return ( focusVertexId < 0 ) ?
				null : idmap.getVertex( focusVertexId, ref );
	}

	@Override
	public Listeners< FocusListener > listeners()
	{
		return listeners;
	}

	private void notifyListeners()
	{
		for ( final FocusListener l : listeners.list )
			l.focusChanged();
	}

	@Override
	public void graphRebuilt()
	{
		focus( null );
// TODO: notifyListeners(); ? (This may change the layout and we might want to re-center on the focused vertex
	}

	@Override
	public void vertexAdded( final V vertex )
	{
// TODO: notifyListeners(); ? (This may change the layout and we might want to re-center on the focused vertex
	}

	@Override
	public synchronized void vertexRemoved( final V vertex )
	{
		if ( focusVertexId == idmap.getVertexId( vertex ) )
			focus( null );
// TODO: notifyListeners(); ? (This may change the layout and we might want to re-center on the focused vertex
	}

	@Override
	public void edgeAdded( final E edge )
	{}

	@Override
	public synchronized void edgeRemoved( final E edge )
	{}
}
