package org.mastodon.views.bdv.overlay;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.graph.Edge;
import org.mastodon.graph.ReadOnlyGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.graph.algorithm.AbstractGraphAlgorithm;
import org.mastodon.graph.algorithm.util.Graphs;
import org.mastodon.model.FocusListener;
import org.mastodon.model.FocusModel;
import org.mastodon.model.SelectionListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.util.Listeners;

/**
 * Manages several visibility modes for the vertices and edges of a graph.
 * <p>
 * Each visibility mode is identified by the enum {@link VisibilityMode}. The
 * current visibility mode stored in an instance of this class can be accessed
 * with {@link #getMode()} and cycled with {@link #nextMode()}.
 * <p>
 * {@link Visibilities} can return useful implementation of the
 * {@link Visibility} interface, that can state whether a vertex or an edge is
 * visible. The {@link Visibility} instance behavior returned by
 * {@link #getVisibility()} matches the visibility mode.
 * <p>
 * Listeners implementing the {@link VisibilityListener} interface can register
 * to an instance of this class, to be notified when the visibility of the
 * current mode changes. This might happens for instance
 * <ul>
 * <li>when the visibility mode changes;</li>
 * <li>when the selection changes in the {@link VisibilityMode#SELECTION} mode;
 * <li>when the focus changes in the
 * {@link VisibilityMode#TRACK_OF_FOCUSED_VERTEX} mode.
 * </ul>
 *
 * @author Jean-Yves Tinevez
 * @param <V>
 *            the type of model vertex.
 * @param <E>
 *            the type of model edge.
 */
public class Visibilities< V extends Vertex< E >, E extends Edge< V > >
{

	private final ReadOnlyGraph< V, E > graph;

	private final SelectionModel< V, E > selectionModel;

	private final FocusModel< V, E > focusModel;

	private VisibilityMode currentMode;

	private Visibility< V, E > currentVisibility;

	private final ReentrantReadWriteLock lock;

	private final Listeners.List< VisibilityListener > visibilityListeners;

	/**
	 * Used to keep track of the listener we added to the
	 * {@link SelectionModel}, to deregister it if we have.
	 */
	private SelectionListener previousSelectionListener;

	/**
	 * Used to keep track of the listener we added to the {@link FocusModel}, to
	 * deregister it if we have.
	 */
	private FocusListener previousFocusListener;

	/**
	 * Creates a new visibilities instance.
	 *
	 * @param graph
	 *            the graph of the objects we want to control visibility of.
	 * @param selectionModel
	 *            a selection model built on the specified graph, to be used on
	 *            the {@link VisibilityMode#SELECTION} mode.
	 * @param focusModel
	 *            a focus model built on the specified graph, to be used on the
	 *            {@link VisibilityMode#TRACK_OF_FOCUSED_VERTEX} mode.
	 * @param lock
	 *            the lock on the specified graph, used to read-lock the graph
	 *            when building the {@link Visibility} instances.
	 */
	public Visibilities(
			final ReadOnlyGraph< V, E > graph,
			final SelectionModel< V, E > selectionModel,
			final FocusModel< V, E > focusModel,
			final ReentrantReadWriteLock lock )
	{
		this.graph = graph;
		this.selectionModel = selectionModel;
		this.focusModel = focusModel;
		this.lock = lock;
		this.visibilityListeners = new Listeners.SynchronizedList<>();
		setMode( VisibilityMode.ALL );
	}

	/**
	 * Cycles to the next visibility mode.
	 *
	 * @return the new visibility mode.
	 */
	public VisibilityMode nextMode()
	{
		final List< VisibilityMode > modes = Arrays.asList( VisibilityMode.values() );
		int indexOf = modes.indexOf( currentMode );
		if ( indexOf == modes.size() - 1 )
			indexOf = -1;

		setMode( modes.get( indexOf + 1 ) );
		return currentMode;
	}

	/**
	 * Sets the current visibility mode.
	 *
	 * @param mode
	 *            the visibility mode.
	 */
	public void setMode( final VisibilityMode mode )
	{
		currentMode = mode;
		currentVisibility = createVisibility();
	}

	/**
	 * Returns the current visibility mode.
	 *
	 * @return the visibility mode.
	 */
	public VisibilityMode getMode()
	{
		return currentMode;
	}

	/**
	 * Returns the visibility instance corresponding to the current visibility
	 * mode.
	 *
	 * @return the visibility object.
	 */
	public Visibility< V, E > getVisibility()
	{
		return currentVisibility;
	}

	private Visibility< V, E > createVisibility()
	{
		// Deregister previous focus listener.
		if ( null != previousFocusListener )
		{
			focusModel.listeners().remove( previousFocusListener );
			previousFocusListener = null;
		}
		// Deregister previous selection listener.
		if ( null != previousSelectionListener )
		{
			selectionModel.listeners().remove( previousSelectionListener );
			previousSelectionListener = null;
		}

		switch ( currentMode )
		{
		case ALL:
			notifyListeners();
			return new AllVisibility<>();
		case NONE:
			notifyListeners();
			return new NoneVisibility<>();
		case SELECTION:
			final SelectionVisibility selectionVisibility = new SelectionVisibility( selectionModel );
			selectionVisibility.selectionChanged();
			selectionModel.listeners().add( selectionVisibility );
			previousSelectionListener = selectionVisibility;
			return selectionVisibility;
		case TRACK_OF_FOCUSED_VERTEX:
			final TrackOfFocusedVisibility trackOfFocusedVisibility = new TrackOfFocusedVisibility( graph, focusModel, lock );
			trackOfFocusedVisibility.focusChanged();
			focusModel.listeners().add( trackOfFocusedVisibility );
			previousFocusListener = trackOfFocusedVisibility;
			return trackOfFocusedVisibility;
		default:
			throw new IllegalArgumentException( "Unknwon VisibilityMode: " + currentMode );
		}
	}

