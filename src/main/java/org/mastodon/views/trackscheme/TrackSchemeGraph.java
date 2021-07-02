/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.views.trackscheme;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.RefPool;
import org.mastodon.adapter.RefBimap;
import org.mastodon.app.ViewGraph;
import org.mastodon.collection.IntRefMap;
import org.mastodon.collection.RefSet;
import org.mastodon.collection.ref.IntRefArrayMap;
import org.mastodon.collection.ref.RefSetImp;
import org.mastodon.graph.Edge;
import org.mastodon.graph.GraphChangeListener;
import org.mastodon.graph.GraphChangeNotifier;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.GraphListener;
import org.mastodon.graph.ListenableReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.graph.ref.AbstractEdgePool;
import org.mastodon.graph.ref.AbstractEdgePool.AbstractEdgeLayout;
import org.mastodon.graph.ref.AbstractVertexPool;
import org.mastodon.graph.ref.AbstractVertexPool.AbstractVertexLayout;
import org.mastodon.graph.ref.GraphImp;
import org.mastodon.pool.ByteMappedElement;
import org.mastodon.pool.ByteMappedElementArray;
import org.mastodon.pool.SingleArrayMemPool;
import org.mastodon.pool.attributes.BooleanAttribute;
import org.mastodon.pool.attributes.DoubleAttribute;
import org.mastodon.pool.attributes.IndexAttribute;
import org.mastodon.pool.attributes.IntAttribute;
import org.mastodon.spatial.HasTimepoint;
import org.mastodon.views.trackscheme.wrap.DefaultModelGraphProperties;
import org.mastodon.views.trackscheme.wrap.ModelGraphProperties;
import org.scijava.listeners.Listeners;

