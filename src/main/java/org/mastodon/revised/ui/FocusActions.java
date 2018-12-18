package org.mastodon.revised.ui;

import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.Ref;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Edges;
import org.mastodon.graph.Graph;
import org.mastodon.graph.Vertex;
import org.mastodon.model.FocusModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.revised.mamut.KeyConfigContexts;
import org.mastodon.revised.trackscheme.display.TrackSchemeNavigationActions.NavigatorEtiquette;
import org.mastodon.revised.ui.keymap.CommandDescriptionProvider;
import org.mastodon.revised.ui.keymap.CommandDescriptions;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

/**
 * Generic tree navigation actions.
 * Implements {@link NavigatorEtiquette#FINDER_LIKE} flavour.
 *
 * @param <V>
 *     vertex type.
 * @param <E>
 *     edge type.
 *
 * @author Tobias Pietzsch
 */
public class FocusActions< V extends Vertex< E > & Ref< V >, E extends Edge< V > & Ref< E > >
{
	public static final String NAVIGATE_CHILD = "navigate to child";
	public static final String NAVIGATE_LAST_CHILD = "navigate to last child";
	public static final String NAVIGATE_PARENT = "navigate to parent";
	public static final String NAVIGATE_SIBLING = "navigate to sibling";
	public static final String NAVIGATE_BRANCH_CHILD = "navigate to branch child";
	public static final String NAVIGATE_LAST_BRANCH_CHILD = "navigate to last branch child";
	public static final String NAVIGATE_BRANCH_PARENT = "navigate to branch parent";
	public static final String SELECT_NAVIGATE_CHILD = "select navigate to child";
	public static final String SELECT_NAVIGATE_LAST_CHILD = "select navigate to last child";
	public static final String SELECT_NAVIGATE_PARENT = "select navigate to parent";
	public static final String SELECT_NAVIGATE_SIBLING = "select navigate to sibling";
	public static final String SELECT_NAVIGATE_BRANCH_CHILD = "select navigate to branch child";
	public static final String SELECT_NAVIGATE_LAST_BRANCH_CHILD = "select navigate to last branch child";
	public static final String SELECT_NAVIGATE_BRANCH_PARENT = "select navigate to branch parent";

