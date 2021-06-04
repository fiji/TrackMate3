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
package org.mastodon.views.bdv.overlay;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.mastodon.app.ui.settings.style.Style;
import org.scijava.listeners.Listeners;

public class RenderSettings implements Style< RenderSettings >
{
	/*
	 * PUBLIC DISPLAY CONFIG DEFAULTS.
	 */

	public static final int DEFAULT_LIMIT_TIME_RANGE = 20;
	public static final double DEFAULT_LIMIT_FOCUS_RANGE = 100.;
	public static final boolean DEFAULT_USE_ANTI_ALIASING = true;
	public static final boolean DEFAULT_USE_GRADIENT = false;
	public static final boolean DEFAULT_DRAW_SPOTS = true;
	public static final boolean DEFAULT_DRAW_LINKS = true;
	public static final boolean DEFAULT_DRAW_LINKS_AHEAD_IN_TIME = false;
	public static final boolean DEFAULT_DRAW_ARROW_HEADS = false;
	public static final boolean DEFAULT_DRAW_ELLIPSE = true;
	public static final boolean DEFAULT_DRAW_SLICE_INTERSECTION = true;
	public static final boolean DEFAULT_DRAW_SLICE_PROJECTION = !DEFAULT_DRAW_SLICE_INTERSECTION;
	public static final boolean DEFAULT_DRAW_POINTS = !DEFAULT_DRAW_ELLIPSE || (DEFAULT_DRAW_ELLIPSE && DEFAULT_DRAW_SLICE_INTERSECTION);
	public static final boolean DEFAULT_DRAW_POINTS_FOR_ELLIPSE = false;
	public static final boolean DEFAULT_DRAW_SPOT_LABELS = false;
	public static final boolean DEFAULT_IS_FOCUS_LIMIT_RELATIVE = true;
	public static final double DEFAULT_ELLIPSOID_FADE_DEPTH = 0.2;
	public static final double DEFAULT_POINT_FADE_DEPTH = 0.;
	public static final int DEFAULT_COLOR_SPOT_AND_PRESENT = Color.GREEN.getRGB();
	public static final int DEFAULT_COLOR_PAST = Color.RED.getRGB();
	public static final int DEFAULT_COLOR_FUTURE = Color.BLUE.getRGB();

	public interface UpdateListener
	{
		public void renderSettingsChanged();
	}

	private final Listeners.List< UpdateListener > updateListeners;

	private RenderSettings()
	{
		updateListeners = new Listeners.SynchronizedList<>();
	}

	/**
	 * Returns a new render settings, copied from this instance.
	 *
	 * @param name
	 *            the name for the copied render settings.
	 * @return a new {@link RenderSettings} instance.
	 */
	@Override
	public RenderSettings copy( final String name )
	{
		final RenderSettings rs = new RenderSettings();
		rs.set( this );
		if ( name != null )
			rs.setName( name );
		return rs;
	}

	@Override
	public RenderSettings copy()
	{
		return copy( null );
	}

	public synchronized void set( final RenderSettings settings )
	{
		name = settings.name;
		useAntialiasing = settings.useAntialiasing;
		useGradient = settings.useGradient;
		timeLimit = settings.timeLimit;
		drawLinks = settings.drawLinks;
		drawLinksAheadInTime = settings.drawLinksAheadInTime;
		drawArrowHeads = settings.drawArrowHeads;
		drawSpots = settings.drawSpots;
		drawEllipsoidSliceProjection = settings.drawEllipsoidSliceProjection;
		drawEllipsoidSliceIntersection = settings.drawEllipsoidSliceIntersection;
		drawPoints = settings.drawPoints;
		drawPointsForEllipses = settings.drawPointsForEllipses;
		drawSpotLabels = settings.drawSpotLabels;
		focusLimit = settings.focusLimit;
		isFocusLimitViewRelative = settings.isFocusLimitViewRelative;
		ellipsoidFadeDepth = settings.ellipsoidFadeDepth;
		pointFadeDepth = settings.pointFadeDepth;
		colorSpot = settings.colorSpot;
		colorPast = settings.colorPast;
		colorFuture = settings.colorFuture;
		notifyListeners();
	}

	private void notifyListeners()
	{
		for ( final UpdateListener l : updateListeners.list )
			l.renderSettingsChanged();
	}

