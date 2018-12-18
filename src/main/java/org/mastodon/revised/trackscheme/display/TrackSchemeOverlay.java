package org.mastodon.revised.trackscheme.display;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mastodon.collection.RefList;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightModel;
import org.mastodon.revised.trackscheme.ScreenEdge;
import org.mastodon.revised.trackscheme.ScreenEntities;
import org.mastodon.revised.trackscheme.ScreenVertex;
import org.mastodon.revised.trackscheme.ScreenVertexRange;
import org.mastodon.revised.trackscheme.TrackSchemeEdge;
import org.mastodon.revised.trackscheme.TrackSchemeGraph;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.trackscheme.display.OffsetHeaders.OffsetHeadersListener;
import org.mastodon.revised.trackscheme.display.style.TrackSchemeStyle;

import net.imglib2.ui.OverlayRenderer;

/**
 * An {@link OverlayRenderer} that paints {@link ScreenEntities} of a
 * TrackScheme graph. Comprises methods to paint vertices, edges, and dense
 * vertex ranges. It has no layout capabilities of its own; it just paints
 * laid-out screen objects.
 * <p>
 * It takes the laid-out {@link ScreenEntities} that it receives with the method
 * {@link #setScreenEntities(ScreenEntities)}, and can deal separately with
 * {@link ScreenVertex} and {@link ScreenVertexRange}.
 * <p>
 * This class takes care of double-buffering of {@link ScreenEntities} and
 * retrieving highlight, focus, etc. Details of how to paint vertices, edges and
 * background are {@link PaintDecorations} and {@link PaintGraph}. These classes
 * only operate on {@link ScreenEntities} and do not need to care about
 * concurrent modifications of the graph.
 * <p>
 * When the {@link #drawOverlays(Graphics)} method is called, the following
 * sequence of methods is executed:
 * <ol>
 * <li>{@link PaintDecorations#paintBackground(Graphics2D, int, int, int, int, ScreenEntities, int, TrackSchemeStyle)}
 * to paint background decorations.
 * <li>{@link PaintGraph#paintGraph(Graphics2D, ScreenEntities, int, int, int, TrackSchemeStyle)}
 * to paint the graph's vertices and edges.
 * <li>{@link PaintDecorations#paintHeaders(Graphics2D, int, int, int, int, ScreenEntities, int, TrackSchemeStyle)}
 * to paint header decorations.</li>
 * </ol>
 * <p>
 * It also offers facilities to interrogate what has been painted where, to
 * facilitate writing user interfaces. For instance, it can return the
 * TrackScheme edge or vertex id near a screen {@code (x, y)} coordinate.
 *
 * @author Tobias Pietzsch
 */
public class TrackSchemeOverlay implements OverlayRenderer, OffsetHeadersListener
{
	/**
	 * The {@link ScreenEntities} that are actually drawn on the canvas.
	 */
	protected final ScreenEntities entities;

	/**
	 * {@link ScreenEntities} that have been previously
	 * {@link #setScreenEntities(ScreenEntities) set} for painting. Whenever new
	 * entities are set, these are stored here and marked {@link #pending}. Whenever
	 * entities are painted and new entities are pending, the new entities are painted
	 * to the screen. Before doing this, the entities previously used for painting
	 * are swapped into {@link #pendingEntities}. This is used for double-buffering.
	 */
	private ScreenEntities pendingEntities;

	/**
	 * Whether new entitites are pending.
	 */
	private boolean pending;

	private final TrackSchemeGraph< ?, ? > graph;

	private final HighlightModel< TrackSchemeVertex, TrackSchemeEdge > highlight;

	private final FocusModel< TrackSchemeVertex > focus;

	private int currentTimepoint = 0;

	private int width;

	private int height;

	private int headerWidth;

	private int headerHeight;

	private final TrackSchemeStyle style;

	private final PaintDecorations paintDecorations;

	private final PaintGraph paintGraph;

	/**
	 * The {@link OverlayRenderer}s that draw above the background
	 */
	private final CopyOnWriteArrayList< OverlayRenderer > overlayRenderers;

