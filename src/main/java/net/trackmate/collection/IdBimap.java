package net.trackmate.collection;

import net.trackmate.RefPool;
import net.trackmate.pool.Pool;
import net.trackmate.pool.PoolObject;

/**
 * Bidirectional mapping between integer IDs and objects.
 * <p>
 * Implementations:
 * <ul>
 * <li>A mapping between {@link PoolObject}s and their internal pool
 * indices. Implemented in {@link Pool}.</li>
 * <li>a mapping between Java objects and IDs that are assigned upon first
 * access. Not implemented yet.</li>
 * </ul>
 *
 * @param <O>
 *            the object type.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface IdBimap< O > extends RefPool< O >
{
	public int getId( O o );

//	@Override
//	public O getObject( int id, O ref );
//
//	@Override
//	public O createRef();
//
//	@Override
//	public void releaseRef( final O ref );
}