	public Listeners< UpdateListener > updateListeners()
	{
		return updateListeners;
	}

	/*
	 * DISPLAY SETTINGS FIELDS.
	 */

	/**
	 * The name of this render settings object.
	 */
	private String name;

	/**
	 * Whether to use antialiasing (for drawing everything).
	 */
	private boolean useAntialiasing;

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
	 */
	private boolean drawLinks;

	/**
	 * Whether to draw links ahead in time. They are otherwise drawn only
	 * backward in time.
	 */
	private boolean drawLinksAheadInTime;

	/**
	 * Whether to draw links with an arrow head, in time direction.
	 */
	private boolean drawArrowHeads;

	/**
	 * Whether to draw spots (at all).
	 */
	private boolean drawSpots;

	/**
	 * Whether to draw the projections of spot ellipsoids onto the view plane.
	 */
	private boolean drawEllipsoidSliceProjection;

	/**
	 * Whether to draw the intersections of spot ellipsoids with the view plane.
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
	 * The color used to paint spots and links in the current time-point.
	 */
	private int colorSpot;

	/**
	 * The color used to paint links in the past time-points.
	 */
	private int colorPast;

	/**
	 * The color used to paint links in the future time-points.
	 */
	private int colorFuture;

	/**
	 * Returns the name of this {@link RenderSettings}.
	 *
	 * @return the name.
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the name of this {@link RenderSettings}.
	 *
	 * @param name
	 *            the name to set.
	 */
	@Override
	public synchronized void setName( final String name )
	{
		if ( !Objects.equals( this.name, name ) )
		{
			this.name = name;
			notifyListeners();
		}
	}

	/**
	 * Get the antialiasing setting.
	 *
	 * @return {@code true} if antialiasing is used.
	 */
	public boolean getUseAntialiasing()
	{
		return useAntialiasing;
	}

	/**
	 * Sets whether to use anti-aliasing for drawing.
	 *
	 * @param useAntialiasing
	 *            whether to use use anti-aliasing.
	 */
	public synchronized void setUseAntialiasing( final boolean useAntialiasing )
	{
		if ( this.useAntialiasing != useAntialiasing )
		{
			this.useAntialiasing = useAntialiasing;
			notifyListeners();
		}
	}

	/**
	 * Returns whether a gradient is used for drawing links.
	 *
	 * @return {@code true} if links are drawn using a gradient from source
	 *         color to target color, or {@code false}, if links are drawn using
	 *         the target color.
	 */
	public boolean getUseGradient()
	{
		return useGradient;
	}

	/**
	 * Sets whether to use a gradient for drawing links. If
	 * {@code useGradient=true}, draw links using a gradient from source color
	 * to target color. If {@code useGradient=false}, draw links using the
	 * target color.
	 *
	 * @param useGradient
	 *            whether to use a gradient for drawing links.
	 */
	public synchronized void setUseGradient( final boolean useGradient )
	{
		if ( this.useGradient != useGradient )
		{
			this.useGradient = useGradient;
			notifyListeners();
		}
	}

	/**
	 * Gets the maximum number of time-points into the past for which outgoing
	 * edges should be drawn.
	 *
	 * @return maximum number of time-points into the past to draw links.
	 */
	public int getTimeLimit()
	{
		return timeLimit;
	}

	/**
	 * Sets the maximum number of time-points into the past for which outgoing
	 * edges should be drawn.
	 *
	 * @param timeLimit
	 *            maximum number of time-points into the past to draw links.
	 */
	public synchronized void setTimeLimit( final int timeLimit )
	{
		if ( this.timeLimit != timeLimit )
		{
			this.timeLimit = timeLimit;
			notifyListeners();
		}
	}

	/**
	 * Gets whether to draw links (at all). For specific settings, see
	 * {@link #getTimeLimit()}, {@link #getUseGradient()}.
	 *
	 * @return {@code true} if links are drawn.
	 */
	public boolean getDrawLinks()
	{
		return drawLinks;
	}

	/**
	 * Gets whether to draw links ahead in time. They are otherwise drawn only
	 * backward in time.
	 * 
	 * @return {@code true} if links are drawn ahead in time.
	 */
	public boolean getDrawLinksAheadInTime()
	{
		return drawLinksAheadInTime;
	}

