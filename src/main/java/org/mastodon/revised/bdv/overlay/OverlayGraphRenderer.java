package org.mastodon.revised.bdv.overlay;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

import org.mastodon.collection.RefCollection;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefList;
import org.mastodon.kdtree.ClipConvexPolytope;
import org.mastodon.revised.Util;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.spatial.SpatioTemporalIndex;

import bdv.util.Affine3DHelpers;
import bdv.viewer.TimePointListener;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.kdtree.ConvexPolytope;
import net.imglib2.algorithm.kdtree.HyperPlane;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;
import net.imglib2.util.LinAlgHelpers;

/**
 * Renderer for a time-resliced graph overlay on a BDV.
 * <p>
 * In this class, spatial coordinates are stored in local variables {@code gPos}
 * and {@code lPos} of type {@code double[3]}:
 * <ul>
 * <li>{@code gPos} are world coordinates. It is used to store coordinates in
 * the global referential, that is the one with absolute, physical coordinates.
 * It is used <i>e.g.</i> to store vertex coordinates:
 * {@code vertex.localize(gPos)}.</li>
 * <li>{@code lPos} are viewer coordinates. It is used to store coordinates in
 * the local referential, currently rendered in the BDV under a certain
 * orientation, zoom, etc. Mouse coordinates are typically stored in this
 * variable.
 * </ul>
 *
 * <p>
 *
 * TODO: Review and revise.
 *
 * @param <V>
 *            the type of model vertex.
 * @param <E>
 *            the type of model edge.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class OverlayGraphRenderer< V extends OverlayVertex< V, E >, E extends OverlayEdge< E, V > >
		implements OverlayRenderer, TransformListener< AffineTransform3D >, TimePointListener
{
	private int width;

	private int height;

	private final AffineTransform3D renderTransform;

	private int renderTimepoint;

	private final OverlayGraph< V, E > graph;

	private final SpatioTemporalIndex< V > index;

	private final OverlayHighlight< V, E > highlight;

	private final OverlayFocus< V, E > focus;

	public OverlayGraphRenderer(
			final OverlayGraph< V, E > graph,
			final OverlayHighlight< V, E > highlight,
			final OverlayFocus< V, E > focus )
	{
		this.graph = graph;
		this.highlight = highlight;
		this.focus = focus;
		index = graph.getIndex();
		renderTransform = new AffineTransform3D();
		setRenderSettings( RenderSettings.defaultStyle() );
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		synchronized ( renderTransform )
		{
			renderTransform.set( transform );
		}
	}

	@Override
	public void timePointChanged( final int timepoint )
	{
		renderTimepoint = timepoint;
	}

	public void setRenderSettings( final RenderSettings settings )
	{
		antialiasing = settings.getUseAntialiasing()
				? RenderingHints.VALUE_ANTIALIAS_ON
				: RenderingHints.VALUE_ANTIALIAS_OFF;
		useGradient = settings.getUseGradient();
		timeLimit = settings.getTimeLimit();
		drawLinks = settings.getDrawLinks();
		drawSpots = settings.getDrawSpots();
		drawEllipsoidSliceProjection = settings.getDrawEllipsoidSliceProjection();
		drawEllipsoidSliceIntersection = settings.getDrawEllipsoidSliceIntersection();
		drawPoints = settings.getDrawSpotCenters();
		drawPointsForEllipses = settings.getDrawSpotCentersForEllipses();
		drawSpotLabels = settings.getDrawSpotLabels();
		focusLimit = settings.getFocusLimit();
		isFocusLimitViewRelative = settings.getFocusLimitViewRelative();
		ellipsoidFadeDepth = settings.getEllipsoidFadeDepth();
		pointFadeDepth = settings.getPointFadeDepth();
		defaultVertexStroke = settings.getSpotStroke();
		focusedVertexStroke = settings.getSpotFocusStroke();
		highlightedVertexStroke = settings.getSpotHighlightStroke();
		defaultEdgeStroke = settings.getLinkStroke();
		highlightedEdgeStroke = settings.getLinkHighlightStroke();
		color1 = settings.getLinkColor1();
		color2 = settings.getLinkColor2();
		drawLinkArrows = settings.getDrawLinkArrows();
	}

	public static final double pointRadius = 2.5;

	/**
	 * Antialiasing {@link RenderingHints}.
	 */
	private Object antialiasing;

	/**
	 * If {@code true}, draw links using a gradient from source color to target
	 * color. If {@code false}, draw links using the target color.
	 */
	private boolean useGradient;

	/**
	 * Maximum number of timepoints into the past for which outgoing edges
	 * should be drawn.
	 */
	private int timeLimit;

	/**
	 * Whether to draw links (at all).
	 * For specific settings, see TODO
	 */
	private boolean drawLinks;


	/**
	 * Whether to draw spots (at all).
	 * For specific settings, see TODO
	 */
	private boolean drawSpots;

	/**
	 * Whether to draw the intersections of spot ellipsoids with the view plane.
	 */
	private boolean drawEllipsoidSliceProjection;

	/**
	 * Whether to draw the projections of spot ellipsoids onto the view plane.
	 */
	private boolean drawEllipsoidSliceIntersection;

	/**
	 * Whether to draw spot centers.
	 */
	private boolean drawPoints;

	/**
	 * Whether to draw spot centers also for those points that are visible as ellipses.
	 */
	private boolean drawPointsForEllipses;

	/**
	 * Whether to draw spot labels next to ellipses.
	 */
	private boolean drawSpotLabels;

	/**
	 * Maximum distance from view plane up to which to draw spots.
	 *
	 * <p>
	 * Depending on {@link #isFocusLimitViewRelative}, the distance is either in
	 * the current view coordinate system or in the global coordinate system. If
	 * {@code isFocusLimitViewRelative() == true} then the distance is in
	 * current view coordinates. For example, a value of 100 means that spots
	 * will be visible up to 100 pixel widths from the view plane. Thus, the
	 * effective focus range depends on the current zoom level. If
	 * {@code isFocusLimitViewRelative() == false} then the distance is in
	 * global coordinates. A value of 100 means that spots will be visible up to
	 * 100 units (of the global coordinate system) from the view plane.
	 *
	 * <p>
	 * Ellipsoids are drawn increasingly translucent the closer they are to
	 * {@link #focusLimit}. See {@link #ellipsoidFadeDepth}.
	 */
	private double focusLimit;

	/**
	 * Whether the {@link #focusLimit} is relative to the the current
	 * view coordinate system.
	 *
	 * <p>
	 * If {@code true} then the distance is in current view coordinates. For
	 * example, a value of 100 means that spots will be visible up to 100 pixel
	 * widths from the view plane. Thus, the effective focus range depends on
	 * the current zoom level. If {@code false} then the distance is in global
	 * coordinates. A value of 100 means that spots will be visible up to 100
	 * units (of the global coordinate system) from the view plane.
	 */
	private boolean isFocusLimitViewRelative;

	/**
	 * The ratio of {@link #focusLimit} at which ellipsoids start to
	 * fade. Ellipsoids are drawn increasingly translucent the closer they are
	 * to {@link #focusLimit}. Up to ratio {@link #ellipsoidFadeDepth}
	 * they are fully opaque, then their alpha value goes to 0 linearly.
	 */
	private double ellipsoidFadeDepth;

	/**
	 * The ratio of {@link #focusLimit} at which points start to
	 * fade. Points are drawn increasingly translucent the closer they are
	 * to {@link #focusLimit}. Up to ratio {@link #pointFadeDepth}
	 * they are fully opaque, then their alpha value goes to 0 linearly.
	 */
	private double pointFadeDepth;

	/**
	 * The stroke used to paint the spot outlines.
	 */
	private Stroke defaultVertexStroke;

	/**
	 * The stroke used to paint the selected spot outlines.
	 */
	private Stroke highlightedVertexStroke;

	/**
	 * The stroke used to paint the focused spot outlines.
	 */
	private Stroke focusedVertexStroke;

	/**
	 * The stroke used to paint links.
	 */
	private Stroke defaultEdgeStroke;

	/**
	 * The stroke used to paint highlighted links.
	 */
	private Stroke highlightedEdgeStroke;

	/**
	 * The first color to paint links. The actual color of edges is interpolated
	 * from {@link #color1} to {@link #color2} along time.
	 */
	private Color color1;

	/**
	 * The second color to paint edges. The actual color of edges is
	 * interpolated from {@link #color1} to {@link #color2} along time.
	 */
	private Color color2;

	/**
	 * Whether do draw link arrow heads.
	 */
	private boolean drawLinkArrows;

	/**
	 * Return signed distance of p to z=0 plane, truncated at cutoff and scaled
	 * by 1/cutoff. A point on the plane has d=0. A Point that is at cutoff or
	 * farther behind the plane has d=1. A point that is at -cutoff or more in
	 * front of the plane has d=-1.
	 */
	private static double sliceDistance( final double z, final double cutoff )
	{
		if ( z > 0 )
			return Math.min( z, cutoff ) / cutoff;
		else
			return Math.max( z, -cutoff ) / cutoff;
	}

	/**
	 * Return signed distance of timepoint t to t0, truncated at cutoff and
	 * scaled by 1/cutoff. t=t0 has d=0. t&lt;=t0-cutoff has d=-1.
	 * t=&gt;t0+cutoff has d=1.
	 */
	private static double timeDistance( final double t, final double t0, final double cutoff )
	{
		final double d = t - t0;
		if ( d > 0 )
			return Math.min( d, cutoff ) / cutoff;
		else
			return Math.max( d, -cutoff ) / cutoff;
	}

	private static int trunc255( final int i )
	{
		return Math.min( 255, Math.max( 0, i ) );
	}

	private static int truncRGBA( final int r, final int g, final int b, final int a )
	{
		return ARGBType.rgba(
				trunc255( r ),
				trunc255( g ),
				trunc255( b ),
				trunc255( a ) );
	}

	private static int truncRGBA( final double r, final double g, final double b, final double a )
	{
		return truncRGBA(
				( int ) ( 255 * r ),
				( int ) ( 255 * g ),
				( int ) ( 255 * b ),
				( int ) ( 255 * a ) );
	}

	/**
	 * TODO
	 *
	 * @param sd sliceDistande, between -1 and 1. see {@link #sliceDistance(double, double)}.
	 * @param td timeDistande, between -1 and 1. see {@link #timeDistance(double, double, double)}.
	 * @param sdFade between 0 and 1, from which |sd| value color starts to fade (alpha value decreases).
	 * @param tdFade between 0 and 1, from which |td| value color starts to fade (alpha value decreases).
	 * @param isSelected whether to use selected or un-selected color scheme.
	 * @return vertex/edge color.
	 */
	private static Color getColor( final double sd, final double td, final double sdFade, final double tdFade, final boolean isSelected, final Color color1, final Color color2 )
	{
		/*
		 * |sf| = {                  0  for  |sd| <= sdFade,
		 *          linear from 0 to 1  for  |sd| = sdFade to |sd| = 1 }
		 *
		 * sgn(sf) = sgn(sd)
		 */
		final double sf;
		if ( sd > 0 )
		{
			sf = Math.max( 0, ( sd - sdFade ) / ( 1 - sdFade ) );
		}
		else
		{
			sf = -Math.max( 0, ( -sd - sdFade ) / ( 1 - sdFade ) );
		}

		final double tf;
		if ( td > 0 )
		{
			tf = Math.max( 0, ( td - tdFade ) / ( 1 - tdFade ) );
		}
		else
		{
			tf = -Math.max( 0, ( -td - tdFade ) / ( 1 - tdFade ) );
		}

		final double r1 = color1.getRed() / 255.;
		final double r2 = color2.getRed() / 255.;
		final double g1 = color1.getGreen() / 255.;
		final double g2 = color2.getGreen() / 255.;
		final double b1 = color1.getBlue() / 255.;
		final double b2 = color2.getBlue() / 255.;

		final double a = r1 + ( -2 * td ) * ( r2 - r1 );
		final double b = g1 + ( -2 * td ) * ( g2 - g1 );
		final double c = b1 + ( -2 * td ) * ( b2 - b1 );
		final double r = isSelected ? b : a;
		final double g = isSelected ? a : b;
		return new Color( truncRGBA( r, g, c, ( 1 + tf ) * ( 1 - Math.abs( sf ) ) ), true );
	}

	/**
	 * Gets the {@link ConvexPolytope} described by the specified interval in
	 * viewer coordinates, transformed to global coordinates.
	 *
	 * @param transform
	 *            the transform to transform viewer coordinates back in global
	 *            coordinates.
	 * @param viewerInterval
	 *            the view interval.
	 * @return {@code viewerInterval} transformed to global coordinates.
	 */
	// TODO: move to Utility class
	public static ConvexPolytope getPolytopeGlobal(
			final AffineTransform3D transform,
			final RealInterval viewerInterval )
	{
		return getPolytopeGlobal( transform,
				viewerInterval.realMin( 0 ), viewerInterval.realMax( 0 ),
				viewerInterval.realMin( 1 ), viewerInterval.realMax( 1 ),
				viewerInterval.realMin( 2 ), viewerInterval.realMax( 2 ) );
	}

	/**
	 * Gets the {@link ConvexPolytope} described by the specified interval in
	 * viewer coordinates, transformed to global coordinates.
	 *
	 * @param transform
	 *            the transform to transform viewer coordinates back in global
	 *            coordinates.
	 * @param viewerMinX
	 *            the x min bound of the view interval.
	 * @param viewerMaxX
	 *            the x max bound of the view interval.
	 * @param viewerMinY
	 *            the y min bound of the view interval.
	 * @param viewerMaxY
	 *            the y max bound of the view interval.
	 * @param viewerMinZ
	 *            the z min bound of the view interval.
	 * @param viewerMaxZ
	 *            the z max bound of the view interval.
	 * @return the specified viewer interval, transformed to global coordinates.
	 */
	// TODO: move to Utility class
	public static ConvexPolytope getPolytopeGlobal(
			final AffineTransform3D transform,
			final double viewerMinX, final double viewerMaxX,
			final double viewerMinY, final double viewerMaxY,
			final double viewerMinZ, final double viewerMaxZ )
	{
		final ConvexPolytope polytopeViewer = new ConvexPolytope(
				new HyperPlane(  1,  0,  0, viewerMinX ),
				new HyperPlane( -1,  0,  0, -viewerMaxX ),
				new HyperPlane(  0,  1,  0, viewerMinY ),
				new HyperPlane(  0, -1,  0, -viewerMaxY ),
				new HyperPlane(  0,  0,  1, viewerMinZ),
				new HyperPlane(  0,  0, -1, -viewerMaxZ ) );
		final ConvexPolytope polytopeGlobal = ConvexPolytope.transform( polytopeViewer, transform.inverse() );
		return polytopeGlobal;
	}

	/**
	 * Get the {@link ConvexPolytope} around the specified viewer coordinate
	 * range that is large enough border to ensure that it contains center of
	 * every ellipsoid touching the specified coordinate range.
	 *
	 * @param xMin
	 *            minimum X position on the z=0 plane in viewer coordinates.
	 * @param xMax
	 *            maximum X position on the z=0 plane in viewer coordinates.
	 * @param yMin
	 *            minimum Y position on the z=0 plane in viewer coordinates.
	 * @param yMax
	 *            maximum Y position on the z=0 plane in viewer coordinates.
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	private ConvexPolytope getOverlappingPolytopeGlobal(
			final double xMin,
			final double xMax,
			final double yMin,
			final double yMax,
			final AffineTransform3D transform,
			final int timepoint )
	{
		final double globalToViewerScale = Affine3DHelpers.extractScale( transform, 0 );
		final double maxDepth = isFocusLimitViewRelative
				? focusLimit
				: focusLimit * globalToViewerScale;
		final double border = globalToViewerScale * Math.sqrt( graph.getMaxBoundingSphereRadiusSquared( timepoint ) );
		return getPolytopeGlobal( transform,
				xMin - border, xMax + border,
				yMin - border, yMax + border,
				-maxDepth - border, maxDepth + border );
	}

	/**
	 * TODO
	 * @return
	 */
	private AffineTransform3D getRenderTransformCopy()
	{
		final AffineTransform3D transform = new AffineTransform3D();
		synchronized ( renderTransform )
		{
			transform.set( renderTransform );
		}
		return transform;
	}

	/**
	 * Get the {@link ConvexPolytope} bounding the visible region of global
	 * space, extended by a large enough border to ensure that it contains
	 * center of every ellipsoid that intersects the visible volume.
	 *
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	ConvexPolytope getVisiblePolytopeGlobal(
			final AffineTransform3D transform,
			final int timepoint )
	{
		return getOverlappingPolytopeGlobal( 0, width, 0, height, transform, timepoint );
	}

	/**
	 * Get the {@link ConvexPolytope} around the specified viewer coordinate
	 * that is large enough border to ensure that it contains center of every
	 * ellipsoid containing the specified coordinate.
	 *
	 * @param x
	 *            position on the z=0 plane in viewer coordinates.
	 * @param y
	 *            position on the z=0 plane in viewer coordinates.
	 * @param transform
	 * @param timepoint
	 * @return
	 */
	private ConvexPolytope getSurroundingPolytopeGlobal(
			final double x,
			final double y,
			final AffineTransform3D transform,
			final int timepoint )
	{
		return getOverlappingPolytopeGlobal( x, x, y, y, transform, timepoint );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;
		final FontMetrics fontMetrics = graphics.getFontMetrics();
		final int extraFontHeight = fontMetrics.getAscent() / 2;
		final int extraFontWidth = fontMetrics.charWidth( ' ' ) / 2;

		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final double maxDepth = isFocusLimitViewRelative
				? focusLimit
				: focusLimit * Affine3DHelpers.extractScale( transform, 0 );


		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialiasing );

		final V target = graph.vertexRef();
		final V ref1 = graph.vertexRef();
		final V ref2 = graph.vertexRef();
		final E ref3 = graph.edgeRef();
		final double[] gPos = new double[ 3 ];
		final double[] lPos = new double[ 3 ];

		final double sliceDistanceFade = ellipsoidFadeDepth;
		final double timepointDistanceFade = 0.5;

		final ScreenVertexMath screenVertexMath = new ScreenVertexMath();

		index.readLock().lock();
		try
		{
			if ( drawLinks )
			{
				final E highlighted = highlight.getHighlightedEdge( ref3 );

				graphics.setStroke( defaultEdgeStroke );

				for ( int t = Math.max( 0, currentTimepoint - timeLimit ); t < currentTimepoint; ++t )
				{
					final SpatialIndex< V > si = index.getSpatialIndex( t );
					final ClipConvexPolytope< V > ccp = si.getClipConvexPolytope();
					ccp.clip( getVisiblePolytopeGlobal( transform, t ) );
					for ( final V vertex : ccp.getInsideValues() )
					{
						vertex.localize( gPos );
						transform.apply( gPos, lPos );
						final int x0 = ( int ) lPos[ 0 ];
						final int y0 = ( int ) lPos[ 1 ];
						final double z0 = lPos[ 2 ];
						for ( final E edge : vertex.outgoingEdges() )
						{
							final boolean isHighlighted = edge.equals( highlighted );

							edge.getTarget( target );
							target.localize( gPos );
							transform.apply( gPos, lPos );
							final int x1 = ( int ) lPos[ 0 ];
							final int y1 = ( int ) lPos[ 1 ];

							final double z1 = lPos[ 2 ];

							final double td0 = timeDistance( t, currentTimepoint, timeLimit );
							final double td1 = timeDistance( t + 1, currentTimepoint, timeLimit );
							final double sd0 = sliceDistance( z0, maxDepth );
							final double sd1 = sliceDistance( z1, maxDepth );

							if ( td0 > -1 )
							{
								if ( ( sd0 > -1 && sd0 < 1 ) || ( sd1 > -1 && sd1 < 1 ) )
								{
									final Color c1 = getColor( sd1, td1, sliceDistanceFade, timepointDistanceFade, edge.isSelected(), color1, color2 );
									if ( useGradient )
									{
										final Color c0 = getColor( sd0, td0, sliceDistanceFade, timepointDistanceFade, edge.isSelected(), color1, color2 );
										graphics.setPaint( new GradientPaint( x0, y0, c0, x1, y1, c1 ) );
									}
									else
									{
										graphics.setPaint( c1 );
									}
									if ( isHighlighted )
										graphics.setStroke( highlightedEdgeStroke );
									graphics.drawLine( x0, y0, x1, y1 );

									// Draw arrows for edge direction.
									if ( drawLinkArrows )
									{
										final double dx = x1 - x0;
										final double dy = y1 - y0;
										final double alpha = Math.atan2( dy, dx );
										final double l = 5;
										final double theta = Math.PI / 6.;
										final int x1a = ( int ) Math.round( x1 - l * Math.cos( alpha - theta ) );
										final int x1b = ( int ) Math.round( x1 - l * Math.cos( alpha + theta ) );
										final int y1a = ( int ) Math.round( y1 - l * Math.sin( alpha - theta ) );
										final int y1b = ( int ) Math.round( y1 - l * Math.sin( alpha + theta ) );
										graphics.drawLine( x1, y1, x1a, y1a );
										graphics.drawLine( x1, y1, x1b, y1b );
									}

									if ( isHighlighted )
										graphics.setStroke( defaultEdgeStroke );
								}
							}
						}
					}
				}
			}

			if ( drawSpots )
			{
				final V highlighted = highlight.getHighlightedVertex( ref1 );
				final V focused = focus.getFocusedVertex( ref2 );

				graphics.setStroke( defaultVertexStroke );
				final AffineTransform torig = graphics.getTransform();

				final SpatialIndex< V > si = index.getSpatialIndex( currentTimepoint );
				final ClipConvexPolytope< V > ccp = si.getClipConvexPolytope();
				ccp.clip( getVisiblePolytopeGlobal( transform, currentTimepoint ) );
				for ( final V vertex : ccp.getInsideValues() )
				{
					final boolean isHighlighted = vertex.equals( highlighted );
					final boolean isFocused = vertex.equals( focused );

					screenVertexMath.init( vertex, transform );

					final double x = screenVertexMath.getViewPos()[ 0 ];
					final double y = screenVertexMath.getViewPos()[ 1 ];
					final double z = screenVertexMath.getViewPos()[ 2 ];
					final double sd = sliceDistance( z, maxDepth );

					if ( drawEllipsoidSliceIntersection )
					{
						if ( screenVertexMath.intersectsViewPlane() )
						{
							final double[] tr = screenVertexMath.getIntersectCenter();
							final double theta = screenVertexMath.getIntersectTheta();
							final Ellipse2D ellipse = screenVertexMath.getIntersectEllipse();

							graphics.translate( tr[ 0 ], tr[ 1 ] );
							graphics.rotate( theta );
							graphics.setColor( getColor( 0, 0, ellipsoidFadeDepth, timepointDistanceFade, vertex.isSelected(), color1, color2 ) );
							if ( isHighlighted )
								graphics.setStroke( highlightedVertexStroke );
							else if ( isFocused )
								graphics.setStroke( focusedVertexStroke );
							graphics.draw( ellipse );
							if ( isHighlighted || isFocused )
								graphics.setStroke( defaultVertexStroke );

							if ( !drawEllipsoidSliceProjection && drawSpotLabels )
							{
								// TODO Don't use ellipse, which is an AWT
								// object, for calculation.
								graphics.rotate( -theta );
								final double a = ellipse.getWidth();
								final double b = ellipse.getHeight();
								final double cos = Math.cos( theta );
								final double sin = Math.sin( theta );
								final double l = Math.sqrt( a * a * cos * cos + b * b * sin * sin );
								final float xl = ( float ) l / 2 + extraFontWidth;
								final float yl = extraFontHeight;
								graphics.drawString( vertex.getLabel(), xl, yl );
							}

							graphics.setTransform( torig );
						}
					}

					if ( sd > -1 && sd < 1 )
					{
						if ( drawEllipsoidSliceProjection )
						{
							final double[] tr = screenVertexMath.getProjectCenter();
							final double theta = screenVertexMath.getProjectTheta();
							final Ellipse2D ellipse = screenVertexMath.getProjectEllipse();

							graphics.translate( tr[ 0 ], tr[ 1 ] );
							graphics.rotate( theta );
							graphics.setColor( getColor( sd, 0, ellipsoidFadeDepth, timepointDistanceFade, vertex.isSelected(), color1, color2 ) );
							if ( isHighlighted )
								graphics.setStroke( highlightedVertexStroke );
							else if ( isFocused )
								graphics.setStroke( focusedVertexStroke );
							graphics.draw( ellipse );
							if ( isHighlighted || isFocused )
								graphics.setStroke( defaultVertexStroke );

							if ( drawSpotLabels )
							{
								// TODO Don't use ellipse, which is an AWT
								// object, for calculation.
								graphics.rotate( -theta );
								final double a = ellipse.getWidth();
								final double b = ellipse.getHeight();
								final double cos = Math.cos( theta );
								final double sin = Math.sin( theta );
								final double l = Math.sqrt( a * a * cos * cos + b * b * sin * sin );
								final float xl = ( float ) l / 2 + extraFontWidth;
								final float yl = extraFontHeight;
								graphics.drawString( vertex.getLabel(), xl, yl );
							}

							graphics.setTransform( torig );
						}

						// TODO: use simplified drawPointMaybe and drawPointAlways from getVisibleVertices()
						final boolean drawPoint = drawPoints && ( ( !drawEllipsoidSliceIntersection && !drawEllipsoidSliceProjection )
								|| drawPointsForEllipses
								|| ( drawEllipsoidSliceIntersection && !screenVertexMath.intersectsViewPlane() ) );
						if ( drawPoint )
						{
							graphics.setColor( getColor( sd, 0, pointFadeDepth, timepointDistanceFade, vertex.isSelected(), color1, color2 ) );
							double radius = pointRadius;
							if ( isHighlighted || isFocused )
								radius *= 2;
							final int ox = ( int ) ( x - radius );
							final int oy = ( int ) ( y - radius );
							final int ow = ( int ) ( 2 * radius );
							if ( isFocused )
								graphics.fillRect( ox, oy, ow, ow );
							else
								graphics.fillOval( ox, oy, ow, ow );
						}
					}
				}
			}
		}
		finally
		{
			index.readLock().unlock();
		}
		graph.releaseRef( target );
		graph.releaseRef( ref1 );
		graph.releaseRef( ref2 );
	}

	public E getEdgeAt( final int x, final int y, final double tolerance, final E ref )
	{
		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final ConvexPolytope visiblePolytopeGlobal = getVisiblePolytopeGlobal( transform, currentTimepoint );

		boolean found = false;
		index.readLock().lock();
		try
		{
			final double[] lPosT = new double[ 3 ];
			final double[] gPosT = new double[ 3 ];
			final double[] lPosS = new double[ 3 ];
			final double[] gPosS = new double[ 3 ];
			final V vertexRef = graph.vertexRef();
			double bestDist = tolerance;

			for ( int t = Math.max( 0, currentTimepoint - timeLimit ); t < currentTimepoint; ++t )
			{
				final SpatialIndex< V > si = index.getSpatialIndex( t );
				final ClipConvexPolytope< V > ccp = si.getClipConvexPolytope();
				ccp.clip( visiblePolytopeGlobal );
				for ( final V source : ccp.getInsideValues() )
				{
					source.localize( gPosS );
					transform.apply( gPosS, lPosS );
					final double x1 = lPosS[ 0 ];
					final double y1 = lPosS[ 1 ];
					for ( final E edge : source.outgoingEdges() )
					{
						final V target = edge.getTarget( vertexRef );
						target.localize( gPosT );
						transform.apply( gPosT, lPosT );
						final double x2 = lPosT[ 0 ];
						final double y2 = lPosT[ 1 ];
						final double dist = Util.segmentDist( x, y, x1, y1, x2, y2 );
						if ( dist <= bestDist )
						{
							bestDist = dist;
							ref.refTo( edge );
							found = true;
						}
					}
				}
			}
			graph.releaseRef( vertexRef );
		}
		finally
		{
			index.readLock().unlock();
		}
		return found ? ref : null;
	}

	/**
	 * Transform viewer coordinates to global (world) coordinates.
	 *
	 * @param x
	 *            viewer X coordinate
	 * @param y
	 *            viewer Y coordinate
	 * @param gPos
	 *            receives global coordinates corresponding to viewer
	 *            coordinates <em>(x, y, 0)</em>.
	 */
	public void getGlobalPosition( final int x, final int y, final double[] gPos )
	{
		synchronized ( renderTransform )
		{
			renderTransform.applyInverse( gPos, new double[] { x, y, 0 } );
		}
	}

	/**
	 * Transform global (world) coordinates to viewer coordinates.
	 *
	 * @param gPos
	 *            global coordinates to transform.
	 * @param vPos
	 *            receives the viewer coordinates.
	 */
	public void getViewerPosition( final double[] gPos, final double[] vPos )
	{
		synchronized ( renderTransform )
		{
			renderTransform.apply( gPos, vPos );
		}
	}

	public int getCurrentTimepoint()
	{
		return renderTimepoint;
	}

	public V getVertexAt( final int x, final int y, final double tolerance, final V ref )
	{
		final AffineTransform3D transform = getRenderTransformCopy();
		final int currentTimepoint = renderTimepoint;

		final double[] lPos = new double[] { x, y, 0 };
		final double[] gPos = new double[ 3 ];
		final ScreenVertexMath svm = new ScreenVertexMath();
		transform.applyInverse( gPos, lPos );

		boolean found = false;

		index.readLock().lock();

		if ( drawEllipsoidSliceProjection )
		{
			final ConvexPolytope cropPolytopeGlobal = getSurroundingPolytopeGlobal( x, y, transform, currentTimepoint );
			final ClipConvexPolytope< V > ccp = index.getSpatialIndex( currentTimepoint ).getClipConvexPolytope();
			ccp.clip( cropPolytopeGlobal );

			final double[] xy = new double[] { x, y };
			final double[] vPos = new double[ 3 ];
			double minDist = Double.MAX_VALUE;
			for ( final V v : ccp.getInsideValues() )
			{
				svm.init( v, transform );
				if ( svm.projectionContainsView( xy ) )
				{
					found = true;
					v.localize( vPos );
					final double d = LinAlgHelpers.squareDistance( vPos, gPos );
					if ( d < minDist )
					{
						minDist = d;
						ref.refTo( v );
					}
				}
			}
		}

		if ( !found )
		{
			final NearestNeighborSearch< V > nns = index.getSpatialIndex( currentTimepoint ).getNearestNeighborSearch();
			nns.search( RealPoint.wrap( gPos ) );
			final V v = nns.getSampler().get();
			if ( v != null )
			{
				svm.init( v, transform );
				if ( drawEllipsoidSliceIntersection )
				{
					if ( svm.containsGlobal( gPos ) )
					{
						found = true;
						ref.refTo( v );
					}
				}
				if ( !found && drawPoints )
				{
					final double[] p = svm.getViewPos();
					final double dx = p[ 0 ] - x;
					final double dy = p[ 1 ] - y;
					final double dr = pointRadius + tolerance;
					if ( dx * dx + dy * dy <= dr * dr )
					{
						found = true;
						ref.refTo( v );
					}
				}
			}
		}

		index.readLock().unlock();

		return found ? ref : null;
	}

	/**
	 * Get all vertices that would be visible with the current display settings
	 * and the specified {@code transform} and {@code timepoint}. This is used
	 * to compute {@link OverlayContext}.
	 * <p>
	 * Note, that it doesn't lock the {@link SpatioTemporalIndex}: we assumed,
	 * that this is already done by the caller.
	 * <p>
	 * TODO: The above means that the index is locked for longer than
	 * necessary.Revisit this and once it is clear how contexts are used in
	 * practice.
	 *
	 * @param transform
	 * @param timepoint
	 * @return vertices that would be visible with the current display settings
	 *         and the specified {@code transform} and {@code timepoint}.
	 */
	RefCollection< V > getVisibleVertices( final AffineTransform3D transform, final int timepoint )
	{
		final RefList< V > contextList = RefCollections.createRefList( graph.vertices() );
		final double maxDepth = isFocusLimitViewRelative
				? focusLimit
				: focusLimit * Affine3DHelpers.extractScale( transform, 0 );
		final boolean drawPointAlways = drawPoints
				&& ( ( !drawEllipsoidSliceIntersection && !drawEllipsoidSliceProjection )
						|| drawPointsForEllipses );
		final boolean drawPointMaybe = drawPoints
				&& !drawEllipsoidSliceProjection && drawEllipsoidSliceIntersection;
		final ScreenVertexMath svm = new ScreenVertexMath();

		final ClipConvexPolytope< V > ccp = index.getSpatialIndex( timepoint ).getClipConvexPolytope();
		final ConvexPolytope visiblePolytope = getVisiblePolytopeGlobal( transform, timepoint );
		ccp.clip( visiblePolytope );
		for ( final V vertex : ccp.getInsideValues() )
		{
			svm.init( vertex, transform );

			if ( drawEllipsoidSliceIntersection )
			{
				if ( svm.intersectsViewPlane()
						&& svm.intersectionIntersectsViewInterval( 0, width, 0, height ) )
				{
					contextList.add( vertex );
					continue;
				}
			}

			final double z = svm.getViewPos()[ 2 ];
			final double sd = sliceDistance( z, maxDepth );
			if ( -1 < sd && sd < 1 )
			{
				if ( drawEllipsoidSliceProjection
						&& svm.projectionIntersectsViewInterval( 0, width, 0, height ) )
				{
					contextList.add( vertex );
					continue;
				}

				if ( drawPointAlways || ( drawPointMaybe && !svm.intersectsViewPlane() ) )
				{
					final double x = svm.getViewPos()[ 0 ];
					final double y = svm.getViewPos()[ 1 ];
					if ( 0 <= x && x <= width && 0 <= y && y <= height )
						contextList.add( vertex );
				}
			}
		}

		return contextList;
	}
}
