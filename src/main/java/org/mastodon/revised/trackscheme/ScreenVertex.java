package org.mastodon.revised.trackscheme;

import static org.mastodon.pool.ByteUtils.BOOLEAN_SIZE;
import static org.mastodon.pool.ByteUtils.BYTE_SIZE;
import static org.mastodon.pool.ByteUtils.DOUBLE_SIZE;
import static org.mastodon.pool.ByteUtils.INDEX_SIZE;
import static org.mastodon.revised.trackscheme.ScreenVertex.Transition.NONE;

import org.mastodon.RefPool;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.MemPool;
import org.mastodon.pool.Pool;
import org.mastodon.pool.PoolObject;
import org.mastodon.pool.SingleArrayMemPool;

/**
 * Layouted vertex.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class ScreenVertex extends PoolObject< ScreenVertex, ByteMappedElement >
{
	protected static final int ORIG_VERTEX_INDEX_OFFSET = 0;
	protected static final int X_OFFSET = ORIG_VERTEX_INDEX_OFFSET + INDEX_SIZE;
	protected static final int Y_OFFSET = X_OFFSET + DOUBLE_SIZE;
	protected static final int VERTEX_DIST_OFFSET = Y_OFFSET + DOUBLE_SIZE;
	protected static final int SELECTED_OFFSET = VERTEX_DIST_OFFSET + DOUBLE_SIZE;
	protected static final int GHOST_OFFSET = SELECTED_OFFSET + BOOLEAN_SIZE;
	protected static final int TRANSITION_OFFSET = GHOST_OFFSET + BOOLEAN_SIZE;
	protected static final int IP_SCREENVERTEX_INDEX_OFFSET = TRANSITION_OFFSET + BYTE_SIZE;
	protected static final int IP_RATIO_OFFSET = IP_SCREENVERTEX_INDEX_OFFSET + INDEX_SIZE;
	protected static final int SIZE_IN_BYTES = IP_RATIO_OFFSET + DOUBLE_SIZE;

	private final TrackSchemeVertex vref;

	private final RefPool< TrackSchemeVertex > trackSchemeVertexPool;

	public static enum Transition
	{
		NONE( 0 ),
		APPEAR( 1 ),
		DISAPPEAR( 2 ),
		SELECTING( 3 ),
		DESELECTING( 4 ), ;

		private final byte index;

		private Transition( final int index )
		{
			this.index = ( byte ) index;
		}

		public byte toByte()
		{
			return index;
		}
	}

	protected ScreenVertex( final Pool< ScreenVertex, ByteMappedElement > pool, final RefPool< TrackSchemeVertex > trackSchemeVertexPool )
	{
		super( pool );
		this.trackSchemeVertexPool = trackSchemeVertexPool;
		this.vref = trackSchemeVertexPool.createRef();
	}

	public ScreenVertex init(
			final int id,
			final double x,
			final double y,
			final boolean selected,
			final boolean ghost )
	{
		setTrackSchemeVertexId( id );
		setX( x );
		setY( y );
		setSelected( selected );
		setGhost( ghost );
		setTransition( NONE );
		return this;
	}

	/**
	 * Get the internal pool index of the associated {@link TrackSchemeVertex}.
	 *
	 * @return the internal pool index of the associated
	 *         {@link TrackSchemeVertex}.
	 */
	public int getTrackSchemeVertexId()
	{
		return access.getIndex( ORIG_VERTEX_INDEX_OFFSET );
	}

	protected void setTrackSchemeVertexId( final int id )
	{
		access.putIndex( id, ORIG_VERTEX_INDEX_OFFSET );
	}

	/**
	 * Get the X screen coordinate of the vertex center.
	 *
	 * @return X screen coordinate.
	 */
	public double getX()
	{
		return access.getDouble( X_OFFSET );
	}

	protected void setX( final double x )
	{
		access.putDouble( x, X_OFFSET );
	}

	/**
	 * Get the Y screen coordinate of the vertex center.
	 *
	 * @return Y screen coordinate.
	 */
	public double getY()
	{
		return access.getDouble( Y_OFFSET );
	}

	protected void setY( final double y )
	{
		access.putDouble( y, Y_OFFSET );
	}

	/**
	 * (Estimate of) the distance to the closest vertex on screen.
	 * Center-to-center distance. This is used to determine how large the vertex
	 * should be painted.
	 *
	 * @return distance to the closest vertex on screen.
	 */
	public double getVertexDist()
	{
		return access.getDouble( VERTEX_DIST_OFFSET );
	}

	protected void setVertexDist( final double minVertexScreenDist )
	{
		access.putDouble( minVertexScreenDist, VERTEX_DIST_OFFSET );
	}

	/**
	 * Get the label of the vertex. This calls
	 * {@link TrackSchemeVertex#getLabel()} of the associated
	 * {@link TrackSchemeVertex}.
	 *
	 * @return label of the vertex.
	 */
	public String getLabel()
	{
		final int idx = getTrackSchemeVertexId();
		if ( idx >= 0 )
		{
			trackSchemeVertexPool.getObject( idx, vref );
			return vref.getLabel();
		}
		else
			return "XXX";
	}

	/**
	 * Get the selected state of the vertex.
	 *
	 * @return true, if the vertex is selected.
	 */
	public boolean isSelected()
	{
		return access.getBoolean( SELECTED_OFFSET );
	}

	protected void setSelected( final boolean selected )
	{
		access.putBoolean( selected, SELECTED_OFFSET );
	}

	/**
	 * Get the ghost state of the vertex.
	 *
	 * @return true, if the vertex is ghosted.
	 */
	public boolean isGhost()
	{
		return access.getBoolean( GHOST_OFFSET );
	}

	protected void setGhost( final boolean ghost )
	{
		access.putBoolean( ghost, GHOST_OFFSET );
	}

	/**
	 * Returns the current transition state for this screen vertex.
	 *
	 * @return the transition state.
	 */
	public Transition getTransition()
	{
		return Transition.values()[ access.getByte( TRANSITION_OFFSET ) ];
	}

	protected void setTransition( final Transition t )
	{
		access.putByte( t.toByte(), TRANSITION_OFFSET );
	}

	/**
	 * Get the internal pool index of the interpolated {@link ScreenVertex} for
	 * which this {@link ScreenVertex} is the interpolation target.
	 *
	 * @return internal pool index of the interpolated {@link ScreenVertex}.
	 */
	protected int getInterpolatedScreenVertexIndex()
	{
		return access.getIndex( IP_SCREENVERTEX_INDEX_OFFSET );
	}

	protected void setInterpolatedScreenVertexIndex( final int screenVertexIndex )
	{
		access.putIndex( screenVertexIndex, IP_SCREENVERTEX_INDEX_OFFSET );
	}

	/**
	 * Returns the interpolation completion ratio of the current transition for
	 * this screen vertex.
	 *
	 * @return the interpolation completion ratio.
	 */
	public double getInterpolationCompletionRatio()
	{
		return access.getDouble( IP_RATIO_OFFSET );
	}

	/**
	 * Sets the interpolation completion ratio of the current transition for
	 * this screen vertex.
	 *
	 * @param ratio
	 *            the interpolation completion ratio.
	 */
	protected void setInterpolationCompletionRatio( final double ratio )
	{
		access.putDouble( ratio, IP_RATIO_OFFSET );
	}

	@Override
	protected void setToUninitializedState()
	{}

	/**
	 * Set all fields as in specified {@link ScreenVertex} (which is possibly
	 * from another pool).
	 * <p>
	 * ONLY USE THIS FOR {@link ScreenEntities#set(ScreenEntities)}!
	 *
	 * @param v
	 *            the vertex to clone parameters from.
	 * @return {@code this}.
	 */
	ScreenVertex cloneFrom( final ScreenVertex v )
	{
		setTrackSchemeVertexId( v.getTrackSchemeVertexId() );
		setX( v.getX() );
		setY( v.getY() );
		setVertexDist( v.getVertexDist() );
		setSelected( v.isSelected() );
		setGhost( v.isGhost() );
		setTransition( v.getTransition() );
		setInterpolatedScreenVertexIndex( v.getInterpolatedScreenVertexIndex() );
		setInterpolationCompletionRatio( v.getInterpolationCompletionRatio() );
		return this;
	}

	@Override
	public boolean equals( final Object obj )
	{
		return obj instanceof ScreenVertex &&
				access.equals( ( ( ScreenVertex ) obj ).access );
	}

	@Override
	public int hashCode()
	{
		return access.hashCode();
	}

	public static class ScreenVertexPool extends Pool< ScreenVertex, ByteMappedElement >
	{
		public ScreenVertexPool( final int initialCapacity, final RefPool< TrackSchemeVertex > trackSchemeVertexPool )
		{
			this( initialCapacity, new VertexFactory( initialCapacity, trackSchemeVertexPool ) );
		}

		private ScreenVertexPool( final int initialCapacity, final VertexFactory f )
		{
			super( initialCapacity, f );
			f.vertexPool = this;
		}

		@Override
		public ScreenVertex create( final ScreenVertex vertex )
		{
			return super.create( vertex );
		}

		public void delete( final ScreenVertex vertex )
		{
			deleteByInternalPoolIndex( vertex.getInternalPoolIndex() );
		}

		private static class VertexFactory implements PoolObject.Factory< ScreenVertex, ByteMappedElement >
		{
			private ScreenVertexPool vertexPool;

			private final RefPool< TrackSchemeVertex > trackSchemeVertexPool;

			public VertexFactory( final int initialCapacity, final RefPool< TrackSchemeVertex > trackSchemeVertexPool )
			{
				this.trackSchemeVertexPool = trackSchemeVertexPool;
			}

			@Override
			public int getSizeInBytes()
			{
				return ScreenVertex.SIZE_IN_BYTES;
			}

			@Override
			public ScreenVertex createEmptyRef()
			{
				return new ScreenVertex( vertexPool, trackSchemeVertexPool );
			}

			@Override
			public MemPool.Factory< ByteMappedElement > getMemPoolFactory()
			{
				return SingleArrayMemPool.factory( ByteMappedElementArray.factory );
			}

			@Override
			public Class< ScreenVertex > getRefClass()
			{
				return ScreenVertex.class;
			}
		};
	}

	@Override
	public String toString()
	{
		return String.format( "ScreenVertex(%d, sv=%d, \"%s\", (%.2f, %.2f), %s, isv=%d%s)",
				getInternalPoolIndex(),
				getTrackSchemeVertexId(),
				getLabel(),
				getX(),
				getY(),
				getTransition().toString(),
				getInterpolatedScreenVertexIndex(),
				isSelected() ? ", selected" : "" );
	}
}