	/**
	 * Gets whether to draw links with arrow heads.
	 *
	 * @return {@code true} if links are drawn with arrow heads.
	 */
	public boolean getDrawArrowHeads()
	{
		return drawArrowHeads;
	}

	/**
	 * Sets whether to draw links (at all). For specific settings, see
	 * {@link #setTimeLimit(int)}, {@link #setUseGradient(boolean)}.
	 *
	 * @param drawLinks
	 *            whether to draw links.
	 */
	public synchronized void setDrawLinks( final boolean drawLinks )
	{
		if ( this.drawLinks != drawLinks )
		{
			this.drawLinks = drawLinks;
			notifyListeners();
		}
	}

	/**
	 * Sets whether to draw links ahead in time. They are otherwise drawn only
	 * backward in time.
	 *
	 * @param drawLinksAheadInTime
	 *            whether to draw links ahead in time.
	 */
	public synchronized void setDrawLinksAheadInTime( final boolean drawLinksAheadInTime )
	{
		if ( this.drawLinksAheadInTime != drawLinksAheadInTime )
		{
			this.drawLinksAheadInTime = drawLinksAheadInTime;
			notifyListeners();
		}
	}

	/**
	 * Sets whether to draw links with arrow heads.
	 *
	 * @param drawArrowHeads
	 *            whether to draw links with arrow heads.
	 */
	public synchronized void setDrawArrowHeads( final boolean drawArrowHeads )
	{
		if ( this.drawArrowHeads != drawArrowHeads )
		{
			this.drawArrowHeads = drawArrowHeads;
			notifyListeners();
		}
	}

	/**
	 * Gets whether to draw spots (at all). For specific settings, see other
	 * spot drawing settings.
	 *
	 * @return {@code true} if spots are to be drawn.
	 * @see #getDrawEllipsoidSliceIntersection()
	 * @see #getDrawEllipsoidSliceProjection()
	 * @see #getDrawSpotCenters()
	 * @see #getDrawSpotCentersForEllipses()
	 * @see #getDrawSpotLabels()
	 * @see #getEllipsoidFadeDepth()
	 * @see #getFocusLimit()
	 * @see #getFocusLimitViewRelative()
	 * @see #getPointFadeDepth()
	 */
	public boolean getDrawSpots()
	{
		return drawSpots;
	}

	/**
	 * Sets whether to draw spots (at all). For specific settings, see other
	 * spot drawing settings.
	 *
	 * @param drawSpots
	 *            whether to draw spots.
	 * @see #setDrawEllipsoidSliceIntersection(boolean)
	 * @see #setDrawEllipsoidSliceProjection(boolean)
	 * @see #setDrawSpotCenters(boolean)
	 * @see #setDrawSpotCentersForEllipses(boolean)
	 * @see #setDrawSpotLabels(boolean)
	 * @see #setEllipsoidFadeDepth(double)
	 * @see #setFocusLimit(double)
	 * @see #setFocusLimitViewRelative(boolean)
	 * @see #setPointFadeDepth(double)
	 */
	public synchronized void setDrawSpots( final boolean drawSpots )
	{
		if ( this.drawSpots != drawSpots )
		{
			this.drawSpots = drawSpots;
			notifyListeners();
		}
	}

	/**
	 * Get whether the projections of spot ellipsoids onto the view plane are
	 * drawn.
	 *
	 * @return {@code true} iff projections of spot ellipsoids onto the view
	 *         plane are drawn.
	 */
	public boolean getDrawEllipsoidSliceProjection()
	{
		return drawEllipsoidSliceProjection;
	}

	/**
	 * Set whether to draw the projections of spot ellipsoids onto the view
	 * plane.
	 *
	 * @param drawEllipsoidSliceProjection
	 *            whether to draw projections of spot ellipsoids onto the view
	 *            plane.
	 */
	public synchronized void setDrawEllipsoidSliceProjection( final boolean drawEllipsoidSliceProjection )
	{
		if ( this.drawEllipsoidSliceProjection != drawEllipsoidSliceProjection )
		{
			this.drawEllipsoidSliceProjection = drawEllipsoidSliceProjection;
			notifyListeners();
		}
	}

