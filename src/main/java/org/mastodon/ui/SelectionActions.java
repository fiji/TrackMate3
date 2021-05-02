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
package org.mastodon.ui;

import java.awt.event.ActionEvent;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.SwingUtilities;

import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Graph;
import org.mastodon.graph.GraphChangeNotifier;
import org.mastodon.graph.Vertex;
import org.mastodon.graph.algorithm.traversal.DepthFirstSearch;
import org.mastodon.graph.algorithm.traversal.GraphSearch.SearchDirection;
import org.mastodon.graph.algorithm.traversal.SearchListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.undo.UndoPointMarker;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

/**
 * User-interface actions that are related to a model selection.
 *
 * @param <V>
 *            vertex type
 * @param <E>
 *            edge type
 *
 * @author Jean-Yves Tinevez
 * @author Tobias Pietzsch
 */
public class SelectionActions< V extends Vertex< E >, E extends Edge< V > >
{

	public static final String DELETE_SELECTION = "delete selection";
	public static final String SELECT_WHOLE_TRACK = "select whole track";
	public static final String SELECT_TRACK_DOWNWARD = "select track downward";
	public static final String SELECT_TRACK_UPWARD = "select track upward";
	public static final String SELECT_ALL = "select all";
	public static final String SELECT_ALL_EDGES = "select all links";
	public static final String SELECT_ALL_VERTICES = "select all spots";

