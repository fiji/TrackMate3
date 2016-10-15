package org.mastodon.revised.bdv.overlay.wrap;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mastodon.RefPool;
import org.mastodon.collection.RefCollection;
import org.mastodon.collection.util.AbstractRefPoolCollectionWrapper;
import org.mastodon.graph.Edge;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.bdv.overlay.OverlayGraph;
import org.mastodon.spatial.SpatioTemporalIndex;

/**
 * TODO: implement remaining ReadOnlyGraph methods TODO: implement
 * CollectionCreator
 *
 * @param <V>
 *            the type of the model vertex wrapped.
 * @param <E>
 *            the type of the model edge wrapped.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class OverlayGraphWrapper< V extends Vertex< E >, E extends Edge< V > > implements
		OverlayGraph< OverlayVertexWrapper< V, E >, OverlayEdgeWrapper< V, E > >
{
	final ReadOnlyGraph< V, E > wrappedGraph;

	final GraphIdBimap< V, E > idmap;

	final OverlayProperties< V, E > overlayProperties;

	private final ConcurrentLinkedQueue< OverlayVertexWrapper< V, E > > tmpVertexRefs;

	private final ConcurrentLinkedQueue< OverlayEdgeWrapper< V, E > > tmpEdgeRefs;

	private final SpatioTemporalIndexWrapper< V, E > wrappedIndex;

	public OverlayGraphWrapper(
			final ReadOnlyGraph< V, E > graph,
			final GraphIdBimap< V, E > idmap,
			final SpatioTemporalIndex< V > graphIndex,
			final OverlayProperties< V, E > overlayProperties )
	{
		this.wrappedGraph = graph;
		this.idmap = idmap;
		this.overlayProperties = overlayProperties;
		tmpVertexRefs =	new ConcurrentLinkedQueue<>();
		tmpEdgeRefs = new ConcurrentLinkedQueue<>();
		this.wrappedIndex = new SpatioTemporalIndexWrapper<>( this, graphIndex );
	}

	@Override
	public OverlayVertexWrapper< V, E > vertexRef()
	{
		final OverlayVertexWrapper< V, E > ref = tmpVertexRefs.poll();
		return ref == null ? new OverlayVertexWrapper<>( this ) : ref;
	}

	@Override
	public OverlayEdgeWrapper< V, E > edgeRef()
	{
		final OverlayEdgeWrapper< V, E > ref = tmpEdgeRefs.poll();
		return ref == null ? new OverlayEdgeWrapper<>( this ) : ref;
	}

	@Override
	public void releaseRef( final OverlayVertexWrapper< V, E > ref )
	{
		tmpVertexRefs.add( ref );
	}

	@Override
	public void releaseRef( final OverlayEdgeWrapper< V, E > ref )
	{
		tmpEdgeRefs.add( ref );
	}

	@Override
	public OverlayEdgeWrapper< V, E > getEdge( final OverlayVertexWrapper< V, E > source, final OverlayVertexWrapper< V, E > target )
	{
		return getEdge( source, target, edgeRef() );
	}

	@Override
	public OverlayEdgeWrapper< V, E > getEdge( final OverlayVertexWrapper< V, E > source, final OverlayVertexWrapper< V, E > target, final OverlayEdgeWrapper< V, E > edge )
	{
		edge.we = wrappedGraph.getEdge( source.wv, target.wv, edge.ref );
		return edge.orNull();
	}

	@Override
	public RefCollection< OverlayVertexWrapper< V, E > > vertices()
	{
		return vertices;
	}

	@Override
	public RefCollection< OverlayEdgeWrapper< V, E > > edges()
	{
		return edges;
	}

	@Override
	public SpatioTemporalIndex< OverlayVertexWrapper< V, E > > getIndex()
	{
		return wrappedIndex;
	}

	@Override
	public double getMaxBoundingSphereRadiusSquared( final int timepoint )
	{
		return overlayProperties.getMaxBoundingSphereRadiusSquared( timepoint );
	}

	@Override
	public OverlayVertexWrapper< V, E > addVertex( final int timepoint, final double[] position, final double radius, final OverlayVertexWrapper< V, E > ref )
	{
		ref.wv = overlayProperties.addVertex( timepoint, position, radius, ref.ref );
		return ref;
	}

	@Override
	public OverlayVertexWrapper< V, E > addVertex( final int timepoint, final double[] position, final double[][] covariance, final OverlayVertexWrapper< V, E > ref )
	{
		ref.wv = overlayProperties.addVertex( timepoint, position, covariance, ref.ref );
		return ref;
	}

	@Override
	public OverlayEdgeWrapper< V, E > addEdge( final OverlayVertexWrapper< V, E > source, final OverlayVertexWrapper< V, E > target, final OverlayEdgeWrapper< V, E > ref )
	{
		ref.we = overlayProperties.addEdge( source.wv, target.wv, ref.ref );
		return ref;
	}

	@Override
	public void remove( final OverlayEdgeWrapper< V, E > edge )
	{
		overlayProperties.removeEdge( edge.we );
	}

	@Override
	public void remove( final OverlayVertexWrapper< V, E > vertex )
	{
		overlayProperties.removeVertex( vertex.wv );
	}

	@Override
	public void notifyGraphChanged()
	{
		overlayProperties.notifyGraphChanged();
	}

	private final RefPool< OverlayVertexWrapper< V, E > > vertexPool = new RefPool< OverlayVertexWrapper< V, E > >()
	{
		@Override
		public OverlayVertexWrapper< V, E > createRef()
		{
			return vertexRef();
		}

		@Override
		public void releaseRef( final OverlayVertexWrapper< V, E > v )
		{
			releaseRef( v );
		}

		@Override
		public OverlayVertexWrapper< V, E > getObject( final int index, final OverlayVertexWrapper< V, E > v )
		{
			v.wv = idmap.getVertex( index, v.ref );
			return v;
		}


		@Override
		public int getId( final OverlayVertexWrapper< V, E > v )
		{
			return idmap.getVertexId( v.wv );
		}

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public Class< OverlayVertexWrapper< V, E > > getRefClass()
		{
			return ( Class ) OverlayVertexWrapper.class;
		}
	};

	private final AbstractRefPoolCollectionWrapper< OverlayVertexWrapper< V, E >, RefPool< OverlayVertexWrapper< V, E > > > vertices = new AbstractRefPoolCollectionWrapper< OverlayVertexWrapper< V, E >, RefPool< OverlayVertexWrapper< V, E > > >( vertexPool )
	{
		@Override
		public int size()
		{
			return wrappedGraph.vertices().size();
		}

		@Override
		public Iterator< OverlayVertexWrapper< V, E > > iterator()
		{
			return new OverlayVertexIteratorWrapper<>( OverlayGraphWrapper.this, OverlayGraphWrapper.this.vertexRef(), wrappedGraph.vertices().iterator() );
		}
	};

	private final RefPool< OverlayEdgeWrapper< V, E > > edgePool = new RefPool< OverlayEdgeWrapper< V, E > >()
	{
		@Override
		public OverlayEdgeWrapper< V, E > createRef()
		{
			return edgeRef();
		}

		@Override
		public void releaseRef( final OverlayEdgeWrapper< V, E > e )
		{
			releaseRef( e );
		}

		@Override
		public OverlayEdgeWrapper< V, E > getObject( final int index, final OverlayEdgeWrapper< V, E > e )
		{
			e.we = idmap.getEdge( index, e.ref );
			return e;
		}

		@Override
		public int getId( final OverlayEdgeWrapper< V, E > e )
		{
			return idmap.getEdgeId( e.we );
		}

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public Class< OverlayEdgeWrapper< V, E > > getRefClass()
		{
			return ( Class ) OverlayEdgeWrapper.class;
		}
	};

	private final AbstractRefPoolCollectionWrapper< OverlayEdgeWrapper< V, E >, RefPool< OverlayEdgeWrapper< V, E > > > edges = new AbstractRefPoolCollectionWrapper< OverlayEdgeWrapper< V, E >, RefPool< OverlayEdgeWrapper< V, E > > >( edgePool )
	{
		@Override
		public int size()
		{
			return wrappedGraph.edges().size();
		}

		@Override
		public Iterator< OverlayEdgeWrapper< V, E > > iterator()
		{
			return new OverlayEdgeIteratorWrapper<>( OverlayGraphWrapper.this, OverlayGraphWrapper.this.edgeRef(), wrappedGraph.edges().iterator() );
		}
	};

}