	/**
	 * Creates a new overlay for the specified TrackScheme graph.
	 *
	 * @param graph
	 *            the graph to paint.
	 * @param highlight
	 *            the highlight model that indicates which vertex is
	 *            highlighted.
	 * @param focus
	 *            the focus model that indicates which vertex is focused.
	 * @param paintDecorations
	 *            for painting background and headers
	 * @param paintGraph
	 *            for painting vertices and edges
	 * @param options
	 *            options for TrackScheme look.
	 */
	public TrackSchemeOverlay(
			final TrackSchemeGraph< ?, ? > graph,
			final HighlightModel< TrackSchemeVertex, TrackSchemeEdge > highlight,
			final FocusModel< TrackSchemeVertex > focus,
			final PaintDecorations paintDecorations,
			final PaintGraph paintGraph,
			final TrackSchemeOptions options )
	{
		this.graph = graph;
		this.highlight = highlight;
		this.focus = focus;
		this.paintDecorations = paintDecorations;
		this.paintGraph = paintGraph;
		style = options.values.getStyle();
		width = options.values.getWidth();
		height = options.values.getHeight();
		entities = new ScreenEntities( graph );
		overlayRenderers = new CopyOnWriteArrayList<>();
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D g2 = ( Graphics2D ) g;
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		swapScreenEntities();

		final TrackSchemeVertex ref = graph.vertexRef();
		final TrackSchemeEdge eref = graph.edgeRef();

		final TrackSchemeVertex h = highlight.getHighlightedVertex( ref );
		final int highlightedVertexId = ( h == null ) ? -1 : h.getInternalPoolIndex();

		final TrackSchemeEdge he = highlight.getHighlightedEdge( eref );
		final int highlightedEdgeId = ( he == null ) ? -1 : he.getInternalPoolIndex();

		final TrackSchemeVertex f = focus.getFocused( ref );
		final int focusedVertexId = ( f == null ) ? -1 : f.getInternalPoolIndex();

		graph.releaseRef( ref );

		paintDecorations.paintBackground( g2, width, height, headerWidth, headerHeight, entities, currentTimepoint, style );

		// Paint extra overlay if any.
		for ( final OverlayRenderer or : overlayRenderers )
			or.drawOverlays( g );

		final boolean antialiasOffForGraph = entities.getVertices().size() > 10000;
		if ( antialiasOffForGraph )
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
		paintGraph.paintGraph( g2, entities, highlightedVertexId, highlightedEdgeId, focusedVertexId, style );
		if ( antialiasOffForGraph )
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

		paintDecorations.paintHeaders( g2, width, height, headerWidth, headerHeight, entities, currentTimepoint, style );
	}

	/**
	 * Returns the {@link TrackSchemeEdge} currently painted on this display at
	 * screen coordinates specified by {@code x} and {@code y} and within a
	 * distance tolerance.
	 * <p>
	 * This method exists to facilitate writing mouse handlers.
	 * <p>
	 * Note that this really only looks at edges that are individually painted
	 * on the screen. Edges inside dense ranges are ignored.
	 *
	 * @param x
	 *            the x screen coordinate
	 * @param y
	 *            the y screen coordinate
	 * @param tolerance
	 *            the maximal distance to the closest edge.
	 * @param ref
	 *            a reference that will be used to retrieve the result.
	 * @return the {@link TrackSchemeEdge} at {@code (x, y)}, or {@code null} if
	 *         there is no edge within the distance tolerance.
	 */
	public TrackSchemeEdge getEdgeAt( final int x, final int y, final double tolerance, final TrackSchemeEdge ref )
	{
		synchronized ( entities )
		{
			final RefList< ScreenVertex > vertices = entities.getVertices();
			final ScreenVertex vt = vertices.createRef();
			final ScreenVertex vs = vertices.createRef();

			int i = -1;
			for ( final ScreenEdge e : entities.getEdges() )
			{
				vertices.get( e.getSourceScreenVertexIndex(), vs );
				vertices.get( e.getTargetScreenVertexIndex(), vt );
				if ( paintGraph.distanceToPaintedEdge( x, y, e, vs, vt ) <= tolerance )
				{
					i = e.getTrackSchemeEdgeId();
					break;
				}
			}

			vertices.releaseRef( vs );
			vertices.releaseRef( vt );

			return ( i >= 0 )
					? graph.getEdgePool().getObjectIfExists( i, ref )
					: null;
		}
	}

