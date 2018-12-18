package org.mastodon.adapter;

import org.mastodon.model.FocusListener;
import org.mastodon.model.FocusModel;

/**
 * Adapts a {@code FocusModel<V>} as a {@code FocusModel<WV>}. The mapping
 * between source objects ({@code V}) and wrapped objects ({@code WV}) is
 * established by a {@link RefBimap}.
 *
 * @param <T>
 *            type of source objects.
 * @param <WT>
 *            type of this wrapped {@link FocusModel}.
 *
 * @author Tobias Pietzsch
 */
public class FocusModelAdapter< T, WT >
		implements FocusModel< WT >
{
	private final FocusModel< T > focus;

	private final RefBimap< T, WT > vertexMap;

	private final ForwardedListeners< FocusListener > listeners;

	public FocusModelAdapter(
			final FocusModel< T > focus,
			final RefBimap< T, WT > vertexMap )
	{
		this.focus = focus;
		this.vertexMap = vertexMap;
		this.listeners = new ForwardedListeners.List<>( focus.listeners() );
	}

	@Override
	public void focus( final WT vertex )
	{
		focus.focus( vertexMap.getLeft( vertex ) );
	}

	@Override
	public WT getFocused( final WT ref )
	{
		return vertexMap.getRight( focus.getFocused( vertexMap.reusableLeftRef( ref ) ), ref );
	}

	@Override
	public ForwardedListeners< FocusListener > listeners()
	{
		return listeners;
	}
}
