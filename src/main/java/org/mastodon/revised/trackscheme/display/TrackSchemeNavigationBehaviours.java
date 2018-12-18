package org.mastodon.revised.trackscheme.display;

import java.awt.Color;
import java.awt.Graphics;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.collection.RefSet;
import org.mastodon.model.FocusModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.revised.mamut.KeyConfigContexts;
import org.mastodon.revised.trackscheme.LineageTreeLayout;
import org.mastodon.revised.trackscheme.ScreenTransform;
import org.mastodon.revised.trackscheme.TrackSchemeEdge;
import org.mastodon.revised.trackscheme.TrackSchemeGraph;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.trackscheme.display.OffsetHeaders.OffsetHeadersListener;
import org.mastodon.revised.ui.keymap.CommandDescriptionProvider;
import org.mastodon.revised.ui.keymap.CommandDescriptions;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;

/**
 * Focus and selection behaviours in TrackScheme.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class TrackSchemeNavigationBehaviours implements TransformListener< ScreenTransform >, OffsetHeadersListener
{
	public static final String FOCUS_VERTEX = "ts click focus vertex";
	public static final String NAVIGATE_TO_VERTEX = "ts click navigate to vertex";
	public static final String SELECT = "ts click select";
	public static final String ADD_SELECT = "ts click add to selection";
	public static final String BOX_SELECT = "ts box selection";
	public static final String BOX_ADD_SELECT = "ts box add to selection";

	private static final String[] FOCUS_VERTEX_KEYS = new String[] { "button1", "shift button1" };
	private static final String[] NAVIGATE_TO_VERTEX_KEYS = new String[] { "double-click button1", "shift double-click button1" };
	private static final String[] SELECT_KEYS = new String[] { "button1"};
	private static final String[] ADD_SELECT_KEYS = new String[] { "shift button1"};
	private static final String[] BOX_SELECT_KEYS = new String[] { "button1"};
	private static final String[] BOX_ADD_SELECT_KEYS = new String[] { "shift button1"};

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
			descriptions.add( FOCUS_VERTEX, FOCUS_VERTEX_KEYS, "Focus spot (spot gets keyboard focus)." );
			descriptions.add( NAVIGATE_TO_VERTEX, NAVIGATE_TO_VERTEX_KEYS, "Navigate to spot (in all linked views)." );
			descriptions.add( SELECT, SELECT_KEYS, "Select spot or link." );
			descriptions.add( ADD_SELECT, ADD_SELECT_KEYS, "Add spot or link to selection." );
			descriptions.add( BOX_SELECT, BOX_SELECT_KEYS, "Drag a box to select spots and links." );
			descriptions.add( BOX_ADD_SELECT, BOX_ADD_SELECT_KEYS, "Drag a box to add spots and links to selection." );
		}
	}

	public static final double EDGE_SELECT_DISTANCE_TOLERANCE = 5.0;

	private final InteractiveDisplayCanvasComponent< ScreenTransform > display;

	private final TrackSchemeGraph< ?, ? > graph;

	private final ReentrantReadWriteLock lock;

	private final LineageTreeLayout layout;

	private final NavigationHandler< TrackSchemeVertex, TrackSchemeEdge > navigation;

	private final SelectionModel< TrackSchemeVertex, TrackSchemeEdge > selection;

	private final TrackSchemeOverlay graphOverlay;

	private final FocusModel< TrackSchemeVertex > focus;

	private final ScreenTransform screenTransform;

	/**
	 * Current width of vertical header.
	 */
	private int headerWidth;

	/**
	 * Current height of horizontal header.
	 */
	private int headerHeight;

	private final ClickFocusBehaviour focusVertexBehaviour;

	private final ClickNavigateBehaviour navigateToVertexBehaviour;

	private final ClickSelectionBehaviour selectBehaviour;

	private final ClickSelectionBehaviour addSelectBehaviour;

	private final BoxSelectionBehaviour boxSelectBehaviour;

	private final BoxSelectionBehaviour boxAddSelectBehaviour;

	public TrackSchemeNavigationBehaviours(
			final InteractiveDisplayCanvasComponent< ScreenTransform > display,
			final TrackSchemeGraph< ?, ? > graph,
			final LineageTreeLayout layout,
			final TrackSchemeOverlay graphOverlay,
			final FocusModel< TrackSchemeVertex > focus,
			final NavigationHandler< TrackSchemeVertex, TrackSchemeEdge > navigation,
			final SelectionModel< TrackSchemeVertex, TrackSchemeEdge > selection )
	{
		this.display = display;
		this.graph = graph;
		this.lock = graph.getLock();
		this.layout = layout;
		this.graphOverlay = graphOverlay;
		this.focus = focus;
		this.navigation = navigation;
		this.selection = selection;

		screenTransform = new ScreenTransform();

		focusVertexBehaviour = new ClickFocusBehaviour();
		navigateToVertexBehaviour = new ClickNavigateBehaviour();
		selectBehaviour = new ClickSelectionBehaviour( SELECT, false );
		addSelectBehaviour = new ClickSelectionBehaviour( ADD_SELECT, true );
		boxSelectBehaviour = new BoxSelectionBehaviour( BOX_SELECT, false );
		boxAddSelectBehaviour = new BoxSelectionBehaviour( BOX_ADD_SELECT, true );
	}

	public void install( final Behaviours behaviours )
	{
		behaviours.namedBehaviour( focusVertexBehaviour, FOCUS_VERTEX_KEYS );
		behaviours.namedBehaviour( navigateToVertexBehaviour, NAVIGATE_TO_VERTEX_KEYS );
		behaviours.namedBehaviour( selectBehaviour, SELECT_KEYS );
		behaviours.namedBehaviour( addSelectBehaviour, ADD_SELECT_KEYS );
		behaviours.namedBehaviour( boxSelectBehaviour, BOX_SELECT_KEYS );
		behaviours.namedBehaviour( boxAddSelectBehaviour, BOX_ADD_SELECT_KEYS );
	}

	/*
	 * COMMON METHODS.
	 */

	private double ratioXtoY;

	/*
	 * PRIVATE METHODS
	 */

	private void selectWithin( final int x1, final int y1, final int x2, final int y2, final boolean addToSelection )
	{
		selection.pauseListeners();

		if ( !addToSelection )
			selection.clearSelection();

		final double lx1, ly1, lx2, ly2;
		synchronized ( screenTransform )
		{
			lx1 = screenTransform.screenToLayoutX( x1 );
			ly1 = screenTransform.screenToLayoutY( y1 );
			lx2 = screenTransform.screenToLayoutX( x2 );
			ly2 = screenTransform.screenToLayoutY( y2 );
		}

		final RefSet< TrackSchemeVertex > vs = layout.getActiveVerticesWithin( lx1, ly1, lx2, ly2 );
		final TrackSchemeVertex vertexRef = graph.vertexRef();
		for ( final TrackSchemeVertex v : vs )
		{
			selection.setSelected( v, true );
			for ( final TrackSchemeEdge e : v.outgoingEdges() )
			{
				final TrackSchemeVertex t = e.getTarget( vertexRef );
				if ( vs.contains( t ) )
					selection.setSelected( e, true );
			}
		}

		focus.focus( layout.getClosestActiveVertexWithin( lx1, ly1, lx2, ly2, ratioXtoY, vertexRef ) );

		graph.releaseRef( vertexRef );

		selection.resumeListeners();
	}

	private void select( final int x, final int y, final boolean addToSelection )
	{
		selection.pauseListeners();

		final TrackSchemeVertex vertex = graph.vertexRef();
		final TrackSchemeEdge edge = graph.edgeRef();

		// See if we can select a vertex.
		if ( graphOverlay.getVertexAt( x, y, vertex ) != null )
		{
			final boolean selected = selection.isSelected( vertex );
			if ( !addToSelection )
				selection.clearSelection();
			selection.setSelected( vertex, !selected );
		}
		// See if we can select an edge.
		else if ( graphOverlay.getEdgeAt( x, y, EDGE_SELECT_DISTANCE_TOLERANCE, edge ) != null )
		{
			final boolean selected = selection.isSelected( edge );
			if ( !addToSelection )
				selection.clearSelection();
			selection.setSelected( edge, !selected );
		}
		// Nothing found. clear selection if addToSelection == false
		else if ( !addToSelection )
			selection.clearSelection();

		graph.releaseRef( vertex );
		graph.releaseRef( edge );

		selection.resumeListeners();
	}

	private void navigate( final int x, final int y )
	{
		final TrackSchemeVertex vertex = graph.vertexRef();
		final TrackSchemeEdge edge = graph.edgeRef();

		// See if we can find a vertex.
		if ( graphOverlay.getVertexAt( x, y, vertex ) != null )
		{
			navigation.notifyNavigateToVertex( vertex );
		}
		// See if we can find an edge.
		else if ( graphOverlay.getEdgeAt( x, y, EDGE_SELECT_DISTANCE_TOLERANCE, edge ) != null )
		{
			navigation.notifyNavigateToEdge( edge );
		}

		graph.releaseRef( vertex );
		graph.releaseRef( edge );
	}

	private void focus( final int x, final int y )
	{
		final TrackSchemeVertex ref = graph.vertexRef();

		focus.focus( graphOverlay.getVertexAt( x, y, ref ) ); // if clicked outside, getVertexAt == null, clears the focus.

		graph.releaseRef( ref );
	}

	/*
	 * BEHAVIOURS
	 */

	/**
	 * Behaviour to focus a vertex with a mouse click. If the click happens
	 * outside of a vertex, the focus is cleared.
	 * <p>
	 * Note that this only applies to vertices that are individually painted on
	 * the screen. Vertices inside dense ranges are ignored.
	 */
	private class ClickFocusBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		public ClickFocusBehaviour()
		{
			super( FOCUS_VERTEX );
		}

		@Override
		public void click( final int x, final int y )
		{
			if ( x < headerWidth || y < headerHeight )
				return;

			lock.readLock().lock();
			try
			{
				focus( x, y );
			}
			finally
			{
				lock.readLock().unlock();
			}
		}
	}

	/**
	 * Behaviour to navigate to a vertex with a mouse click.
	 * <p>
	 * Note that this only applies to vertices that are individually painted on
	 * the screen. Vertices inside dense ranges are ignored.
	 */
	private class ClickNavigateBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		public ClickNavigateBehaviour()
		{
			super( NAVIGATE_TO_VERTEX );
		}

		@Override
		public void click( final int x, final int y )
		{
			if ( x < headerWidth || y < headerHeight )
				return;

			lock.readLock().lock();
			try
			{
				navigate( x, y );
			}
			finally
			{
				lock.readLock().unlock();
			}
		}
	}

	/**
	 * Behaviour to select a vertex with a mouse click.
	 * <p>
	 * Note that this only applies to vertices that are individually painted on
	 * the screen. Vertices inside dense ranges are ignored.
	 */
	private class ClickSelectionBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		private final boolean addToSelection;

		public ClickSelectionBehaviour( final String name, final boolean addToSelection )
		{
			super( name );
			this.addToSelection = addToSelection;
		}

		@Override
		public void click( final int x, final int y )
		{
			if ( x < headerWidth || y < headerHeight )
				return;

			lock.readLock().lock();
			try
			{
				select( x, y, addToSelection );
			}
			finally
			{
				lock.readLock().unlock();
			}
		}
	}

	/**
	 * Behaviour to select vertices and edges inside a bounding box with a mouse
	 * drag.
	 * <p>
	 * The selection happens in layout space, so it also selects vertices inside
	 * dense ranges. A vertex is inside the bounding box if its layout
	 * coordinate is inside the bounding box.
	 */
	private class BoxSelectionBehaviour extends AbstractNamedBehaviour implements DragBehaviour, OverlayRenderer
	{
		/**
		 * Coordinates where mouse dragging started.
		 */
		private int oX, oY;

		/**
		 * Coordinates where mouse dragging currently is.
		 */
		private int eX, eY;

		private boolean dragging = false;

		private boolean ignore = false;

		private final boolean addToSelection;

		public BoxSelectionBehaviour( final String name, final boolean addToSelection )
		{
			super( name );
			this.addToSelection = addToSelection;
		}

		@Override
		public void init( final int x, final int y )
		{
			oX = x;
			oY = y;
			dragging = false;
			ignore = x < headerWidth || y < headerHeight;
		}

		@Override
		public void drag( final int x, final int y )
		{
			if ( ignore )
				return;

			eX = x;
			eY = y;
			if ( !dragging )
			{
				dragging = true;
				display.addOverlayRenderer( this );
			}
			display.repaint();
		}

		@Override
		public void end( final int x, final int y )
		{
			if ( ignore )
				return;

			if ( dragging )
			{
				dragging = false;
				display.removeOverlayRenderer( this );
				display.repaint();
				lock.readLock().lock();
				try
				{
					selectWithin(
							oX - headerWidth,
							oY - headerHeight,
							eX - headerWidth,
							eY - headerHeight,
							addToSelection );
				}
				finally
				{
					lock.readLock().unlock();
				}
			}
		}

		/**
		 * Draws the selection box, if there is one.
		 */
		@Override
		public void drawOverlays( final Graphics g )
		{
			g.setColor( Color.RED );
			final int x = Math.min( oX, eX );
			final int y = Math.min( oY, eY );
			final int width = Math.abs( eX - oX );
			final int height = Math.abs( eY - oY );
			g.drawRect( x, y, width, height );
		}

		@Override
		public void setCanvasSize( final int width, final int height )
		{}
	}

	@Override
	public void transformChanged( final ScreenTransform transform )
	{
		synchronized ( screenTransform )
		{
			screenTransform.set( transform );
			ratioXtoY = transform.getXtoYRatio();
		}
	}

	@Override
	public void updateHeaderSize( final int width, final int height )
	{
		headerWidth = width;
		headerHeight = height;
	}
}