	/**
	 * Returns the {@link TrackSchemeVertex} currently painted on this display
	 * at screen coordinates specified by {@code x} and {@code y}.
	 * <p>
	 * This method exists to facilitate writing mouse handlers.
	 * <p>
	 * Note that this really only looks at vertices that are individually
	 * painted on the screen. Vertices inside dense ranges are ignored.
	 *
	 * @param x
	 *            the x screen coordinate
	 * @param y
	 *            the y screen coordinate
	 * @param ref
	 *            a reference that will be used to retrieve the result.
	 * @return the {@link TrackSchemeVertex} at
	 *         {@code (x, y)}, or {@code null} if there is no vertex at this position.
	 */
	public TrackSchemeVertex getVertexAt( final int x, final int y, final TrackSchemeVertex ref )
	{
		synchronized ( entities )
		{
			double d2Best = Double.POSITIVE_INFINITY;
			int iBest = -1;
			for ( final ScreenVertex v : entities.getVertices() )
			{
				if ( paintGraph.isInsidePaintedVertex( x, y, v ) )
				{
					final int i = v.getTrackSchemeVertexId();
					if ( i >= 0 )
					{
						final double dx = v.getX() - x;
						final double dy = v.getY() - y;
						final double d2 = dx * dx + dy * dy;
						if ( d2 < d2Best )
						{
							d2Best = d2;
							iBest = i;
						}
					}
				}
			}

			return ( iBest >= 0 )
					? graph.getVertexPool().getObjectIfExists( iBest, ref )
					: null;
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
		for ( final OverlayRenderer overlay : overlayRenderers )
			overlay.setCanvasSize( width, height );
	}

	@Override
	public void updateHeaderSize( final int width, final int height )
	{
		headerWidth = width;
		headerHeight = height;
	}

	/**
	 * Returns the width of this overlay.
	 *
	 * @return the width.
	 */
	protected int getWidth()
	{
		return width;
	}

	/**
	 * Returns the hight of this overlay.
	 *
	 * @return the height.
	 */
	protected int getHeight()
	{
		return height;
	}

	/**
	 * Sets the current timepoint.
	 *
	 * @param timepoint
	 *            the current timepoint.
	 */
	public void setCurrentTimepoint( final int timepoint )
	{
		this.currentTimepoint  = timepoint;
	}

	/**
	 * Returns the current timepoint.
	 *
	 * @return the current timepoint.
	 */
	protected int getCurrentTimepoint()
	{
		return currentTimepoint;
	}

	/**
	 * Set the {@link ScreenEntities} to paint.
	 *
	 * @param entities
	 *            {@link ScreenEntities} to paint.
	 * @return the previous {@link ScreenEntities}.
	 */
	public synchronized ScreenEntities setScreenEntities( final ScreenEntities entities )
	{
		final ScreenEntities tmp = pendingEntities;
		pendingEntities = entities;
		pending = true;
		return tmp;
	}

	/**
	 * Provides subclass access to {@link ScreenEntities} to paint.
	 * Implements double-buffering.
	 *
	 * @return current {@link ScreenEntities}.
	 */
	private synchronized ScreenEntities swapScreenEntities()
	{
		if ( pending )
		{
			synchronized ( entities )
			{
				entities.set( pendingEntities );
				pending = false;
			}
		}
		return entities;
	}

	/**
	 * Adds an extra overlay that will be painted along with this one. Overlays
	 * added by this method will be painted after background has been painted,
	 * but before the graph and decoration are painted, so that they "lay below"
	 * the graph and decoration renderings.
	 *
	 * @param overlay
	 *            the overlay to paint.
	 */
	public void addOverlayRenderer( final OverlayRenderer overlay )
	{
		overlayRenderers.add( overlay );
		setCanvasSize( getWidth(), getHeight() );
	}

	/**
	 * Remove an {@link OverlayRenderer}.
	 *
	 * @param renderer
	 *            overlay renderer to remove.
	 */
	public void removeOverlayRenderer( final OverlayRenderer renderer )
	{
		overlayRenderers.remove( renderer );
	}

	public static class TrackSchemeOverlayFactory
	{
		public TrackSchemeOverlay create(
				final TrackSchemeGraph< ?, ? > graph,
				final HighlightModel< TrackSchemeVertex, TrackSchemeEdge > highlight,
				final FocusModel< TrackSchemeVertex > focus,
				final TrackSchemeOptions options )
		{
			return new TrackSchemeOverlay( graph, highlight, focus, new PaintDecorations(), new PaintGraph(), options );
		}
	}
}
