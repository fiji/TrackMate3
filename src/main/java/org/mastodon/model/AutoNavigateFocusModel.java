package org.mastodon.model;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.util.Listeners;

/**
 * A {@code FocusModel} that calls {@code notifyNavigateToVertex()} on
 * {@code focusVertex()}.
 * <p>
 * This allows to implement view-follows-focus behaviour on demand (without
 * having to hard-wire navigation into {@code FocusActions}).
 *
 * @param <V>
 *            the type of vertices in the graph.
 * @param <E>
 *            the type of edges in the graph.
 */
public class AutoNavigateFocusModel< V extends Vertex< E >, E extends Edge< V > > implements FocusModel< V >
{
	private final FocusModel< V > focus;

	private final NavigationHandler< V, E > navigation;

	public AutoNavigateFocusModel(
			final FocusModel< V > focus,
			final NavigationHandler< V, E > navigation )
	{
		this.focus = focus;
		this.navigation = navigation;
	}

	@Override
	public void focus( final V vertex )
	{
		focus.focus( vertex );
		navigation.notifyNavigateToVertex( vertex );
	}

	@Override
	public V getFocused( final V ref )
	{
		return focus.getFocused( ref );
	}

	@Override
	public Listeners< FocusListener > listeners()
	{
		return focus.listeners();
	}
}