	private void notifyListeners()
	{
		for ( final VisibilityListener l : visibilityListeners.list )
			l.visibilityChanged();
	}

	/**
	 * Exposes the visibility listeners that will be notified when the
	 * visibility changes. Notification will be triggered for instance:
	 * <ul>
	 * <li>when the visibility mode changes;</li>
	 * <li>when the selection changes in the {@link VisibilityMode#SELECTION}
	 * mode;
	 * <li>when the focus changes in the
	 * {@link VisibilityMode#TRACK_OF_FOCUSED_VERTEX} mode.
	 * </ul>
	 *
	 * @return
	 */
	public Listeners< VisibilityListener > getVisibilityListeners()
	{
		return visibilityListeners;
	}

	/**
	 * Interface for listeners that will be notified when visibility changes.
	 */
	public static interface VisibilityListener
	{
		/**
		 * Called when the visibility changes.
		 */
		public void visibilityChanged();
	}

	/**
	 * Class that can determine whether the objects of a graph are visible or
	 * not.
	 *
	 * @param <V>
	 *            the type of model vertex.
	 * @param <E>
	 *            the type of model edge.
	 */
	public static interface Visibility< V extends Vertex< E >, E extends Edge< V > >
	{
		/**
		 * Returns whether the specified vertex is visible.
		 *
		 * @param v
		 *            the vertex.
		 * @return <code>true</code> if the specified vertex is visible.
		 */
		public boolean isVisible( V v );

		/**
		 * Returns whether the specified edge is visible.
		 *
		 * @param e
		 *            the edge.
		 * @return <code>true</code> if the specified edge is visible.
		 */
		public boolean isVisible( E e );
	}

	private class TrackOfFocusedVisibility implements Visibility< V, E >, FocusListener
	{

		private final FocusModel< V, E > focusModel;

		private final ReadOnlyGraph< V, E > graph;

		private final RefSet< V > vertices;

		private final RefSet< E > edges;

		private final ReentrantReadWriteLock lock;

		public TrackOfFocusedVisibility( final ReadOnlyGraph< V, E > graph, final FocusModel< V, E > focusModel, final ReentrantReadWriteLock lock )
		{
			this.graph = graph;
			this.focusModel = focusModel;
			this.lock = lock;
			this.vertices = RefCollections.createRefSet( graph.vertices() );
			this.edges = RefCollections.createRefSet( graph.edges() );
		}

		@Override
		public boolean isVisible( final V v )
		{
			return vertices.contains( v );
		}

		@Override
		public boolean isVisible( final E e )
		{
			return edges.contains( e );
		}

		@Override
		public void focusChanged()
		{
			vertices.clear();
			edges.clear();
			lock.readLock().lock();
			try
			{
				final V focused = focusModel.getFocusedVertex( graph.vertexRef() );
				if ( null != focused )
				{
					final ConnectedComponent< V, E > visitor = new ConnectedComponent<>( graph, vertices, edges );
					visitor.visit( focused );
				}
				notifyListeners();
			}
			finally
			{
				lock.readLock().unlock();
			}
		}

	}

	private static class ConnectedComponent< V extends Vertex< E >, E extends Edge< V > > extends AbstractGraphAlgorithm< V, E >
	{

		private final RefSet< V > vertices;

		private final RefSet< E > edges;

		public ConnectedComponent( final ReadOnlyGraph< V, E > graph, final RefSet< V > vertices, final RefSet< E > edges )
		{
			super( graph );
			this.vertices = vertices;
			this.edges = edges;
		}

		public void visit( final V v )
		{
			vertices.add( v );
			for ( final E e : v.edges() )
			{
				edges.add( e );
				final V target = Graphs.getOppositeVertex( e, v, graph.vertexRef() );
				if ( vertices.contains( target ) )
					continue;

				visit( target );
			}
		}
	}

	private class SelectionVisibility implements Visibility< V, E >, SelectionListener
	{

		private final SelectionModel< V, E > selectionModel;

		public SelectionVisibility( final SelectionModel< V, E > selectionModel )
		{
			this.selectionModel = selectionModel;
		}

		@Override
		public boolean isVisible( final V v )
		{
			return selectionModel.isSelected( v );
		}

		@Override
		public boolean isVisible( final E e )
		{
			return selectionModel.isSelected( e );
		}

		@Override
		public void selectionChanged()
		{
			notifyListeners();
		}
	}

	private static class NoneVisibility< V extends Vertex< E >, E extends Edge< V > > implements Visibility< V, E >
	{

		@Override
		public boolean isVisible( final V v )
		{
			return false;
		}

		@Override
		public boolean isVisible( final E e )
		{
			return false;
		}

	}

	private static class AllVisibility< V extends Vertex< E >, E extends Edge< V > > implements Visibility< V, E >
	{

		@Override
		public boolean isVisible( final V v )
		{
			return true;
		}

		@Override
		public boolean isVisible( final E e )
		{
			return true;
		}

	}

	/**
	 * Enum specifying visibility modes.
	 */
	public enum VisibilityMode
	{

		/**
		 * All the graph objects are visible.
		 */
		ALL( "All" ),
		/**
		 * Only the vertices and edges of the track (connected component) of the
		 * currently focused vertex are visible.
		 */
		TRACK_OF_FOCUSED_VERTEX( "Track of focused vertex" ),
		/**
		 * Only the content of the current selection is visible.
		 */
		SELECTION( "Current selection" ),
		/**
		 * Nothing is visible.
		 */
		NONE( "No overlay" );

		private final String name;

		private VisibilityMode( final String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