	/**
	 * Get whether the intersections of spot ellipsoids with the view plane are
	 * drawn.
	 *
	 * @return {@code true} iff intersections of spot ellipsoids with the view
	 *         plane are drawn.
	 */
	public boolean getDrawEllipsoidSliceIntersection()
	{
		return drawEllipsoidSliceIntersection;
	}

	/**
	 * Set whether to draw the intersections of spot ellipsoids with the view
	 * plane.
	 *
	 * @param drawEllipsoidSliceIntersection
	 *            whether to draw intersections of spot ellipsoids with the view
	 *            plane.
	 */
	public synchronized void setDrawEllipsoidSliceIntersection( final boolean drawEllipsoidSliceIntersection )
	{
		if ( this.drawEllipsoidSliceIntersection != drawEllipsoidSliceIntersection )
		{
			this.drawEllipsoidSliceIntersection = drawEllipsoidSliceIntersection;
			notifyListeners();
		}
	}

	/**
	 * Get whether spot centers are drawn.
	 * <p>
	 * Note that spot centers are usually only drawn, if no ellipse for the spot
	 * was drawn (unless {@link #getDrawSpotCentersForEllipses()}
	 * {@code == true}).
	 *
	 * @return whether spot centers are drawn.
	 */
	public boolean getDrawSpotCenters()
	{
		return drawPoints;
	}

	/**
	 * Set whether spot centers are drawn.
	 * <p>
	 * Note that spot centers are usually only drawn, if no ellipse for the spot
	 * was drawn (unless {@link #getDrawSpotCentersForEllipses()}
	 * {@code == true}).
	 *
	 * @param drawPoints
	 *            whether spot centers are drawn.
	 */
	public synchronized void setDrawSpotCenters( final boolean drawPoints )
	{
		if ( this.drawPoints != drawPoints )
		{
			this.drawPoints = drawPoints;
			notifyListeners();
		}
	}

	/**
	 * Get whether spot centers are also drawn for those points that are visible
	 * as ellipses. See {@link #getDrawSpotCenters()}.
	 *
	 * @return whether spot centers are also drawn for those points that are
	 *         visible as ellipses.
	 */
	public boolean getDrawSpotCentersForEllipses()
	{
		return drawPointsForEllipses;
	}

	/**
	 * Set whether spot centers are also drawn for those points that are visible
	 * as ellipses.
	 *
	 * @param drawPointsForEllipses
	 *            whether spot centers are also drawn for those points that are
	 *            visible as ellipses.
	 */
	public synchronized void setDrawSpotCentersForEllipses( final boolean drawPointsForEllipses )
	{
		if ( this.drawPointsForEllipses != drawPointsForEllipses )
		{
			this.drawPointsForEllipses = drawPointsForEllipses;
			notifyListeners();
		}
	}

	/**
	 * Get whether spot labels are drawn next to ellipses.
	 *
	 * @return whether spot labels are drawn next to ellipses.
	 */
	public boolean getDrawSpotLabels()
	{
		return drawSpotLabels;
	}

	/**
	 * Set whether spot labels are drawn next to ellipses.
	 *
	 * @param drawSpotLabels
	 *            whether spot labels are drawn next to ellipses.
	 */
	public void setDrawSpotLabels( final boolean drawSpotLabels )
	{
		if ( this.drawSpotLabels != drawSpotLabels )
		{
			this.drawSpotLabels = drawSpotLabels;
			notifyListeners();
		}
	}

	/**
	 * Get the maximum distance from the view plane up to which to spots are
	 * drawn.
	 * <p>
	 * Depending on {@link #getFocusLimitViewRelative()}, the distance is either
	 * in the current view coordinate system or in the global coordinate system.
	 * If {@code getFocusLimitViewRelative() == true} then the distance is in
	 * current view coordinates. For example, a value of 100 means that spots
	 * will be visible up to 100 pixel widths from the view plane. Thus, the
	 * effective focus range depends on the current zoom level. If
	 * {@code getFocusLimitViewRelative() == false} then the distance is in
	 * global coordinates. A value of 100 means that spots will be visible up to
	 * 100 units (of the global coordinate system) from the view plane.
	 * <p>
	 * Ellipsoids are drawn increasingly translucent the closer they are to the
	 * {@code focusLimit}. See {@link #getEllipsoidFadeDepth()}.
	 *
	 * @return the maximum distance from the view plane up to which to spots are
	 *         drawn.
	 */
	public double getFocusLimit()
	{
		return focusLimit;
	}

