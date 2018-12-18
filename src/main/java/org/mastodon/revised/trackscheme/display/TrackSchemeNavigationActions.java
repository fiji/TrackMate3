package org.mastodon.revised.trackscheme.display;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.model.FocusModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.revised.mamut.KeyConfigContexts;
import org.mastodon.revised.trackscheme.LineageTreeLayout;
import org.mastodon.revised.trackscheme.TrackSchemeEdge;
import org.mastodon.revised.trackscheme.TrackSchemeGraph;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.ui.keymap.CommandDescriptionProvider;
import org.mastodon.revised.ui.keymap.CommandDescriptions;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;

/**
 * Keyboard navigation actions in TrackScheme. Supports two flavours:
 * {@link NavigatorEtiquette#FINDER_LIKE} and
 * {@link NavigatorEtiquette#MIDNIGHT_COMMANDER_LIKE}.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class TrackSchemeNavigationActions
{
	public static final String NAVIGATE_CHILD = "ts navigate to child";
	public static final String NAVIGATE_PARENT = "ts navigate to parent";
	public static final String NAVIGATE_LEFT = "ts navigate left";
	public static final String NAVIGATE_RIGHT = "ts navigate right";
	public static final String SELECT_NAVIGATE_CHILD = "ts select navigate to child";
	public static final String SELECT_NAVIGATE_PARENT = "ts select navigate to parent";
	public static final String SELECT_NAVIGATE_LEFT = "ts select navigate left";
	public static final String SELECT_NAVIGATE_RIGHT = "ts select navigate right";
	public static final String TOGGLE_FOCUS_SELECTION = "ts toggle focus selection";

	private static final String[] NAVIGATE_CHILD_KEYS = new String[] { "DOWN" };
	private static final String[] NAVIGATE_PARENT_KEYS = new String[] { "UP" };
	private static final String[] NAVIGATE_LEFT_KEYS = new String[] { "LEFT" };
	private static final String[] NAVIGATE_RIGHT_KEYS = new String[] { "RIGHT" };
	private static final String[] SELECT_NAVIGATE_CHILD_KEYS = new String[] { "shift DOWN" };
	private static final String[] SELECT_NAVIGATE_PARENT_KEYS = new String[] { "shift UP" };
	private static final String[] SELECT_NAVIGATE_LEFT_KEYS = new String[] { "shift LEFT" };
	private static final String[] SELECT_NAVIGATE_RIGHT_KEYS = new String[] { "shift RIGHT" };
	private static final String[] TOGGLE_FOCUS_SELECTION_KEYS = new String[] { "SPACE" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.TRACKSCHEME );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( NAVIGATE_CHILD, NAVIGATE_CHILD_KEYS, "Go to the first child of the current spot." );
			descriptions.add( NAVIGATE_PARENT, NAVIGATE_PARENT_KEYS, "Go to the parent of the current spot." );
			descriptions.add( NAVIGATE_LEFT, NAVIGATE_LEFT_KEYS, "Go to the spot on the left." );
			descriptions.add( NAVIGATE_RIGHT, NAVIGATE_RIGHT_KEYS, "Go to the spot on the right." );
			descriptions.add( SELECT_NAVIGATE_CHILD, SELECT_NAVIGATE_CHILD_KEYS, "Go to the first child of the current spot, and select it." );
			descriptions.add( SELECT_NAVIGATE_PARENT, SELECT_NAVIGATE_PARENT_KEYS, "Go to the parent of the current spot, and select it." );
			descriptions.add( SELECT_NAVIGATE_LEFT, SELECT_NAVIGATE_LEFT_KEYS, "Go to the spot on the left, and select it." );
			descriptions.add( SELECT_NAVIGATE_RIGHT, SELECT_NAVIGATE_RIGHT_KEYS, "Go to the spot on the right, and select it." );
			descriptions.add( TOGGLE_FOCUS_SELECTION, TOGGLE_FOCUS_SELECTION_KEYS, "Toggle selection of the current spot." );
		}
	}

	private enum Direction
	{
		CHILD,
		PARENT,
		LEFT_SIBLING,
		RIGHT_SIBLING
	}

	public enum NavigatorEtiquette
	{
		/**
		 * SelectionModel is tied to the focus. When moving the focus with arrow
		 * keys, the selection moves with the focus. When clicking a vertex it
		 * is focused and selected, The focused vertex is always selected. Focus
		 * still exists independent of selection: multiple vertices can be
		 * selected, but only one of them can have the focus. When extending the
		 * selection with shift+arrow keys, the vertex to which the focus moves
		 * should be selected. When box selection is drawn, the selected vertex
		 * closest to the position where the drag ended should receive the
		 * focus.
		 */
		FINDER_LIKE,
		/**
		 * SelectionModel is independent of focus. Moving the focus with arrow keys
		 * doesn't alter selection. Space key toggles selection of focused
		 * vertex. When extending the selection with shift+arrow keys, the
		 * selection of the currently focused vertex is toggled, then the focus
		 * is moved.
		 */
		MIDNIGHT_COMMANDER_LIKE;
	}

	private final TrackSchemeGraph< ?, ? > graph;

	private final ReentrantReadWriteLock lock;

	private final LineageTreeLayout layout;

	private final SelectionModel< TrackSchemeVertex, TrackSchemeEdge > selection;

	private final FocusModel< TrackSchemeVertex > focus;

	/**
	 * Creates a new {@link TrackSchemeNavigationActions}.
	 *
	 * @param graph
	 *            the {@link TrackSchemeGraph} through which to navigate.
	 * @param layout
	 *            the graph layout.
	 * @param focus
	 *            normally {@link TrackSchemeAutoFocus} which makes navigation
	 *            follow the focus.
	 * @param selection
	 *            the selection model for TrackScheme. Is used to possibly add
	 *            the navigated object to the selection.
	 */
	public TrackSchemeNavigationActions(
			final TrackSchemeGraph< ?, ? > graph,
			final LineageTreeLayout layout,
			final FocusModel< TrackSchemeVertex > focus,
			final SelectionModel< TrackSchemeVertex, TrackSchemeEdge > selection )
	{
		this.graph = graph;
		this.lock = graph.getLock();
		this.layout = layout;
		this.focus = focus;
		this.selection = selection;
	}

	public void install(
			final Actions actions,
			final NavigatorEtiquette etiquette )
	{
		switch ( etiquette )
		{
		case MIDNIGHT_COMMANDER_LIKE:
			actions.runnableAction( () -> selectAndFocusNeighbor( Direction.CHILD, false ), NAVIGATE_CHILD, NAVIGATE_CHILD_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighbor( Direction.PARENT, false ), NAVIGATE_PARENT, NAVIGATE_PARENT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighbor( Direction.LEFT_SIBLING, false ), NAVIGATE_LEFT, NAVIGATE_LEFT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighbor( Direction.RIGHT_SIBLING, false ), NAVIGATE_RIGHT, NAVIGATE_RIGHT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighbor( Direction.CHILD, true ), SELECT_NAVIGATE_CHILD, SELECT_NAVIGATE_CHILD_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighbor( Direction.PARENT, true ), SELECT_NAVIGATE_PARENT, SELECT_NAVIGATE_PARENT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighbor( Direction.LEFT_SIBLING, true ), SELECT_NAVIGATE_LEFT, SELECT_NAVIGATE_LEFT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighbor( Direction.RIGHT_SIBLING, true ), SELECT_NAVIGATE_RIGHT, SELECT_NAVIGATE_RIGHT_KEYS );
			actions.runnableAction( () -> toggleSelectionOfFocusedVertex(), TOGGLE_FOCUS_SELECTION, TOGGLE_FOCUS_SELECTION_KEYS );
			break;
		case FINDER_LIKE:
		default:
			actions.runnableAction( () -> selectAndFocusNeighborFL( Direction.CHILD, true ), NAVIGATE_CHILD, NAVIGATE_CHILD_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighborFL( Direction.PARENT, true ), NAVIGATE_PARENT, NAVIGATE_PARENT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighborFL( Direction.LEFT_SIBLING, true ), NAVIGATE_LEFT, NAVIGATE_LEFT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighborFL( Direction.RIGHT_SIBLING, true ), NAVIGATE_RIGHT, NAVIGATE_RIGHT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighborFL( Direction.CHILD, false ), SELECT_NAVIGATE_CHILD, SELECT_NAVIGATE_CHILD_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighborFL( Direction.PARENT, false ), SELECT_NAVIGATE_PARENT, SELECT_NAVIGATE_PARENT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighborFL( Direction.LEFT_SIBLING, false ), SELECT_NAVIGATE_LEFT, SELECT_NAVIGATE_LEFT_KEYS );
			actions.runnableAction( () -> selectAndFocusNeighborFL( Direction.RIGHT_SIBLING, false ), SELECT_NAVIGATE_RIGHT, SELECT_NAVIGATE_RIGHT_KEYS );
			break;
		}
	}

	/*
	 * COMMANDER-LIKE ETIQUETTE METHODS.
	 */

	/**
	 * Focus a neighbor (parent, child, left sibling, right sibling) of the
	 * currently focused vertex. Possibly, the currently focused vertex is added
	 * to the selection.
	 *
	 * @param direction
	 *            which neighbor to focus.
	 * @param select
	 *            if {@code true}, the currently focused vertex is added to the
	 *            selection (before moving the focus).
	 */
	private void selectAndFocusNeighbor( final Direction direction, final boolean select )
	{
		final TrackSchemeVertex ref = graph.vertexRef();
		selectAndFocusNeighbor( direction, select, ref );
		graph.releaseRef( ref );
	}

	private TrackSchemeVertex selectAndFocusNeighbor( final Direction direction, final boolean select, final TrackSchemeVertex ref )
	{
		lock.readLock().lock();
		try
		{
			final TrackSchemeVertex vertex = focus.getFocused( ref );
			if ( vertex == null )
				return null;

			if ( select )
				selection.setSelected( vertex, true );

			final TrackSchemeVertex current;
			switch ( direction )
			{
			case CHILD:
				current = layout.getFirstActiveChild( vertex, ref );
				break;
			case PARENT:
				current = layout.getFirstActiveParent( vertex, ref );
				break;
			case LEFT_SIBLING:
				current = layout.getLeftSibling( vertex, ref );
				break;
			case RIGHT_SIBLING: default:
				current = layout.getRightSibling( vertex, ref );
				break;
			}

			if ( current != null )
				focus.focus( current );

			return current;
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	/**
	 * Toggle the selected state of the currently focused vertex.
	 */
	private void toggleSelectionOfFocusedVertex()
	{
		final TrackSchemeVertex ref = graph.vertexRef();
		lock.readLock().lock();
		try
		{
			final TrackSchemeVertex v = focus.getFocused( ref );
			if ( v != null )
				selection.toggle( v );
			}
		finally
		{
			lock.readLock().unlock();
			graph.releaseRef( ref );
		}
	}

	/*
	 * FINDER-LIKE ETIQUETTE METHODS.
	 */

	/**
	 * Focus and select a neighbor (parent, child, left sibling, right sibling)
	 * of the currently focused vertex. The selection can be cleared before
	 * moving focus.
	 *
	 * @param direction
	 *            which neighbor to focus.
	 * @param clearSelection
	 *            if {@code true}, the selection is cleared before moving the
	 *            focus.
	 */
	private void selectAndFocusNeighborFL( final Direction direction, final boolean clearSelection )
	{
		final TrackSchemeVertex ref = graph.vertexRef();
		selectAndFocusNeighborFL( direction, clearSelection, ref );
		graph.releaseRef( ref );
	}

	private TrackSchemeVertex selectAndFocusNeighborFL( final Direction direction, final boolean clearSelection, final TrackSchemeVertex ref )
	{
		lock.readLock().lock();
		try
		{
			final TrackSchemeVertex vertex = focus.getFocused( ref );
			if ( vertex == null )
				return null;

			selection.pauseListeners();

			final TrackSchemeVertex current;
			switch ( direction )
			{
			case CHILD:
				current = layout.getFirstActiveChild( vertex, ref );
				break;
			case PARENT:
				current = layout.getFirstActiveParent( vertex, ref );
				break;
			case LEFT_SIBLING:
				current = layout.getLeftSibling( vertex, ref );
				break;
			case RIGHT_SIBLING:
			default:
				current = layout.getRightSibling( vertex, ref );
				break;
			}

			if ( current != null )
			{
				focus.focus( current );
				if ( clearSelection )
					selection.clearSelection();
				selection.setSelected( current, true );
			}

			selection.resumeListeners();
			return current;
		}
		finally
		{
			lock.readLock().unlock();
		}
	}
}
