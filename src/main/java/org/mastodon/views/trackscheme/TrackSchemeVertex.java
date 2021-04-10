package org.mastodon.views.trackscheme;

import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.ref.AbstractVertex;
import org.mastodon.model.HasLabel;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.PoolObject;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.views.trackscheme.TrackSchemeGraph.TrackSchemeVertexPool;

/**
 * The vertex class for TrackScheme.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class TrackSchemeVertex extends AbstractVertex< TrackSchemeVertex, TrackSchemeEdge, TrackSchemeVertexPool, ByteMappedElement > implements HasLabel, HasTimepoint
{
	final ModelGraphWrapper< ?, ? >.ModelVertexWrapper modelVertex;

	@Override
	protected void setToUninitializedState()
	{
		super.setToUninitializedState();
		setScreenVertexIndex( -1 );
	}

	TrackSchemeVertex initModelId( final int modelVertexId )
	{
		setModelVertexId( modelVertexId );
		setLayoutX( 0 );
		setLayoutTimestamp( -1 );
		setLayoutInEdgeIndex( 0 );
		setGhost( false );
		updateTimepointFromModel();
		return this;
	}

	/**
	 * Gets the ID of the associated model vertex, as defined by a
	 * {@link GraphIdBimap}. For {@link PoolObject} model vertices, the ID will
	 * be the internal pool index of the model vertex.
	 *
	 * @return the ID of the associated model vertex.
	 */
	public int getModelVertexId()
	{
		return pool.origVertexIndex.get( this );
	}

	protected void setModelVertexId( final int id )
	{
		pool.origVertexIndex.setQuiet( this, id );
	}

	@Override
	public String toString()
	{
		return String.format( "TrackSchemeVertex( ID=%d, LABEL=%s, X=%.2f, TIMEPOINT=%d )",
				getModelVertexId(),
				getLabel(),
				getLayoutX(),
				getTimepoint() );
	}

	TrackSchemeVertex( final TrackSchemeVertexPool pool )
	{
		super( pool );
		modelVertex = pool.modelGraphWrapper.createVertexWrapper( this );
	}

	/**
	 * Gets the label of associated model vertex.
	 *
	 * @return the label.
	 *
	 */
	@Override
	public String getLabel()
	{
		return modelVertex.getLabel();
	}

	/**
	 * Sets the label of associated model vertex.
	 *
	 * @param label
	 *            the label to set.
	 */
	@Override
	public void setLabel( final String label )
	{
		modelVertex.setLabel( label );
	}

	@Override
	public int getTimepoint()
	{
		return pool.timepoint.get( this );
	}

	protected void setTimepoint( final int timepoint )
	{
		pool.timepoint.setQuiet( this, timepoint );
	}

	protected void updateTimepointFromModel()
	{
		setTimepoint( modelVertex.getTimepoint() );
	}

	/**
	 * Internal pool index of last {@link ScreenVertex} that was created for
	 * this vertex. Used for lookup when creating {@link ScreenEdge}s.
	 *
	 * @return internal pool index of associated {@link ScreenVertex}.
	 */
	public int getScreenVertexIndex()
	{
		return pool.screenVertexIndex.get( this );
	}

	protected void setScreenVertexIndex( final int screenVertexIndex )
	{
		pool.screenVertexIndex.setQuiet( this, screenVertexIndex );
	}

	public double getLayoutX()
	{
		return pool.layoutX.get( this );
	}

	protected void setLayoutX( final double x )
	{
		pool.layoutX.setQuiet( this, x );
	}

	/**
	 * Layout timestamp is set when this vertex is layouted (assigned a
	 * {@link #getLayoutX() coordinate}). It is also used to mark active
	 * vertices before a partial layout.
	 *
	 * @return layout timestamp.
	 */
	public int getLayoutTimestamp()
	{
		return pool.layoutTimeStamp.get( this );
	}

	public void setLayoutTimestamp( final int timestamp )
	{
		pool.layoutTimeStamp.setQuiet( this, timestamp );
	}

	/**
	 * internal pool index of first (and only) edge through which this vertex
	 * was touched in last layout.
	 * <p>
	 * TODO: REMOVE? This is not be needed because VertexOrder is built directly
	 * during layout now.
	 *
	 * @return internal pool index of first (and only) edge through which this
	 *         vertex was touched in last layout.
	 */
	protected int getLayoutInEdgeIndex()
	{
		return pool.layoutInEdgeIndex.get( this );
	}

	protected void setLayoutInEdgeIndex( final int index )
	{
		pool.layoutInEdgeIndex.setQuiet( this, index );
	}

	/**
	 * A vertex is set to <em>ghost</em> if it is hit during a partial layout
	 * and is marked with a timestamp &lt; the current mark.
	 *
	 * @return whether this vertex is a ghost
	 */
	protected boolean isGhost()
	{
		return pool.ghost.get( this );
	}

	protected void setGhost( final boolean ghost )
	{
		pool.ghost.setQuiet( this, ghost );
	}
}
