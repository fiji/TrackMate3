package net.trackmate.graph;

import net.trackmate.graph.mempool.MappedElement;

public class AbstractVertexPool<
			V extends AbstractVertex< V, E, T >,
			E extends AbstractEdge< E, ?, ? >,
			T extends MappedElement >
		extends Pool< V, T >
{
	private AbstractEdgePool< E, ?, ? > edgePool;

	public AbstractVertexPool(
			final int initialCapacity,
			final PoolObject.Factory< V, T > vertexFactory )
	{
		super( initialCapacity, vertexFactory );
	}

	public void linkEdgePool( final AbstractEdgePool< E, ?, ? > edgePool )
	{
		this.edgePool = edgePool;
	}

	@Override
	public V createRef()
	{
		final V vertex = super.createRef();
		if ( edgePool != null )
			vertex.linkEdgePool( edgePool );
		return vertex;
	}

	public V create()
	{
		return create( createRef() );
	}

	@Override
	public V create( final V vertex )
	{
		return super.create( vertex );
	}

	public void release( final V vertex )
	{
		if ( edgePool != null )
			edgePool.releaseAllLinkedEdges( vertex );
		releaseByInternalPoolIndex( vertex.getInternalPoolIndex() );
	}
}