/**
 * A specialized graph to be used in the TrackScheme application.
 * <p>
 * A {@link TrackSchemeGraph} wraps a model graph, where the graph is laid out
 * in a hierarchical-temporal way.
 * <p>
 * The {@link TrackSchemeGraph} duplicates the structure of the the model graph
 * using {@link TrackSchemeVertex} and {@link TrackSchemeEdge} objects. The
 * structure is kept in sync with the model graph by registering as a
 * {@link GraphListener}.
 * <p>
 * The vertices and edges of the {@link TrackSchemeGraph} expose properties
 * related to graph {@link LineageTreeLayout layout} (such as layout
 * coordinates) and painting (such as whether the vertex is selected). Some of
 * these (layout coordinates) are stored in the {@link TrackSchemeGraph}
 * entities, while others (label, selected state) are backed by the model
 * entities.
 * <p>
 * A mapping between vertices of the model graph and {@link TrackSchemeVertex
 * vertices} of the {@link TrackSchemeGraph} is established through unique IDs
 * assigned to model vertices. For this, we require a {@link GraphIdBimap
 * bidirectional map} from model graph entities to unique IDs. The
 * {@link TrackSchemeGraph} then maintains a map from unique model IDs to
 * {@link TrackSchemeVertex} allowing to go from a model vertex to the
 * corresponding {@link TrackSchemeVertex}. Vice versa, allowing to go from a
 * {@link TrackSchemeVertex} to the corresponding model vertex, each
 * {@link TrackSchemeVertex} stores the unique ID of its corresponding model
 * vertex.
 * <p>
 * Through the model-ID bimap, {@link TrackSchemeGraph} is decoupled from the
 * model graph implementation. The model graph might be stored as pool objects
 * or plain objects, or it might be a wrapper around a graph stored in a
 * database. The only requirements on the model graph is that its vertices
 * implement {@link HasTimepoint}. Other properties (such as vertex labels or
 * selection states) are accessed through {@link ModelGraphProperties} that know
 * how to retrieve/compute them for a given model vertex/edge ID. We provide a
 * default implementation of {@link DefaultModelGraphProperties} that should be
 * applicable for almost all model graphs.
 * <p>
 * {@link TrackSchemeGraph} registers as a {@link GraphChangeListener} with the
 * model graph and forwards {@link GraphChangeListener#graphChanged()
 * graphChanged} events such that interested clients can register with the
 * {@link TrackSchemeGraph} and do not have to know the model graph.
 *
 * @param <V>
 *            the type of the vertices of the wrapped model graph.
 * @param <E>
 *            the type of the edges of the wrapped model graph.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class TrackSchemeGraph<
		V extends Vertex< E >,
		E extends Edge< V > >
	extends GraphImp<
				TrackSchemeGraph.TrackSchemeVertexPool,
				TrackSchemeGraph.TrackSchemeEdgePool,
				TrackSchemeVertex, TrackSchemeEdge, ByteMappedElement >
	implements GraphListener< V, E >, GraphChangeNotifier, GraphChangeListener, ViewGraph< V, E, TrackSchemeVertex, TrackSchemeEdge >
{
	private final ListenableReadOnlyGraph< V, E > modelGraph;

	private final ModelGraphProperties< V, E > modelGraphProperties;

	private final ReentrantReadWriteLock lock;

	private final GraphIdBimap< V, E > idmap;

	private final IntRefMap< TrackSchemeVertex > idToTrackSchemeVertex;

	private final IntRefMap< TrackSchemeEdge > idToTrackSchemeEdge;

	private final RefSet< TrackSchemeVertex > roots;

	private V mv;

	private final TrackSchemeVertex tsv;

	private final TrackSchemeVertex tsv2;

	private final TrackSchemeEdge tse;

	private final Listeners.List< GraphChangeListener > listeners;

	private final RefBimap< V, TrackSchemeVertex > vertexMap;

	private final RefBimap< E, TrackSchemeEdge > edgeMap;

	/**
	 * Creates a new TrackSchemeGraph with a default initial capacity.
	 *
	 * @param modelGraph
	 *            the model graph to wrap.
	 * @param idmap
	 *            the bidirectional id map of the model graph.
	 * @param modelGraphProperties
	 *            an accessor for properties of the model graph.
	 */
	public TrackSchemeGraph(
			final ListenableReadOnlyGraph< V, E > modelGraph,
			final GraphIdBimap< V, E > idmap,
			final ModelGraphProperties< V, E > modelGraphProperties )
	{
		this( modelGraph, idmap, modelGraphProperties, new ReentrantReadWriteLock() );
	}

	/**
	 * Creates a new TrackSchemeGraph with a default initial capacity.
	 *
	 * @param modelGraph
	 *            the model graph to wrap.
	 * @param idmap
	 *            the bidirectional id map of the model graph.
	 * @param modelGraphProperties
	 *            an accessor for properties of the model graph.
	 * @param lock
	 *            read/write locks for the model graph
	 */
	public TrackSchemeGraph(
			final ListenableReadOnlyGraph< V, E > modelGraph,
			final GraphIdBimap< V, E > idmap,
			final ModelGraphProperties< V, E > modelGraphProperties,
			final ReentrantReadWriteLock lock )
	{
		this( modelGraph, idmap, modelGraphProperties, lock, 10000 );
	}

	/**
	 * Creates a new {@link TrackSchemeGraph} that reproduces the current model
	 * graph structure. It registers as a {@link GraphListener} to keep in sync
	 * with changes the model graph.
	 *
	 * @param modelGraph
	 *            the model graph to wrap.
	 * @param idmap
	 *            the bidirectional id map of the model graph.
	 * @param modelGraphProperties
	 *            an accessor for properties of the model graph.
	 * @param lock
	 *            read/write locks for the model graph
	 * @param initialCapacity
	 *            the initial capacity for the graph storage.
	 */
	public TrackSchemeGraph(
			final ListenableReadOnlyGraph< V, E > modelGraph,
			final GraphIdBimap< V, E > idmap,
			final ModelGraphProperties< V, E > modelGraphProperties,
			final ReentrantReadWriteLock lock,
			final int initialCapacity )
	{
		super( new TrackSchemeEdgePool(
				initialCapacity,
				new TrackSchemeVertexPool(
						initialCapacity,
						new ModelGraphWrapper<>( idmap, modelGraphProperties ) ) ) );
		this.modelGraph = modelGraph;
		this.modelGraphProperties = modelGraphProperties;
		this.lock = lock;
		this.idmap = idmap;
		idToTrackSchemeVertex =	new IntRefArrayMap<>( vertexPool );
		idToTrackSchemeEdge = new IntRefArrayMap<>( edgePool );
		roots = new RefSetImp<>( vertexPool );
		mv = modelGraph.vertexRef();
		tsv = vertexRef();
		tsv2 = vertexRef();
		tse = edgeRef();
		listeners = new Listeners.SynchronizedList<>();
		vertexMap = new TrackSchemeVertexBimap<>( this );
		edgeMap = new TrackSchemeEdgeBimap<>( this );

		modelGraph.addGraphListener( this );
		modelGraph.addGraphChangeListener( this );
		lock.writeLock().lock();
		try
		{
			graphRebuilt();
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	/**
	 * Exposes the {@link RefPool} for the TrackScheme vertices of this
	 * TrackSchemeGraph.
	 *
	 * @return the vertex pool.
	 */
	public RefPool< TrackSchemeVertex > getVertexPool()
	{
		return vertexPool;
	}

	/**
	 * Exposes the {@link RefPool} for the TrackScheme edges of this
	 * TrackSchemeGraph.
	 *
	 * @return the edge pool.
	 */
	public RefPool< TrackSchemeEdge > getEdgePool()
	{
		return edgePool;
	}

	/**
	 * @return the bidirectional id map of the model graph.
	 */
	public GraphIdBimap< V, E > getGraphIdBimap()
	{
		return idmap;
	}

	/**
	 * Returns the roots of this graph.
	 * <p>
	 * Roots are defined as the vertices that have no incoming edges. Therefore
	 * a single connected-component of the graph may have several roots.
	 * <p>
	 * To be properly used in TrackScheme, it is best to adopt the convention
	 * where all edges are directed along time: They should depart from the
	 * earliest vertex in time, and point to the latest vertex in time. Then,
	 * the roots of the graph corresponds to vertices that appear as time
	 * increases.
	 *
	 * @return the roots of the graph, as a set of vertices.
	 */
	public RefSet< TrackSchemeVertex > getRoots()
	{
		return roots;
	}

	@Override
	public String toString()
	{
		final StringBuffer sb = new StringBuffer( "TrackSchemeGraph {\n" );
		sb.append( "  vertices = {\n" );
		for ( final TrackSchemeVertex vertex : vertices() )
			sb.append( "    " + vertex + "\n" );
		sb.append( "  },\n" );
		sb.append( "  edges = {\n" );
		for ( final TrackSchemeEdge edge : edges() )
			sb.append( "    " + edge + "\n" );
		sb.append( "  }\n" );
		sb.append( "},\n" );
		sb.append( "  roots = {\n" );
		for ( final TrackSchemeVertex vertex : roots )
			sb.append( "    " + vertex + "\n" );
		sb.append( "  },\n" );
		sb.append( "}" );
		return sb.toString();
	}

	/**
	 * Returns the vertex in this TrackSchemeGraph that corresponds to the model
	 * vertex with the specified id.
	 *
	 * @param modelId
	 *            the id of the vertex in the model graph.
	 * @param ref
	 *            a TrackSchemeVertex reference.
	 * @return the TrackSchemeVertex corresponding to the model vertex with the
	 *         specified id.
	 */
	TrackSchemeVertex getTrackSchemeVertexForModelId( final int modelId, final TrackSchemeVertex ref )
	{
		return idToTrackSchemeVertex.get( modelId, ref );
	}

	TrackSchemeEdge getTrackSchemeEdgeForModelId( final int modelId, final TrackSchemeEdge ref )
	{
		return idToTrackSchemeEdge.get( modelId, ref );
	}

	/**
	 * Get the list of GraphChangeListeners. This can be used to add (or
	 * remove) a GraphChangeListener that will be notified when this
	 * TrackSchemeGraph changes.
	 *
	 * @return list of GraphChangeListeners
	 */
	public Listeners< GraphChangeListener > graphChangeListeners()
	{
		return listeners;
	}

	public ReentrantReadWriteLock getLock()
	{
		return lock;
	}

	@Override
	public TrackSchemeVertex addVertex()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public TrackSchemeVertex addVertex( final TrackSchemeVertex ref )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public TrackSchemeEdge addEdge( final TrackSchemeVertex source, final TrackSchemeVertex target )
	{
		return addEdge( source, target, edgeRef() );
	}

	@Override
	public TrackSchemeEdge addEdge( final TrackSchemeVertex source, final TrackSchemeVertex target, final TrackSchemeEdge ref )
	{
		final E mref = modelGraph.edgeRef();

		final E medge = modelGraphProperties.addEdge( vertexMap.getLeft( source ), vertexMap.getLeft( target ), mref );
		modelGraphProperties.initEdge( medge );
		final TrackSchemeEdge edge = edgeMap.getRight( medge, ref );

		modelGraph.releaseRef( mref );

		return edge;
	}

	@Override
	public TrackSchemeEdge insertEdge( final TrackSchemeVertex source, final int sourceOutIndex, final TrackSchemeVertex target, final int targetInIndex )
	{
		return insertEdge( source, sourceOutIndex, target, targetInIndex, edgeRef() );
	}

	@Override
	public TrackSchemeEdge insertEdge( final TrackSchemeVertex source, final int sourceOutIndex, final TrackSchemeVertex target, final int targetInIndex, final TrackSchemeEdge ref )
	{
		final E mref = modelGraph.edgeRef();

		final E medge = modelGraphProperties.insertEdge( vertexMap.getLeft( source ), sourceOutIndex, vertexMap.getLeft( target ), targetInIndex, mref );
		modelGraphProperties.initEdge( medge );
		final TrackSchemeEdge edge = edgeMap.getRight( medge, ref );

		modelGraph.releaseRef( mref );

		return edge;
	}

	@Override
	public void remove( final TrackSchemeVertex vertex )
	{
		modelGraphProperties.removeVertex( vertexMap.getLeft( vertex ) );
	}

	@Override
	public void remove( final TrackSchemeEdge edge )
	{
		modelGraphProperties.removeEdge( edgeMap.getLeft( edge ) );
	}

	/*
	 * GraphChangeNotifier
	 */

	/**
	 * Triggers a {@link GraphChangeListener#graphChanged()} event.
	 *
	 * notifyGraphChanged() is not implicitly called in addVertex() etc because
	 * we want to support batches of add/remove with one final
	 * notifyGraphChanged() at the end.
	 */
	@Override
	public void notifyGraphChanged()
	{
		modelGraphProperties.notifyGraphChanged();
	}

	/*
	 * GraphChangeListener
	 */

	@Override
	public void graphChanged()
	{
		for ( final GraphChangeListener l : listeners.list )
			l.graphChanged();
	}

	/*
	 * GraphListener
	 */

	@Override
	public void graphRebuilt()
	{
		idToTrackSchemeVertex.clear();
		idToTrackSchemeEdge.clear();
		roots.clear();

		for ( final V v : modelGraph.vertices() )
		{
			final int id = idmap.getVertexId( v );
			super.addVertex( tsv ).initModelId( id );
			idToTrackSchemeVertex.put( id, tsv );
			if ( v.incomingEdges().isEmpty() )
				roots.add( tsv );
		}
		for ( final E e : modelGraph.edges() )
		{
			final int id = idmap.getEdgeId( e );
			idToTrackSchemeVertex.get( idmap.getVertexId( e.getSource( mv ) ), tsv );
			idToTrackSchemeVertex.get( idmap.getVertexId( e.getTarget( mv ) ), tsv2 );
			super.insertEdge( tsv, e.getSourceOutIndex(), tsv2, e.getTargetInIndex(), tse ).initModelId( id );
			idToTrackSchemeEdge.put( id, tse );
		}
	}

	@Override
	public void vertexAdded( final V vertex )
	{
		final int id = idmap.getVertexId( vertex );
		super.addVertex( tsv ).initModelId( id );
		idToTrackSchemeVertex.put( id, tsv );
		roots.add( tsv );
	}

	@Override
	public void vertexRemoved( final V vertex )
	{
		final int id = idmap.getVertexId( vertex );
		if ( idToTrackSchemeVertex.remove( id, tsv ) != null )
		{
			if ( tsv.incomingEdges().isEmpty() )
				roots.remove( tsv );
			super.remove( tsv );
		}
	}

	@Override
	public void edgeAdded( final E edge )
	{
		final int id = idmap.getEdgeId( edge );
		idToTrackSchemeVertex.get( idmap.getVertexId( edge.getSource( mv ) ), tsv );
		idToTrackSchemeVertex.get( idmap.getVertexId( edge.getTarget( mv ) ), tsv2 );
		if ( tsv2.incomingEdges().isEmpty() )
			roots.remove( tsv2 );
		super.insertEdge( tsv, edge.getSourceOutIndex(), tsv2, edge.getTargetInIndex(), tse ).initModelId( id );
		idToTrackSchemeEdge.put( id, tse );
	}

	@Override
	public void edgeRemoved( final E edge )
	{
		final int id = idmap.getEdgeId( edge );
		if ( idToTrackSchemeEdge.remove( id, tse ) != null )
		{
			if ( tse.getTarget( tsv ).incomingEdges().size() == 1 )
				roots.add( tsv );
			super.remove( tse );
		}
	}

//	@Override // TODO: should be implemented for some listener interface, or REMOVE? (vertices never change timepoint?)
	public void vertexTimepointChanged( final V vertex )
	{
		idToTrackSchemeVertex.get( idmap.getVertexId( vertex ), tsv ).updateTimepointFromModel();
	}

	/**
	 * Get bidirectional mapping between model vertices and view vertices.
	 *
	 * @return bidirectional mapping between model vertices and view vertices.
	 */
	@Override
	public RefBimap< V, TrackSchemeVertex > getVertexMap()
	{
		return vertexMap;
	}

	/**
	 * Get bidirectional mapping between model edges and view edges.
	 *
	 * @return bidirectional mapping between model edges and view edges.
	 */
	@Override
	public RefBimap< E, TrackSchemeEdge > getEdgeMap()
	{
		return edgeMap;
	}

	/*
	 * vertex and edge pools
	 */

	static class TrackSchemeVertexLayout extends AbstractVertexLayout
	{
		final IndexField origVertexIndex = indexField();
		final IntField layoutTimeStamp = intField();
		final IndexField layoutInEdgeIndex = indexField();
		final DoubleField layoutX = doubleField();
		final IntField timepoint = intField();
		final IndexField screenVertexIndex = indexField();
		final BooleanField ghost = booleanField();
	}

	static TrackSchemeVertexLayout vertexLayout = new TrackSchemeVertexLayout();

	static class TrackSchemeVertexPool extends AbstractVertexPool< TrackSchemeVertex, TrackSchemeEdge, ByteMappedElement >
	{
		final ModelGraphWrapper< ?, ? > modelGraphWrapper;

		final IndexAttribute< TrackSchemeVertex > origVertexIndex = new IndexAttribute<>( vertexLayout.origVertexIndex, this );
		final IntAttribute< TrackSchemeVertex > layoutTimeStamp = new IntAttribute<>( vertexLayout.layoutTimeStamp, this );
		final IndexAttribute< TrackSchemeVertex > layoutInEdgeIndex = new IndexAttribute<>( vertexLayout.layoutInEdgeIndex, this );
		final DoubleAttribute< TrackSchemeVertex > layoutX = new DoubleAttribute<>( vertexLayout.layoutX, this );
		final IntAttribute< TrackSchemeVertex > timepoint = new IntAttribute<>( vertexLayout.timepoint, this );
		final IndexAttribute< TrackSchemeVertex > screenVertexIndex = new IndexAttribute<>( vertexLayout.screenVertexIndex, this );
		final BooleanAttribute< TrackSchemeVertex > ghost = new BooleanAttribute<>( vertexLayout.ghost, this );

		private TrackSchemeVertexPool( final int initialCapacity, final ModelGraphWrapper< ?, ? > modelGraphWrapper )
		{
			super( initialCapacity, vertexLayout, TrackSchemeVertex.class, SingleArrayMemPool.factory( ByteMappedElementArray.factory ) );
			this.modelGraphWrapper = modelGraphWrapper;
		}

		@Override
		protected TrackSchemeVertex createEmptyRef()
		{
			return new TrackSchemeVertex( this );
		}
	}

	static class TrackSchemeEdgeLayout extends AbstractEdgeLayout
	{
		final IndexField origEdgeIndex = indexField();
		final IndexField screenEdgeIndex = indexField();
	}

	static TrackSchemeEdgeLayout edgeLayout = new TrackSchemeEdgeLayout();

	static class TrackSchemeEdgePool extends AbstractEdgePool< TrackSchemeEdge, TrackSchemeVertex, ByteMappedElement >
	{
		final ModelGraphWrapper< ?, ? > modelGraphWrapper;

		final IndexAttribute< TrackSchemeEdge > origEdgeIndex = new IndexAttribute<>( edgeLayout.origEdgeIndex, this );
		final IndexAttribute< TrackSchemeEdge > screenEdgeIndex = new IndexAttribute<>( edgeLayout.screenEdgeIndex, this );

		private TrackSchemeEdgePool( final int initialCapacity, final TrackSchemeVertexPool vertexPool )
		{
			super( initialCapacity, edgeLayout, TrackSchemeEdge.class, SingleArrayMemPool.factory( ByteMappedElementArray.factory ), vertexPool );
			modelGraphWrapper = vertexPool.modelGraphWrapper;
			vertexPool.linkEdgePool( this );
		}

		@Override
		protected TrackSchemeEdge createEmptyRef()
		{
			return new TrackSchemeEdge( this );
		}
	}
}
