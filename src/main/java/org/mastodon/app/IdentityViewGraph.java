package org.mastodon.app;

import org.mastodon.RefPool;
import org.mastodon.adapter.RefBimap;
import org.mastodon.collection.RefCollection;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Edges;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;

/**
 * A {@link ViewGraph} that simply exposes the graph it wraps.
 * 
 * @author Jean-Yves Tinevez
 *
 * @param <V>
 *            the type of vertices in the view and wrapped graphs.
 * @param <E>
 *            the type of edges in the view and wrapped graphs.
 */
public class IdentityViewGraph< V extends Vertex< E >, E extends Edge< V > > implements ViewGraph< V, E, V, E >
{

	private final ReadOnlyGraph< V, E > wrappedGraph;

	private final IdentityRefBimap< V > vertexIdBimap;

	private final IdentityRefBimap< E > edgeIdBimap;

	private IdentityViewGraph( final ReadOnlyGraph< V, E > graph, final GraphIdBimap< V, E > idBimap )
	{
		this.wrappedGraph = graph;
		this.vertexIdBimap = new IdentityRefBimap<>( idBimap.vertexIdBimap() );
		this.edgeIdBimap = new IdentityRefBimap<>( idBimap.edgeIdBimap() );
	}

	@Override
	public E getEdge( final V source, final V target )
	{
		return wrappedGraph.getEdge( source, target );
	}

	@Override
	public E getEdge( final V source, final V target, final E ref )
	{
		return wrappedGraph.getEdge( source, target, ref );
	}

	@Override
	public Edges< E > getEdges( final V source, final V target )
	{
		return wrappedGraph.getEdges( source, target );
	}

	@Override
	public Edges< E > getEdges( final V source, final V target, final V ref )
	{
		return wrappedGraph.getEdges( source, target, ref );
	}

	@Override
	public V vertexRef()
	{
		return wrappedGraph.vertexRef();
	}

	@Override
	public E edgeRef()
	{
		return wrappedGraph.edgeRef();
	}

	@Override
	public void releaseRef( final V ref )
	{
		wrappedGraph.releaseRef( ref );
	}

	@Override
	public void releaseRef( final E ref )
	{
		wrappedGraph.releaseRef( ref );
	}

	@Override
	public RefCollection< V > vertices()
	{
		return wrappedGraph.vertices();
	}

	@Override
	public RefCollection< E > edges()
	{
		return wrappedGraph.edges();
	}

	@Override
	public RefBimap< V, V > getVertexMap()
	{
		return vertexIdBimap;
	}

	@Override
	public RefBimap< E, E > getEdgeMap()
	{
		return edgeIdBimap;
	}

	private static final class IdentityRefBimap< O > implements RefBimap< O, O >
	{

		private final RefPool< O > pool;

		public IdentityRefBimap( final RefPool< O > pool )
		{
			this.pool = pool;
		}

		@Override
		public O getLeft( final O right )
		{
			return right;
		}

		@Override
		public O getRight( final O left, final O ref )
		{
			return left;
		}

		@Override
		public O reusableLeftRef( final O ref )
		{
			return ref;
		}

		@Override
		public O reusableRightRef()
		{
			return pool.createRef();
		}

		@Override
		public void releaseRef( final O ref )
		{
			pool.releaseRef( ref );
		}
	}

	/**
	 * Wraps the specified graph in a {@link ViewGraph} that plainly exposes it.
	 * 
	 * @param graph
	 *            the graph to wrap.
	 * @param idBimap
	 *            a {@link GraphIdBimap} for the wrapped graph.
	 * @return
	 */
	public static final < V extends Vertex< E >, E extends Edge< V > > ViewGraph< V, E, V, E > wrap( final ReadOnlyGraph< V, E > graph, final GraphIdBimap< V, E > idBimap )
	{
		return new IdentityViewGraph<>( graph, idBimap );
	}
}