	public static final String[] DELETE_SELECTION_KEYS = new String[] { "shift DELETE" };
	public static final String[] SELECT_WHOLE_TRACK_KEYS = new String[] { "shift SPACE" };
	public static final String[] SELECT_TRACK_DOWNWARD_KEYS = new String[] { "shift PAGE_DOWN" };
	public static final String[] SELECT_TRACK_UPWARD_KEYS = new String[] { "shift PAGE_UP" };
	public static final String[] SELECT_ALL_KEYS = new String[] { "ctrl A", "meta A" };
	public static final String[] SELECT_ALL_EDGES_KEYS = new String[] { "ctrl shift A", "meta shift A" };
	public static final String[] SELECT_ALL_VERTICES_KEYS = new String[] { "ctrl alt  A", "meta alt A" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( DELETE_SELECTION, DELETE_SELECTION_KEYS, "Delete current selection." );
			descriptions.add( SELECT_WHOLE_TRACK, SELECT_WHOLE_TRACK_KEYS, "Select the whole track of the current spot." );
			descriptions.add( SELECT_TRACK_DOWNWARD, SELECT_TRACK_DOWNWARD_KEYS, "Select the track downward from the current spot." );
			descriptions.add( SELECT_TRACK_UPWARD, SELECT_TRACK_UPWARD_KEYS, "Select the track upward form the current spot." );
			descriptions.add( SELECT_ALL, SELECT_ALL_KEYS, "Select all spots and links." );
			descriptions.add( SELECT_ALL_VERTICES, SELECT_ALL_VERTICES_KEYS, "Select all spots." );
			descriptions.add( SELECT_ALL_EDGES, SELECT_ALL_EDGES_KEYS, "Select all links." );
		}
	}

	/**
	 * Create selection actions and install them in the specified
	 * {@link Actions}.
	 *
	 * @param <V>
	 *            the type of vertices in the graph.
	 * @param <E>
	 *            the type of edges in the graph.
	 *
	 * @param actions
	 *            Actions are added here.
	 * @param graph
	 *            the graph to define actions for.
	 * @param lock
	 *            a reentrant read/write lock to prevent concurrent modification
	 *            of the graph.
	 * @param notify
	 *            is notified when the graph is changed by these actions.
	 * @param selection
	 *            the selection model.
	 * @param undo
	 *            used to set undo points after a batch of graph changes completes.
	 */
	public static < V extends Vertex< E >, E extends Edge< V > > void install(
			final Actions actions,
			final Graph< V, E > graph,
			final ReentrantReadWriteLock lock,
			final GraphChangeNotifier notify,
			final SelectionModel< V, E > selection,
			final UndoPointMarker undo )
	{
		final SelectionActions< V, E > sa = new SelectionActions<>( graph, lock, notify, selection, undo );
		actions.namedAction( sa.deleteSelectionAction, DELETE_SELECTION_KEYS );
		actions.namedAction( sa.selectWholeTrackAction, SELECT_WHOLE_TRACK_KEYS );
		actions.namedAction( sa.selectTrackDownwardAction, SELECT_TRACK_DOWNWARD_KEYS );
		actions.namedAction( sa.selectTrackUpwardAction, SELECT_TRACK_UPWARD_KEYS );
		actions.namedAction( sa.selectAllAction, SELECT_ALL_KEYS );
		actions.namedAction( sa.selectAllVerticesAction, SELECT_ALL_VERTICES_KEYS );
		actions.namedAction( sa.selectAllEdgesAction, SELECT_ALL_EDGES_KEYS );
	}

	private final Graph< V, E > graph;

	private final ReentrantReadWriteLock lock;

	private final GraphChangeNotifier notify;

	private final SelectionModel< V, E > selection;

	private final UndoPointMarker undo;

	private final DeleteSelectionAction deleteSelectionAction;

	private final TrackSelectionAction selectWholeTrackAction;

	private final TrackSelectionAction selectTrackDownwardAction;

	private final TrackSelectionAction selectTrackUpwardAction;

	private final SelectAllAction selectAllAction;

	private final SelectAllAction selectAllVerticesAction;

	private final SelectAllAction selectAllEdgesAction;

	private SelectionActions(
			final Graph< V, E > graph,
			final ReentrantReadWriteLock lock,
			final GraphChangeNotifier notify,
			final SelectionModel< V, E > selection,
			final UndoPointMarker undo )
	{
		this.graph = graph;
		this.lock = lock;
		this.notify = notify;
		this.selection = selection;
		this.undo = undo;
		deleteSelectionAction = new DeleteSelectionAction( DELETE_SELECTION );
		selectWholeTrackAction = new TrackSelectionAction( SELECT_WHOLE_TRACK, SearchDirection.UNDIRECTED );
		selectTrackDownwardAction = new TrackSelectionAction( SELECT_TRACK_DOWNWARD, SearchDirection.DIRECTED );
		selectTrackUpwardAction = new TrackSelectionAction( SELECT_TRACK_UPWARD, SearchDirection.REVERSED );
		selectAllAction = new SelectAllAction( SELECT_ALL, true, true);
		selectAllVerticesAction = new SelectAllAction( SELECT_ALL_VERTICES, true, false);
		selectAllEdgesAction = new SelectAllAction( SELECT_ALL_EDGES, false, true);
	}

	class DeleteSelectionAction	extends AbstractNamedAction
	{
		private static final long serialVersionUID = 1L;

		DeleteSelectionAction( final String name )
		{
			super( name );
			SwingUtilities.invokeLater( () -> setEnabled( !selection.isEmpty() ) );
			selection.listeners().add( () -> SwingUtilities.invokeLater(
					() -> setEnabled( !selection.isEmpty() ) ) );
		}

		@Override
		public void actionPerformed( final ActionEvent event )
		{
			if ( selection.isEmpty() )
				return;

			lock.writeLock().lock();
			try
			{
				final RefSet< E > edges = selection.getSelectedEdges();
				final RefSet< V > vertices = selection.getSelectedVertices();

				selection.pauseListeners();

				for ( final E e : edges )
					graph.remove( e );

				for ( final V v : vertices )
					graph.remove( v );

				undo.setUndoPoint();
				notify.notifyGraphChanged();
				selection.resumeListeners();
			}
			finally
			{
				lock.writeLock().unlock();
			}
		}
	}

	class TrackSelectionAction extends AbstractNamedAction
	{
		private static final long serialVersionUID = 1L;

		private final SearchDirection directivity;

		TrackSelectionAction( final String name, final SearchDirection directivity )
		{
			super( name );
			this.directivity = directivity;
			SwingUtilities.invokeLater( () -> setEnabled( !selection.isEmpty() ) );
			selection.listeners().add( () -> SwingUtilities.invokeLater(
					() -> setEnabled( !selection.isEmpty() ) ) );
		}

		@Override
		public void actionPerformed( final ActionEvent event )
		{
			if ( selection.isEmpty() )
				return;

			lock.writeLock().lock();
			try
			{
				selection.pauseListeners();

				final RefSet< V > vertices = RefCollections.createRefSet( graph.vertices() );
				vertices.addAll( selection.getSelectedVertices() );
				final V ref = graph.vertexRef();
				for ( final E e : selection.getSelectedEdges() )
				{
					vertices.add( e.getSource( ref ) );
					vertices.add( e.getTarget( ref ) );
				}
				graph.releaseRef( ref );

				selection.clearSelection();

				// Prepare the iterator.
				final DepthFirstSearch< V, E > search = new DepthFirstSearch<>( graph, directivity );
				search.setTraversalListener( new SearchListener< V, E, DepthFirstSearch< V, E > >()
				{
					@Override
					public void processVertexLate( final V vertex, final DepthFirstSearch< V, E > search )
					{}

					@Override
					public void processVertexEarly( final V vertex, final DepthFirstSearch< V, E > search )
					{
						selection.setSelected( vertex, true );
					}

					@Override
					public void processEdge( final E edge, final V from, final V to, final DepthFirstSearch< V, E > search )
					{
						selection.setSelected( edge, true );
					}

					@Override
					public void crossComponent( final V from, final V to, final DepthFirstSearch< V, E > search )
					{}
				} );

				// Iterate from all vertices that were in the selection.
				for ( final V v : vertices )
					if ( !selection.isSelected( v ) )
						search.start( v );

				selection.resumeListeners();
			}
			finally
			{
				lock.writeLock().unlock();
			}
		}
	}

	class SelectAllAction extends AbstractNamedAction
	{

		private static final long serialVersionUID = 1L;

		private final boolean selectVertices;

		private final boolean selectEdges;

		public SelectAllAction( final String name, final boolean selectVertices, final boolean selectEdges )
		{
			super( name );
			this.selectVertices = selectVertices;
			this.selectEdges = selectEdges;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			lock.readLock().lock();
			try
			{
				selection.pauseListeners();
				selection.clearSelection();
				if (selectVertices)
					selection.setVerticesSelected( graph.vertices(), true );
				if (selectEdges)
					selection.setEdgesSelected( graph.edges(), true );

			}
			finally
			{
				selection.resumeListeners();
				lock.readLock().unlock();
			}
		}
	}
}
