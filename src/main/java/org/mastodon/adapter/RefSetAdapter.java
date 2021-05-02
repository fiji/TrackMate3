package org.mastodon.adapter;

import java.util.Collection;
import java.util.Iterator;

import org.mastodon.collection.RefSet;

/**
 * Adapts a {@code RefSet<O>} as a {@code RefSet<WO>}. The mapping between
 * source elements {@code O} and wrapped elements {@code WO} is established by a
 * {@code RefBimap<O, WO>}.
 *
 * @param <O>
 *            element type of source collection being wrapped.
 * @param <WO>
 *            element type of this wrapper collection.
 *
 * @author Tobias Pietzsch
 */
public class RefSetAdapter< O, WO >
		implements RefSet< WO >
{
	private final RefSet< O > set;

	private final RefBimap< O, WO > map;

	public RefSetAdapter(
			final RefSet< O > set,
			final RefBimap< O, WO > map )
	{
		this.set = set;
		this.map = map;
	}

	@Override
	public WO createRef()
	{
		return map.reusableRightRef();
	}

	@Override
	public void releaseRef( final WO obj )
	{
		map.releaseRef( obj );
	}

	@Override
	public int size()
	{
		return set.size();
	}

	@Override
	public boolean isEmpty()
	{
		return set.isEmpty();
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean contains( final Object o )
	{
		return set.contains( map.getLeft( ( WO ) o ) );
	}

	@Override
	public Iterator< WO > iterator()
	{
		return new Iterator< WO >()
		{
			private final Iterator< O > iterator = set.iterator();

			private final WO ref = createRef();

			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public WO next()
			{
				return map.getRight( iterator.next(), ref );
			}
		};
	}

	@Override
	public Object[] toArray()
	{
		throw new UnsupportedOperationException( "not implemented (yet)" );
	}

	@Override
	public < T > T[] toArray( final T[] a )
	{
		throw new UnsupportedOperationException( "not implemented (yet)" );
	}

	@Override
	public boolean add( final WO e )
	{
		return set.add( map.getLeft( e ) );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean remove( final Object o )
	{
		return set.remove( map.getLeft( ( WO ) o ) );
	}

	@Override
	public boolean containsAll( final Collection< ? > c )
	{
		throw new UnsupportedOperationException( "not implemented (yet)" );
	}

	@Override
	public boolean addAll( final Collection< ? extends WO > c )
	{
		throw new UnsupportedOperationException( "not implemented (yet)" );
	}

	@Override
	public boolean removeAll( final Collection< ? > c )
	{
		throw new UnsupportedOperationException( "not implemented (yet)" );
	}

	@Override
	public boolean retainAll( final Collection< ? > c )
	{
		throw new UnsupportedOperationException( "not implemented (yet)" );
	}

	@Override
	public void clear()
	{
		set.clear();
	}
}