	private static final String[] NAVIGATE_CHILD_KEYS = new String[] { "ctrl DOWN" };
	private static final String[] NAVIGATE_LAST_CHILD_KEYS = new String[] { "ctrl meta DOWN" };
	private static final String[] NAVIGATE_PARENT_KEYS = new String[] { "ctrl UP" };
	private static final String[] NAVIGATE_SIBLING_KEYS = new String[] { "ctrl LEFT", "ctrl RIGHT" };
	private static final String[] NAVIGATE_BRANCH_CHILD_KEYS = new String[] { "ctrl alt DOWN" };
	private static final String[] NAVIGATE_LAST_BRANCH_CHILD_KEYS = new String[] { "ctrl alt meta DOWN" };
	private static final String[] NAVIGATE_BRANCH_PARENT_KEYS = new String[] { "ctrl alt UP" };
	private static final String[] SELECT_NAVIGATE_CHILD_KEYS = new String[] { "shift ctrl DOWN" };
	private static final String[] SELECT_NAVIGATE_LAST_CHILD_KEYS = new String[] { "shift ctrl meta DOWN" };
	private static final String[] SELECT_NAVIGATE_PARENT_KEYS = new String[] { "shift ctrl UP" };
	private static final String[] SELECT_NAVIGATE_SIBLING_KEYS = new String[] { "shift ctrl LEFT", "shift ctrl RIGHT" };
	private static final String[] SELECT_NAVIGATE_BRANCH_CHILD_KEYS = new String[] { "shift ctrl alt DOWN" };
	private static final String[] SELECT_NAVIGATE_LAST_BRANCH_CHILD_KEYS = new String[] { "shift ctrl alt meta DOWN" };
	private static final String[] SELECT_NAVIGATE_BRANCH_PARENT_KEYS = new String[] { "shift ctrl alt UP" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.BIGDATAVIEWER, KeyConfigContexts.TRACKSCHEME );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( NAVIGATE_CHILD, NAVIGATE_CHILD_KEYS, "Go to the first child of the current spot." );
			descriptions.add( NAVIGATE_LAST_CHILD, NAVIGATE_LAST_CHILD_KEYS, "Go to the last child of the current spot." );
			descriptions.add( NAVIGATE_PARENT, NAVIGATE_PARENT_KEYS, "Go to the parent of the current spot." );
			descriptions.add( NAVIGATE_SIBLING, NAVIGATE_SIBLING_KEYS, "Go to the sibling of the current spot." );
			descriptions.add( NAVIGATE_BRANCH_CHILD, NAVIGATE_BRANCH_CHILD_KEYS, "Go to the next division on the first branch of the current spot." );
			descriptions.add( NAVIGATE_LAST_BRANCH_CHILD, NAVIGATE_LAST_BRANCH_CHILD_KEYS, "Go to the next division on the last branch of the current spot." );
			descriptions.add( NAVIGATE_BRANCH_PARENT, NAVIGATE_BRANCH_PARENT_KEYS, "Go to the previous division on the branch of the current spot." );
			descriptions.add( SELECT_NAVIGATE_CHILD, SELECT_NAVIGATE_CHILD_KEYS, "Go to the first child of the current spot, and select it." );
			descriptions.add( SELECT_NAVIGATE_LAST_CHILD, SELECT_NAVIGATE_LAST_CHILD_KEYS, "Go to the last child of the current spot, and select it." );
			descriptions.add( SELECT_NAVIGATE_PARENT, SELECT_NAVIGATE_PARENT_KEYS, "Go to the parent of the current spot, and select it." );
			descriptions.add( SELECT_NAVIGATE_SIBLING, SELECT_NAVIGATE_SIBLING_KEYS, "Go to the sibling of the current spot, and select it." );
			descriptions.add( SELECT_NAVIGATE_BRANCH_CHILD, SELECT_NAVIGATE_BRANCH_CHILD_KEYS, "Go to the next division on the first branch of the current spot, and select all spots on the way." );
			descriptions.add( SELECT_NAVIGATE_LAST_BRANCH_CHILD, SELECT_NAVIGATE_LAST_BRANCH_CHILD_KEYS, "Go to the next division on the last branch of the current spot, and select all spots on the way." );
			descriptions.add( SELECT_NAVIGATE_BRANCH_PARENT, SELECT_NAVIGATE_BRANCH_PARENT_KEYS, "Go to the previous division on the branch of the current spot, and select all spots on the way." );
		}
	}

	private final RunnableAction navigateToChildAction;

	private final RunnableAction navigateToLastChildAction;

	private final RunnableAction navigateToParentAction;

	private final RunnableAction navigateToSiblingAction;

	private final RunnableAction navigateToBranchChildAction;

	private final RunnableAction navigateToLastBranchChildAction;

	private final RunnableAction navigateToBranchParentAction;

	private final RunnableAction selectNavigateToChildAction;

	private final RunnableAction selectNavigateToLastChildAction;

	private final RunnableAction selectNavigateToParentAction;

	private final RunnableAction selectNavigateToSiblingAction;

	private final RunnableAction selectNavigateToBranchChildAction;

	private final RunnableAction selectNavigateToLastBranchChildAction;

	private final RunnableAction selectNavigateToBranchParentAction;

	public static < V extends Vertex< E > & Ref< V >, E extends Edge< V > & Ref< E > > void install(
			final Actions actions,
			final Graph< V, E > graph,
			final ReentrantReadWriteLock lock,
			final FocusModel< V > focus,
			final SelectionModel< V, E > selection )
	{
		final FocusActions< V, E > fa = new FocusActions<>( graph, lock, focus, selection );

		actions.namedAction( fa.navigateToChildAction, NAVIGATE_CHILD_KEYS );
		actions.namedAction( fa.navigateToLastChildAction, NAVIGATE_LAST_CHILD_KEYS );
		actions.namedAction( fa.navigateToParentAction, NAVIGATE_PARENT_KEYS );
		actions.namedAction( fa.navigateToSiblingAction, NAVIGATE_SIBLING_KEYS );
		actions.namedAction( fa.navigateToBranchChildAction, NAVIGATE_BRANCH_CHILD_KEYS );
		actions.namedAction( fa.navigateToLastBranchChildAction, NAVIGATE_LAST_BRANCH_CHILD_KEYS );
		actions.namedAction( fa.navigateToBranchParentAction, NAVIGATE_BRANCH_PARENT_KEYS );
		actions.namedAction( fa.selectNavigateToChildAction, SELECT_NAVIGATE_CHILD_KEYS );
		actions.namedAction( fa.selectNavigateToLastChildAction, SELECT_NAVIGATE_LAST_CHILD_KEYS );
		actions.namedAction( fa.selectNavigateToParentAction, SELECT_NAVIGATE_PARENT_KEYS );
		actions.namedAction( fa.selectNavigateToSiblingAction, SELECT_NAVIGATE_SIBLING_KEYS );
		actions.namedAction( fa.selectNavigateToBranchChildAction, SELECT_NAVIGATE_BRANCH_CHILD_KEYS );
		actions.namedAction( fa.selectNavigateToLastBranchChildAction, SELECT_NAVIGATE_LAST_BRANCH_CHILD_KEYS );
		actions.namedAction( fa.selectNavigateToBranchParentAction, SELECT_NAVIGATE_BRANCH_PARENT_KEYS );
	}

	private enum Direction
	{
		CHILD,
		LAST_CHILD,
		PARENT,
		SIBLING,
	}

	private final Graph< V, E > graph;

	private final ReentrantReadWriteLock lock;

	private final FocusModel< V > focus;

	private final SelectionModel< V, E > selection;

	public FocusActions(
			final Graph< V, E > graph,
			final ReentrantReadWriteLock lock,
			final FocusModel< V > focus,
			final SelectionModel< V, E > selection )
	{
		this.graph = graph;
		this.lock = lock;
		this.focus = focus;
		this.selection = selection;

		navigateToChildAction = new RunnableAction( NAVIGATE_CHILD, () -> selectAndFocusNeighbor( Direction.CHILD, false ) );
		navigateToLastChildAction = new RunnableAction( NAVIGATE_LAST_CHILD, () -> selectAndFocusNeighbor( Direction.LAST_CHILD, false ) );
		navigateToParentAction = new RunnableAction( NAVIGATE_PARENT, () -> selectAndFocusNeighbor( Direction.PARENT, false ) );
		navigateToSiblingAction = new RunnableAction( NAVIGATE_SIBLING, () -> selectAndFocusNeighbor( Direction.SIBLING, false ) );
		navigateToBranchChildAction = new RunnableAction( NAVIGATE_BRANCH_CHILD, () -> selectAndFocusBranchNeighbor( Direction.CHILD, false ) );
		navigateToLastBranchChildAction = new RunnableAction( NAVIGATE_LAST_BRANCH_CHILD, () -> selectAndFocusBranchNeighbor( Direction.LAST_CHILD, false ) );
		navigateToBranchParentAction = new RunnableAction( NAVIGATE_BRANCH_PARENT, () -> selectAndFocusBranchNeighbor( Direction.PARENT, false ) );
		selectNavigateToChildAction = new RunnableAction( SELECT_NAVIGATE_CHILD, () -> selectAndFocusNeighbor( Direction.CHILD, true ) );
		selectNavigateToLastChildAction = new RunnableAction( SELECT_NAVIGATE_LAST_CHILD, () -> selectAndFocusNeighbor( Direction.LAST_CHILD, true ) );
		selectNavigateToParentAction = new RunnableAction( SELECT_NAVIGATE_PARENT, () -> selectAndFocusNeighbor( Direction.PARENT, true ) );
		selectNavigateToSiblingAction = new RunnableAction( SELECT_NAVIGATE_SIBLING, () -> selectAndFocusNeighbor( Direction.SIBLING, true ) );
		selectNavigateToBranchChildAction = new RunnableAction( SELECT_NAVIGATE_BRANCH_CHILD, () -> selectAndFocusBranchNeighbor( Direction.CHILD, true ) );
		selectNavigateToLastBranchChildAction = new RunnableAction( SELECT_NAVIGATE_LAST_BRANCH_CHILD, () -> selectAndFocusBranchNeighbor( Direction.LAST_CHILD, true ) );
		selectNavigateToBranchParentAction = new RunnableAction( SELECT_NAVIGATE_BRANCH_PARENT, () -> selectAndFocusBranchNeighbor( Direction.PARENT, true ) );
	}

	/**
	 * Focus and select a neighbor (parent, child, sibling)
	 * of the currently focused vertex. The selection can be cleared before
	 * moving focus.
	 *
	 * @param direction
	 * 		which neighbor to focus.
	 * @param expandSelection
	 * 		if {@code false}, the selection is cleared before moving the
	 * 		focus (and selecting the newly focused vertex).
	 */
	private void selectAndFocusNeighbor( final Direction direction, final boolean expandSelection )
	{
		final V ref1 = graph.vertexRef();
		final V ref2 = graph.vertexRef();
		lock.readLock().lock();
		try
		{
			final V vertex = focus.getFocused( ref1 );
			if ( vertex == null )
				return;

			final V current;
			switch ( direction )
			{
			case CHILD:
				current = firstChild( vertex, ref2 );
				break;
			case LAST_CHILD:
				current = lastChild( vertex, ref2 );
				break;
			case PARENT:
				current = firstParent( vertex, ref2 );
				break;
			case SIBLING:
				current = nextSibling( vertex, ref2 );
				break;
			default:
				current = null;
			}

			if ( current != null )
			{
				selection.pauseListeners();

				focus.focus( current );

				if ( !expandSelection )
					selection.clearSelection();
				selection.setSelected( current, true );

				selection.resumeListeners();
			}
		}
		finally
		{
			lock.readLock().unlock();
			graph.releaseRef( ref1 );
			graph.releaseRef( ref2 );
		}
	}

	private void selectAndFocusBranchNeighbor( final Direction direction, final boolean expandSelection )
	{
		final V ref1 = graph.vertexRef();
		final V ref2 = graph.vertexRef();
		final V ref3 = graph.vertexRef();
		lock.readLock().lock();
		try
		{
			final V vertex = focus.getFocused( ref1 );
			if ( vertex == null )
				return;

			selection.pauseListeners();

			final V current;
			switch ( direction )
			{
			case CHILD:
				current = firstBranchChild( vertex, ref2, expandSelection );
				break;
			case LAST_CHILD:
				current = lastBranchChild( vertex, ref2, expandSelection );
				break;
			case PARENT:
				current = firstBranchParent( vertex, ref2, expandSelection );
				break;
			default:
				current = null;
			}

			if ( current != null )
			{
				focus.focus( current );
				if ( !expandSelection )
				{
					selection.clearSelection();
					selection.setSelected( current, true );
				}
			}

			selection.resumeListeners();
		}
		finally
		{
			lock.readLock().unlock();
			graph.releaseRef( ref1 );
			graph.releaseRef( ref2 );
			graph.releaseRef( ref3 );
		}
	}

	private V firstChild( final V vertex, final V ref )
	{
		final Edges< E > outgoing = vertex.outgoingEdges();
		return outgoing.isEmpty() ? null : outgoing.iterator().next().getTarget( ref );
	}

	private V lastChild( final V vertex, final V ref )
	{
		final Iterator< E > it = vertex.outgoingEdges().iterator();
		E edge = null;
		while ( it.hasNext() )
			edge = it.next();
		return edge == null ? null : edge.getTarget( ref );
	}

	private V firstParent( final V vertex, final V ref )
	{
		final Edges< E > incoming = vertex.incomingEdges();
		return incoming.isEmpty() ? null : incoming.iterator().next().getSource( ref );
	}

	private V firstBranchChild( final V vertex, final V ref, final boolean expandSelection )
	{
		V v = vertex;
		while ( true )
		{
			if ( v.outgoingEdges().isEmpty() )
				break;
			v = firstChild( v, ref );
			if ( expandSelection )
				selection.setSelected( v, true );
			if ( v.outgoingEdges().size() > 1 )
				break;
		}
		return v == vertex ? null : v;
	}

	private V lastBranchChild( final V vertex, final V ref, final boolean expandSelection )
	{
		V v = vertex;
		while ( true )
		{
			if ( v.outgoingEdges().isEmpty() )
				break;
			v = lastChild( v, ref );
			if ( expandSelection )
				selection.setSelected( v, true );
			if ( v.outgoingEdges().size() > 1 )
				break;
		}
		return v == vertex ? null : v;
	}

	private V firstBranchParent( final V vertex, final V ref, final boolean expandSelection )
	{
		V v = vertex;
		while ( true )
		{
			if ( v.incomingEdges().isEmpty() )
				break;
			v = firstParent( v, ref );
			if ( expandSelection )
				selection.setSelected( v, true );
			if ( v.outgoingEdges().size() > 1 )
				break;
		}
		return v == vertex ? null : v;
	}

	private V nextSibling( final V vertex, final V ref )
	{
		final V ref1 = graph.vertexRef();
		final V ref2 = graph.vertexRef();
		try
		{
			V child = ref1;
			final V parent = ref2;
			child.refTo( vertex );
			int d = 0;
			while ( true )
			{
				if ( child.incomingEdges().isEmpty() )
					return null;
				parent.refTo( firstParent( child, ref ) );
				if ( parent.outgoingEdges().size() > 1 )
					break;
				child.refTo( parent );
				++d;
			}

			final Edges< E > outgoing = parent.outgoingEdges();
			final Iterator< E > iter = outgoing.iterator();
			while ( iter.hasNext() )
			{
				if ( iter.next().getTarget( ref ).equals( child ) )
				{
					child = iter.hasNext()
							? iter.next().getTarget( ref )
							: outgoing.iterator().next().getTarget( ref );

					for ( ; d > 0 && child != null; --d )
						child = firstChild( child, ref );

					return child;
				}
			}

			return null;
		}
		finally
		{
			graph.releaseRef( ref1 );
			graph.releaseRef( ref2 );
		}
	}
}