	/**
	 * Set the maximum distance from the view plane up to which to spots are
	 * drawn. See {@link #getFocusLimit()}.
	 *
	 * @param focusLimit
	 *            the maximum distance from the view plane up to which to spots
	 *            are drawn.
	 */
	public synchronized void setFocusLimit( final double focusLimit )
	{
		if ( this.focusLimit != focusLimit )
		{
			this.focusLimit = focusLimit;
			notifyListeners();
		}
	}

	/**
	 * Set whether the {@link #getFocusLimit()} is relative to the the current
	 * view coordinate system.
	 * <p>
	 * If {@code true} then the distance is in current view coordinates. For
	 * example, a value of 100 means that spots will be visible up to 100 pixel
	 * widths from the view plane. Thus, the effective focus range depends on
	 * the current zoom level. If {@code false} then the distance is in global
	 * coordinates. A value of 100 means that spots will be visible up to 100
	 * units (of the global coordinate system) from the view plane.
	 *
	 * @return {@code true} iff the {@link #getFocusLimit()} is relative to the
	 *         the current view coordinate system.
	 */
	public boolean getFocusLimitViewRelative()
	{
		return isFocusLimitViewRelative;
	}

	/**
	 * Set whether the {@link #getFocusLimit()} is relative to the the current
	 * view coordinate system. See {@link #getFocusLimitViewRelative()}.
	 *
	 * @param isFocusLimitViewRelative
	 *            whether the {@link #getFocusLimit()} is relative to the the
	 *            current view coordinate system.
	 */
	public synchronized void setFocusLimitViewRelative( final boolean isFocusLimitViewRelative )
	{
		if ( this.isFocusLimitViewRelative != isFocusLimitViewRelative )
		{
			this.isFocusLimitViewRelative = isFocusLimitViewRelative;
			notifyListeners();
		}
	}

	/**
	 * Get the ratio of {@link #getFocusLimit()} at which ellipsoids start to
	 * fade. Ellipsoids are drawn increasingly translucent the closer they are
	 * to {@link #getFocusLimit()}. Up to ratio {@link #getEllipsoidFadeDepth()}
	 * they are fully opaque, then their alpha value goes to 0 linearly.
	 *
	 * @return the ratio of {@link #getFocusLimit()} at which ellipsoids start
	 *         to fade.
	 */
	public double getEllipsoidFadeDepth()
	{
		return ellipsoidFadeDepth;
	}

	/**
	 * Set the ratio of {@link #getFocusLimit()} at which ellipsoids start to
	 * fade. See {@link #getEllipsoidFadeDepth()}.
	 *
	 * @param ellipsoidFadeDepth
	 *            the ratio of {@link #getFocusLimit()} at which ellipsoids
	 *            start to fade.
	 */
	public synchronized void setEllipsoidFadeDepth( final double ellipsoidFadeDepth )
	{
		if ( this.ellipsoidFadeDepth != ellipsoidFadeDepth )
		{
			this.ellipsoidFadeDepth = ellipsoidFadeDepth;
			notifyListeners();
		}
	}

	/**
	 * The ratio of {@link #getFocusLimit()} at which points start to fade.
	 * Points are drawn increasingly translucent the closer they are to
	 * {@link #getFocusLimit()}. Up to ratio {@link #getPointFadeDepth} they are
	 * fully opaque, then their alpha value goes to 0 linearly.
	 *
	 * @return the ratio of {@link #getFocusLimit()} at which points start to
	 *         fade.
	 */
	public double getPointFadeDepth()
	{
		return pointFadeDepth;
	}

	/**
	 * Set the ratio of {@link #getFocusLimit()} at which points start to fade.
	 * See {@link #getPointFadeDepth()}.
	 *
	 * @param pointFadeDepth
	 *            the ratio of {@link #getFocusLimit()} at which points start to
	 *            fade.
	 */
	public synchronized void setPointFadeDepth( final double pointFadeDepth )
	{
		if ( this.pointFadeDepth != pointFadeDepth )
		{
			this.pointFadeDepth = pointFadeDepth;
			notifyListeners();
		}
	}

	/**
	 * Returns the color used to paint spots and links in the current
	 * time-point.
	 * 
	 * @return the color used to paint spots and links in the current
	 *         time-point.
	 */
	public int getColorSpot()
	{
		return colorSpot;
	}

