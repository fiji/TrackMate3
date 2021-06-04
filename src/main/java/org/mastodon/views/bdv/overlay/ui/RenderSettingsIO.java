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
package org.mastodon.views.bdv.overlay.ui;

import static org.yaml.snakeyaml.DumperOptions.FlowStyle.FLOW;

import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mastodon.io.yaml.AbstractWorkaroundConstruct;
import org.mastodon.io.yaml.WorkaroundConstructor;
import org.mastodon.io.yaml.WorkaroundRepresent;
import org.mastodon.io.yaml.WorkaroundRepresenter;
import org.mastodon.views.bdv.overlay.RenderSettings;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Facilities to dump / load {@link RenderSettings} to / from a YAML file.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class RenderSettingsIO
{
	private static class RenderSettingsRepresenter extends WorkaroundRepresenter
	{
		public RenderSettingsRepresenter()
		{
			putRepresent( new RepresentBasicStroke( this ) );
			putRepresent( new RepresentRenderSettings( this ) );
		}
	}

	private static class RenderSettingsConstructor extends WorkaroundConstructor
	{
		public RenderSettingsConstructor()
		{
			super( Object.class );
			putConstruct( new ConstructBasicStroke( this ) );
			putConstruct( new ConstructRenderSettings( this ) );
		}
	}

	/**
	 * Returns a YAML instance that can dump / load a collection of
	 * {@link RenderSettings} to / from a .yaml file.
	 *
	 * @return a new YAML instance.
	 */
	static Yaml createYaml()
	{
		final DumperOptions dumperOptions = new DumperOptions();
		final Representer representer = new RenderSettingsRepresenter();
		final Constructor constructor = new RenderSettingsConstructor();
		final Yaml yaml = new Yaml( constructor, representer, dumperOptions );
		return yaml;
	}

	private static final Tag STROKE_TAG = new Tag( "!stroke" );

	private static class RepresentBasicStroke extends WorkaroundRepresent
	{
		public RepresentBasicStroke( final WorkaroundRepresenter r )
		{
			super( r, STROKE_TAG, BasicStroke.class );
		}

		@Override
		public Node representData( final Object data )
		{
			final BasicStroke s = ( BasicStroke ) data;
			final Map< String, Object > mapping = new LinkedHashMap<>();
			mapping.put( "width", s.getLineWidth() );
			mapping.put( "cap", s.getEndCap() );
			mapping.put( "join", s.getLineJoin() );
			mapping.put( "miterlimit", s.getMiterLimit() );
			ArrayList< Float > dash = null;
			final float[] dashArray = s.getDashArray();
			if ( dashArray != null )
			{
				dash = new ArrayList<>();
				for ( final float f : dashArray )
					dash.add( f );
			}
			mapping.put( "dash", dash );
			mapping.put( "dash_phase", s.getDashPhase() );
			final Node node = representMapping( getTag(), mapping, FLOW );
			return node;
		}
	}

	private static class ConstructBasicStroke extends AbstractWorkaroundConstruct
	{
		public ConstructBasicStroke( final WorkaroundConstructor c )
		{
			super( c, STROKE_TAG );
		}

		@Override
		public Object construct( final Node node )
		{
			try
			{
				final Map< Object, Object > mapping = constructMapping( ( MappingNode  ) node );
				final float width = ( ( Double ) mapping.get( "width" ) ).floatValue();
				final int cap = ( Integer ) mapping.get( "cap" );
				final int join = ( Integer ) mapping.get( "join" );
				final float miterlimit = ( ( Double ) mapping.get( "miterlimit" ) ).floatValue();
				@SuppressWarnings( "unchecked" )
				final List< Double > list = ( List< Double > ) mapping.get( "dash" );
				float[] dash = null;
				if ( list != null && !list.isEmpty() )
				{
					dash = new float[ list.size() ];
					int i = 0;
					for ( final double d : list )
						dash[ i++ ] = ( float ) d;
				}
				final float dash_phase = ( ( Double ) mapping.get( "dash_phase" ) ).floatValue();
				return new BasicStroke( width, cap, join, miterlimit, dash, dash_phase );
			}
			catch( final Exception e )
			{
				e.printStackTrace();
			}
			return null;
		}
	}

	private static final Tag RENDERSETTINGS_TAG = new Tag( "!bdvrendersettings" );

	private static class RepresentRenderSettings extends WorkaroundRepresent
	{
		public RepresentRenderSettings( final WorkaroundRepresenter r )
		{
			super( r, RENDERSETTINGS_TAG, RenderSettings.class );
		}

		@Override
		public Node representData( final Object data )
		{
			final RenderSettings s = ( RenderSettings ) data;
			final Map< String, Object > mapping = new LinkedHashMap< >();

			mapping.put( "name", s.getName() );

			mapping.put( "antialiasing", s.getUseAntialiasing() );
			mapping.put( "drawLinks", s.getDrawLinks() );
			mapping.put( "drawLinksAheadInTime", s.getDrawLinksAheadInTime() );
			mapping.put( "drawArrowHeads", s.getDrawArrowHeads() );
			mapping.put( "timeRangeForLinks", s.getTimeLimit() );
			mapping.put( "gradientForLinks", s.getUseGradient() );
			mapping.put( "drawSpots", s.getDrawSpots() );
			mapping.put( "drawEllipsoidIntersection", s.getDrawEllipsoidSliceIntersection() );
			mapping.put( "drawEllipsoidProjection", s.getDrawEllipsoidSliceProjection() );
			mapping.put( "drawSpotCenters", s.getDrawSpotCenters() );
			mapping.put( "drawSpotCentersForEllipses", s.getDrawSpotCentersForEllipses() );
			mapping.put( "drawSpotLabels", s.getDrawSpotLabels() );
			mapping.put( "focusLimit", s.getFocusLimit() );
			mapping.put( "focusLimitViewRelative", s.getFocusLimitViewRelative() );
			mapping.put( "ellipsoidFadeDepth", s.getEllipsoidFadeDepth() );
			mapping.put( "pointFadeDepth", s.getPointFadeDepth() );
			mapping.put( "colorSpot", s.getColorSpot() );
			mapping.put( "colorPast", s.getColorPast() );
			mapping.put( "colorFuture", s.getColorFuture() );
			

			final Node node = representMapping( getTag(), mapping, getDefaultFlowStyle() );
			return node;
		}
	}

	private static class ConstructRenderSettings extends AbstractWorkaroundConstruct
	{
		public ConstructRenderSettings( final WorkaroundConstructor c )
		{
			super( c, RENDERSETTINGS_TAG );
		}

		@Override
		public Object construct( final Node node )
		{
			try
			{
				final Map< Object, Object > mapping = constructMapping( ( MappingNode ) node );
				final String name = ( String ) mapping.get( "name" );
				final RenderSettings s = RenderSettings.defaultStyle().copy( name );

				s.setName( ( String ) mapping.getOrDefault( "name", "CouldNotFindName" ) );

				s.setUseAntialiasing( ( boolean ) mapping.getOrDefault( "antialiasing", RenderSettings.DEFAULT_USE_ANTI_ALIASING ) );
				s.setDrawLinks( ( boolean ) mapping.getOrDefault( "drawLinks", RenderSettings.DEFAULT_DRAW_LINKS ) );
				s.setDrawLinksAheadInTime( ( boolean ) mapping.getOrDefault( "drawLinksAheadInTime", RenderSettings.DEFAULT_DRAW_LINKS_AHEAD_IN_TIME ) );
				s.setDrawArrowHeads( ( boolean ) mapping.getOrDefault( "drawArrowHeads", RenderSettings.DEFAULT_DRAW_ARROW_HEADS ) );
				s.setTimeLimit( ( int ) mapping.getOrDefault( "timeRangeForLinks", RenderSettings.DEFAULT_LIMIT_TIME_RANGE ) );
				s.setUseGradient( ( boolean ) mapping.getOrDefault( "gradientForLinks", RenderSettings.DEFAULT_USE_GRADIENT ) );
				s.setDrawSpots( ( boolean ) mapping.getOrDefault( "drawSpots", RenderSettings.DEFAULT_DRAW_SPOTS ) );
				s.setDrawEllipsoidSliceIntersection( ( boolean ) mapping.getOrDefault( "drawEllipsoidIntersection", RenderSettings.DEFAULT_DRAW_SLICE_INTERSECTION ) );
				s.setDrawEllipsoidSliceProjection( ( boolean ) mapping.getOrDefault( "drawEllipsoidProjection", RenderSettings.DEFAULT_DRAW_SLICE_PROJECTION ) );
				s.setDrawSpotCenters( ( boolean ) mapping.getOrDefault( "drawSpotCenters", RenderSettings.DEFAULT_DRAW_POINTS ) );
				s.setDrawSpotCentersForEllipses( ( boolean ) mapping.getOrDefault( "drawSpotCentersForEllipses", RenderSettings.DEFAULT_DRAW_POINTS_FOR_ELLIPSE ) );
				s.setDrawSpotLabels( ( boolean ) mapping.getOrDefault( "drawSpotLabels", RenderSettings.DEFAULT_DRAW_SPOT_LABELS ) );
				s.setFocusLimit( ( double ) mapping.getOrDefault( "focusLimit", RenderSettings.DEFAULT_LIMIT_FOCUS_RANGE ) );
				s.setFocusLimitViewRelative( ( boolean ) mapping.getOrDefault( "focusLimitViewRelative", RenderSettings.DEFAULT_IS_FOCUS_LIMIT_RELATIVE ) );
				s.setEllipsoidFadeDepth( ( double ) mapping.getOrDefault( "ellipsoidFadeDepth", RenderSettings.DEFAULT_ELLIPSOID_FADE_DEPTH ) );
				s.setPointFadeDepth( ( double ) mapping.getOrDefault( "pointFadeDepth", RenderSettings.DEFAULT_POINT_FADE_DEPTH ) );
				s.setColorSpot( ( int ) mapping.getOrDefault( "colorSpot", RenderSettings.DEFAULT_COLOR_SPOT_AND_PRESENT ) );
				s.setColorPast( ( int ) mapping.getOrDefault( "colorPast", RenderSettings.DEFAULT_COLOR_PAST ) );
				s.setColorFuture( ( int ) mapping.getOrDefault( "colorFuture", RenderSettings.DEFAULT_COLOR_FUTURE ) );

				return s;
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
			return null;
		}
	}
}