	/**
	 * Sets the color used to paint spots and links in the current time-point.
	 * 
	 * @param colorSpot
	 *            the color used to paint spots and links in the current
	 *            time-point.
	 */
	public synchronized void setColorSpot( final int colorSpot )
	{
		if ( this.colorSpot != colorSpot )
		{
			this.colorSpot = colorSpot;
			notifyListeners();
		}
	}

	/**
	 * Returns the color used to paint links in the past time-points.
	 * 
	 * @return the color used to paint links in the past time-points.
	 */
	public int getColorPast()
	{
		return colorPast;
	}

	/**
	 * Sets the color used to paint links in the past time-points.
	 * 
	 * @param colorPast
	 *            the color used to paint links in the past time-points.
	 */
	public synchronized void setColorPast( final int colorPast )
	{
		if ( this.colorPast != colorPast )
		{
			this.colorPast = colorPast;
			notifyListeners();
		}
	}

	/**
	 * Returns the color used to paint links in the future time-points.
	 * 
	 * @return the color used to paint links in the future time-points.
	 */
	public int getColorFuture()
	{
		return colorFuture;
	}

	/**
	 * Sets the color used to paint links in the future time-points.
	 * 
	 * @param colorFuture
	 *            the color used to paint links in the future time-points.
	 */
	public synchronized void setColorFuture( final int colorFuture )
	{
		if ( this.colorFuture != colorFuture )
		{
			this.colorFuture = colorFuture;
			notifyListeners();
		}
	}

	/*
	 * DEFAULTS RENDER SETTINGS LIBRARY.
	 */

	private static final RenderSettings df;
	static
	{
		df = new RenderSettings();
		df.useAntialiasing = DEFAULT_USE_ANTI_ALIASING;
		df.useGradient = DEFAULT_USE_GRADIENT;
		df.timeLimit = DEFAULT_LIMIT_TIME_RANGE;
		df.drawLinks = DEFAULT_DRAW_LINKS;
		df.drawLinksAheadInTime = DEFAULT_DRAW_LINKS_AHEAD_IN_TIME;
		df.drawArrowHeads = DEFAULT_DRAW_ARROW_HEADS;
		df.drawSpots = DEFAULT_DRAW_SPOTS;
		df.drawEllipsoidSliceProjection = DEFAULT_DRAW_SLICE_PROJECTION;
		df.drawEllipsoidSliceIntersection = DEFAULT_DRAW_SLICE_INTERSECTION;
		df.drawPoints = DEFAULT_DRAW_POINTS;
		df.drawPointsForEllipses = DEFAULT_DRAW_POINTS_FOR_ELLIPSE;
		df.drawSpotLabels = DEFAULT_DRAW_SPOT_LABELS;
		df.focusLimit = DEFAULT_LIMIT_FOCUS_RANGE;
		df.isFocusLimitViewRelative = DEFAULT_IS_FOCUS_LIMIT_RELATIVE;
		df.ellipsoidFadeDepth = DEFAULT_ELLIPSOID_FADE_DEPTH;
		df.pointFadeDepth = DEFAULT_POINT_FADE_DEPTH;
		df.colorSpot = DEFAULT_COLOR_SPOT_AND_PRESENT;
		df.colorPast = DEFAULT_COLOR_PAST;
		df.colorFuture = DEFAULT_COLOR_FUTURE;
		df.name = "Default";
	}

	private static final RenderSettings POINT_CLOUD;
	static
	{
		POINT_CLOUD = df.copy( "Point cloud" );
		POINT_CLOUD.drawLinks = false;
		POINT_CLOUD.drawEllipsoidSliceIntersection = false;
		POINT_CLOUD.isFocusLimitViewRelative = false;
	}

	private static final RenderSettings NONE;
	static
	{
		NONE = df.copy( "No overlay" );
		NONE.drawLinks = false;
		NONE.drawSpots = false;
	}

	public static final Collection< RenderSettings > defaults;
	static
	{
		defaults = new ArrayList<>( 4 );
		defaults.add( df );
		defaults.add( POINT_CLOUD );
		defaults.add( NONE );
	}

	public static RenderSettings defaultStyle()
	{
		return df;
	}
}
