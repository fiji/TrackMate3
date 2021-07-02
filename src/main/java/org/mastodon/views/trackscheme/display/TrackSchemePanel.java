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
package org.mastodon.views.trackscheme.display;

import static org.mastodon.views.trackscheme.display.InertialScreenTransformEventHandler.boundXLayoutBorder;
import static org.mastodon.views.trackscheme.display.InertialScreenTransformEventHandler.boundYLayoutBorder;

import bdv.viewer.InteractiveDisplayCanvas;
import bdv.viewer.OverlayRenderer;
import bdv.viewer.TransformListener;
import bdv.viewer.render.PainterThread;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.UIManager;

import org.mastodon.graph.GraphChangeListener;
import org.mastodon.model.FocusListener;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightListener;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.NavigationListener;
import org.mastodon.model.SelectionListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.model.TimepointListener;
import org.mastodon.model.TimepointModel;
import org.mastodon.ui.NavigationEtiquette;
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextListener;
import org.mastodon.views.trackscheme.ContextLayout;
import org.mastodon.views.trackscheme.LineageTreeLayout;
import org.mastodon.views.trackscheme.ScreenEntities;
import org.mastodon.views.trackscheme.ScreenEntitiesInterpolator;
import org.mastodon.views.trackscheme.ScreenTransform;
import org.mastodon.views.trackscheme.TrackSchemeEdge;
import org.mastodon.views.trackscheme.TrackSchemeGraph;
import org.mastodon.views.trackscheme.TrackSchemeVertex;
import org.mastodon.views.trackscheme.display.TrackSchemeOptions.Values;
import org.mastodon.views.trackscheme.display.animate.AbstractAnimator;
import org.mastodon.views.trackscheme.display.style.TrackSchemeStyle;

public class TrackSchemePanel extends JPanel implements
		TransformListener< ScreenTransform >,
		PainterThread.Paintable,
		HighlightListener,
		FocusListener,
		TimepointListener,
		GraphChangeListener,
		SelectionListener,
		NavigationListener< TrackSchemeVertex, TrackSchemeEdge >,
		ContextListener< TrackSchemeVertex >
{
	private static final long serialVersionUID = 1L;

	private final long ANIMATION_MILLISECONDS;

	private final TrackSchemeGraph< ?, ? > graph;

	/**
	 * Canvas used for displaying the trackscheme graph.
	 */
	private final InteractiveDisplayCanvas display;

	private final JScrollBar xScrollBar;

	private final JScrollBar yScrollBar;

	/**
	 * The current transform from layout to screen coordinates.
	 */
	private final ScreenTransformState screenTransform;

	/**
	 * layout the {@link TrackSchemeGraph} into layout coordinates.
	 */
	private final LineageTreeLayout layout;

	/**
	 * TODO
	 */
	private final ContextLayout contextLayout;

	/**
	 * determine how layouted vertices and edges are colored.
	 */
	private final GraphColorGenerator< TrackSchemeVertex, TrackSchemeEdge > colorGenerator;

	/**
	 * compute {@link ScreenEntities} from {@link LineageTreeLayout} using the
	 * current {@link ScreenTransform} and interpolate between
	 * {@link ScreenEntities} for animation.
	 */
	private final ScreenEntityAnimator entityAnimator;

	private final PainterThread painterThread;

	private final TrackSchemeOverlay graphOverlay;

	/**
	 * TODO
	 */
	// TODO rename
	private final Flags flags;

	/**
	 * Minimum timepoint in dataset.
	 */
	private double layoutMinY;

	/**
	 * Maximum timepoint in dataset.
	 */
	private double layoutMaxY;

	/**
	 * Minimum layoutX coordinate in current layout.
	 */
	private double layoutMinX;

	/**
	 * Maximum layoutX coordinate in current layout.
	 */
	private double layoutMaxX;

	/**
	 * Ratio of {@link #xScrollBar} values to layoutX coordinates.
	 */
	private double xScrollScale;

	/**
	 * Ratio of {@link #yScrollBar} values to layoutY (timepoint) coordinates.
	 */
	private double yScrollScale;

	/**
	 * If {@code true}, then scroll-bar {@link AdjustmentListener}s ignore
	 * events (when {@link #screenTransform} is changed by means other than the
	 * user dragging the scroll-bar).
	 */
	private boolean ignoreScrollBarChanges;

	private final FocusModel< TrackSchemeVertex, TrackSchemeEdge > focus;

	private final TimepointModel timepoint;

	private final TrackSchemeAutoFocus autoFocus;

	private final TrackSchemeNavigationActions navigationActions;

	private final TrackSchemeNavigationBehaviours navigationBehaviours;

	private final InertialScreenTransformEventHandler transformEventHandler;

	private final OffsetHeaders offsetHeaders;

	private NavigationEtiquette navigationEtiquette;

	private NavigationBehaviour navigationBehaviour;

	public TrackSchemePanel(
			final TrackSchemeGraph< ?, ? > graph,
			final HighlightModel< TrackSchemeVertex, TrackSchemeEdge > highlight,
			final FocusModel< TrackSchemeVertex, TrackSchemeEdge > focus,
			final TimepointModel timepoint,
			final SelectionModel< TrackSchemeVertex, TrackSchemeEdge > selection,
			final NavigationHandler< TrackSchemeVertex, TrackSchemeEdge > navigation,
			final TrackSchemeOptions optional )
	{
		super( new BorderLayout(), false );
		this.graph = graph;
		this.focus = focus;
		this.timepoint = timepoint;

		final Values options = optional.values;
		ANIMATION_MILLISECONDS = options.getAnimationDurationMillis();

		graph.graphChangeListeners().add( this );
		navigation.listeners().add( this );

		final int w = options.getWidth();
		final int h = options.getHeight();
		display = new InteractiveDisplayCanvas( w, h );

		screenTransform = new ScreenTransformState( new ScreenTransform( -10000, 10000, -10000, 10000, w, h ) );
		screenTransform.listeners().add( this );
		transformEventHandler = new InertialScreenTransformEventHandler( screenTransform );

		highlight.listeners().add( this );
		focus.listeners().add( this );
		timepoint.listeners().add( this );
		selection.listeners().add( this );

		graphOverlay = options.getTrackSchemeOverlayFactory().create( graph, highlight, focus, optional );

		display.overlays().add( graphOverlay );

		// This should be the last OverlayRenderer in display.
		// It triggers repainting if there is currently an ongoing animation.
		display.overlays().add( new OverlayRenderer()
		{
			@Override
			public void setCanvasSize( final int width, final int height )
			{}

			@Override
			public void drawOverlays( final Graphics g )
			{
				checkAnimate();
			}
		} );

		layout = new LineageTreeLayout( graph, selection );
		contextLayout = new ContextLayout( graph, layout );
		colorGenerator = options.getGraphColorGenerator();
		layout.layoutListeners().add( transformEventHandler );
		entityAnimator = new ScreenEntityAnimator();
		painterThread = new PainterThread( this );
		flags = new Flags();

		final MouseHighlightHandler highlightHandler = new MouseHighlightHandler( graphOverlay, highlight, graph );
		display.addMouseMotionListener( highlightHandler );
		display.addMouseListener( highlightHandler );
		screenTransform.listeners().add( highlightHandler );

		autoFocus = new TrackSchemeAutoFocus( layout, focus );
		screenTransform.listeners().add( autoFocus );

		navigationActions = new TrackSchemeNavigationActions( graph, layout, autoFocus, selection );

		navigationBehaviours = new TrackSchemeNavigationBehaviours( display, graph, layout, graphOverlay, autoFocus, navigation, selection );
		screenTransform.listeners().add( navigationBehaviours );

		offsetHeaders = new OffsetHeaders();
		offsetHeaders.listeners().add( transformEventHandler );
		offsetHeaders.listeners().add( graphOverlay );
		offsetHeaders.listeners().add( navigationBehaviours );
		offsetHeaders.listeners().add( highlightHandler );
		offsetHeaders.setHeaderSize( 25, 20 );

		xScrollBar = new JScrollBar( JScrollBar.HORIZONTAL );
		yScrollBar = new JScrollBar( JScrollBar.VERTICAL );
		xScrollBar.addAdjustmentListener( new AdjustmentListener()
		{
			@Override
			public void adjustmentValueChanged( final AdjustmentEvent e )
			{
				if ( ignoreScrollBarChanges )
					return;

				final double s = xScrollBar.getValue() / xScrollScale;
				final ScreenTransform t = screenTransform.get();
				t.shiftLayoutX( s - t.getMinX() );
				screenTransform.set( t );

				// TODO: probably this should be triggered in a listener to screenTransform:
				flags.setTransformChanged();
				painterThread.requestRepaint();
			}
		} );
		yScrollBar.addAdjustmentListener( new AdjustmentListener()
		{
			@Override
			public void adjustmentValueChanged( final AdjustmentEvent e )
			{
				if ( ignoreScrollBarChanges )
					return;

				final double s = yScrollBar.getValue() / yScrollScale;
				final ScreenTransform t = screenTransform.get();
				t.shiftLayoutY( s - t.getMinY() );
				screenTransform.set( t );

				// TODO: probably this should be triggered in a listener to screenTransform:
				flags.setTransformChanged();
				painterThread.requestRepaint();
			}
		} );

		display.setTransformEventHandler( transformEventHandler );
		add( display, BorderLayout.CENTER );

		add( yScrollBar, BorderLayout.EAST );
		final JPanel xScrollPanel = new JPanel( new BorderLayout() );
		xScrollPanel.add( xScrollBar, BorderLayout.CENTER );
		final int space = ( Integer ) UIManager.getDefaults().get( "ScrollBar.width" );
		xScrollPanel.add( Box.createRigidArea( new Dimension( space, 0 ) ), BorderLayout.EAST );
		add( xScrollPanel, BorderLayout.SOUTH );

		setNavigationEtiquette( options.getNavigationEtiquette() );

		painterThread.start();
	}

	/**
	 * Stop the painter thread.
	 */
	public void stop()
	{
		painterThread.interrupt();
	}

	/**
	 * Sets the time-point range of the dataset.
	 *
	 * @param minTimepoint
	 *            the min time-point.
	 * @param maxTimepoint
	 *            the max time-point.
	 */
	public void setTimepointRange( final int minTimepoint, final int maxTimepoint )
	{
		layoutMinY = minTimepoint;
		layoutMaxY = maxTimepoint;
		transformEventHandler.setLayoutRangeY( minTimepoint, maxTimepoint );
	}

	/**
	 * request repainting if there is currently an ongoing animation.
	 */
	void checkAnimate()
	{
		if ( !entityAnimator.isComplete() )
			painterThread.requestRepaint();
	}

	@Override
	public void paint()
	{
		final ReentrantReadWriteLock lock = graph.getLock();
		lock.readLock().lock();
		try
		{
			final ScreenTransform transform = screenTransform.get();
			final Flags flags = this.flags.clear();
			if ( flags.graphChanged )
			{
//				System.out.println( "paint: graphChanged" );
				layout.layout();
				layoutMinX = layout.getCurrentLayoutMinX();
				layoutMaxX = layout.getCurrentLayoutMaxX();
				entityAnimator.startAnimation( transform, ANIMATION_MILLISECONDS );
			}
			else if ( flags.transformChanged )
			{
//				System.out.println( "paint: transformChanged" );
//				entityAnimator.startAnimation( transform, 0 );
				if ( context != null && contextLayout.buildContext( context, transform, false ) )
				{
					layoutMinX = layout.getCurrentLayoutMinX();
					layoutMaxX = layout.getCurrentLayoutMaxX();
					entityAnimator.continueAnimation( transform, ANIMATION_MILLISECONDS );
				}
				else
					entityAnimator.continueAnimation( transform, 0 );
//					entityAnimator.startAnimation( transform, 0 );
//				entityAnimator.startAnimation( transform, ANIMATION_MILLISECONDS );
			}
			else if ( flags.selectionChanged )
			{
//				System.out.println( "paint: selectionChanged" );
				entityAnimator.startAnimation( transform, ANIMATION_MILLISECONDS );
			}
			else if ( flags.contextChanged )
			{
//				System.out.println( "paint: contextChanged" );
				if ( context == null )
				{
					layout.layout();
					layoutMinX = layout.getCurrentLayoutMinX();
					layoutMaxX = layout.getCurrentLayoutMaxX();
				}
				else if ( contextLayout.buildContext( context, transform, true ) )
				{
					layoutMinX = layout.getCurrentLayoutMinX();
					layoutMaxX = layout.getCurrentLayoutMaxX();
				}
				entityAnimator.startAnimation( transform, ANIMATION_MILLISECONDS );
			}
			else if ( flags.entitiesAttributesChanged )
			{
//				System.out.println( "paint: entitiesAttributesChanged" ); // DEBUG
				entityAnimator.continueAnimation( transform, 0 );
			}

			entityAnimator.setTime( System.currentTimeMillis() );
			entityAnimator.setPaintEntities( graphOverlay );
			display.repaint();
		}
		finally
		{
			lock.readLock().unlock();
		}
		// adjust scrollbars sizes
		final ScreenTransform t = new ScreenTransform();
		entityAnimator.getLastComputedScreenEntities().getScreenTransform( t );
		xScrollScale = 10000.0 / ( layoutMaxX - layoutMinX + 2 );
		final int xval = ( int ) ( xScrollScale * t.getMinX() );
		final int xext = ( int ) ( xScrollScale * ( t.getMaxX() - t.getMinX() ) );
		final int xmin = ( int ) ( xScrollScale * ( layoutMinX - boundXLayoutBorder ) );
		final int xmax = ( int ) ( xScrollScale * ( layoutMaxX + boundXLayoutBorder ) );
		yScrollScale = 10000.0 / ( layoutMaxY - layoutMinY + 2 );
		final int yval = ( int ) ( yScrollScale * t.getMinY() );
		final int yext = ( int ) ( yScrollScale * ( t.getMaxY() - t.getMinY() ) );
		final int ymin = ( int ) ( yScrollScale * ( layoutMinY - boundYLayoutBorder ) );
		final int ymax = ( int ) ( yScrollScale * ( layoutMaxY + boundYLayoutBorder ) );
		ignoreScrollBarChanges = true;
		xScrollBar.setValues( xval, xext, xmin, xmax );
		yScrollBar.setValues( yval, yext, ymin, ymax );
		ignoreScrollBarChanges = false;
	}

	@Override
	public void timepointChanged()
	{
		final int t = timepoint.getTimepoint();
		if ( graphOverlay.getCurrentTimepoint() != t )
		{
			graphOverlay.setCurrentTimepoint( t );
			display.repaint();
		}
	}

	@Override
	public void transformChanged( final ScreenTransform transform )
	{
		flags.setTransformChanged();
		painterThread.requestRepaint();
	}

	@Override
	public void graphChanged()
	{
		flags.setGraphChanged();
		painterThread.requestRepaint();
	}

	@Override
	public void highlightChanged()
	{
		display.repaint();
	}

	@Override
	public void focusChanged()
	{
		display.repaint();
	}

	@Override
	public void selectionChanged()
	{
		flags.setSelectionChanged();
		painterThread.requestRepaint();
	}

	public void entitiesAttributesChanged()
	{
		flags.setEntitiesAttributesChanged();
		painterThread.requestRepaint();
	}

	// TODO: THIS IS FOR TESTING ONLY
	private Context< TrackSchemeVertex > context;

	// TODO: THIS IS FOR TESTING ONLY
	@Override
	public void contextChanged( final Context< TrackSchemeVertex > context )
	{
		if ( this.context == null && context == null )
			return;

		this.context = context;
		flags.setContextChanged();
		painterThread.requestRepaint();
	}

	public NavigationEtiquette getNavigationEtiquette()
	{
		return navigationEtiquette;
	}

	public void setNavigationEtiquette( final NavigationEtiquette navigationEtiquette )
	{
		this.navigationEtiquette = navigationEtiquette;
		switch( navigationEtiquette )
		{
		case MINIMAL:
			navigationBehaviour = new MinimalNavigationBehaviour( transformEventHandler, 100, 100 );
			break;
		case CENTER_IF_INVISIBLE:
			navigationBehaviour = new CenterIfInvisibleNavigationBehaviour( transformEventHandler );
			break;
		case CENTERING:
		default:
			navigationBehaviour = new CenteringNavigationBehaviour( transformEventHandler );
			break;
		}
	}

	@Override
	public void navigateToVertex( final TrackSchemeVertex v )
	{
		if ( v.getLayoutTimestamp() == layout.getCurrentLayoutTimestamp() )
		{
			timepoint.setTimepoint( v.getTimepoint() );
			navigationBehaviour.navigateToVertex( v, screenTransform.get() );
		}
	}

	@Override
	public void navigateToEdge( final TrackSchemeEdge edge )
	{
		// TODO: focus target vertex?

		final TrackSchemeVertex source = edge.getSource( graph.vertexRef() );
		final TrackSchemeVertex target = edge.getTarget( graph.vertexRef() );
		final int clts = layout.getCurrentLayoutTimestamp();
		if ( target.getLayoutTimestamp() == clts && source.getLayoutTimestamp() == clts )
		{
			timepoint.setTimepoint( target.getTimepoint() );
			navigationBehaviour.navigateToEdge( edge, source, target, screenTransform.get() );
		}
		graph.releaseRef( source );
		graph.releaseRef( target );
	}

	/**
	 * TODO: Let NavigationHandler.navigateToVertex return a target transform
	 * instead of talking to the TransformEventHandler directly.
	 */
	interface NavigationBehaviour
	{
		public void navigateToVertex( final TrackSchemeVertex v, final ScreenTransform currentTransform );

		public void navigateToEdge( final TrackSchemeEdge e, final TrackSchemeVertex source, final TrackSchemeVertex target, final ScreenTransform currentTransform );
	}

	private static class CenteringNavigationBehaviour implements NavigationBehaviour
	{
		private final InertialScreenTransformEventHandler transformEventHandler;

		public CenteringNavigationBehaviour( final InertialScreenTransformEventHandler transformEventHandler )
		{
			this.transformEventHandler = transformEventHandler;
		}

		@Override
		public void navigateToVertex( final TrackSchemeVertex v, final ScreenTransform currentTransform )
		{
			final double lx = v.getLayoutX();
			final double ly = v.getTimepoint();
			transformEventHandler.centerOn( lx, ly );
		}

		@Override
		public void navigateToEdge( final TrackSchemeEdge e, final TrackSchemeVertex source, final TrackSchemeVertex target, final ScreenTransform currentTransform )
		{
			// TODO Auto-generated method stub
			System.err.println( "not implemented: CenteringNavigationBehaviour.navigateToEdge()" );
			new Throwable().printStackTrace( System.out );
		}
	}

	private static class CenterIfInvisibleNavigationBehaviour implements NavigationBehaviour
	{
		private final InertialScreenTransformEventHandler transformEventHandler;

		public CenterIfInvisibleNavigationBehaviour( final InertialScreenTransformEventHandler transformEventHandler )
		{
			this.transformEventHandler = transformEventHandler;
		}

		// With CENTER_IF_INVISIBLE etiquette, only navigate to the specified vertex if not
		// is currently displayed.
		@Override
		public void navigateToVertex( final TrackSchemeVertex v, final ScreenTransform currentTransform )
		{
			final double lx = v.getLayoutX();
			final double ly = v.getTimepoint();
			if ( currentTransform.getMaxX() < lx || currentTransform.getMinX() > lx
					|| currentTransform.getMaxY() < ly || currentTransform.getMinY() > ly )
			{
				transformEventHandler.centerOn( lx, ly );
			}
		}

		@Override
		public void navigateToEdge( final TrackSchemeEdge e, final TrackSchemeVertex source, final TrackSchemeVertex target, final ScreenTransform currentTransform )
		{
			// TODO Auto-generated method stub
			System.err.println( "not implemented: CenterIfInvisibleNavigationBehaviour.navigateToEdge()" );
			new Throwable().printStackTrace( System.out );
		}
	}

	private static class MinimalNavigationBehaviour implements NavigationBehaviour
	{
		private final InertialScreenTransformEventHandler transformEventHandler;

		private final int screenBorderX;

		private final int screenBorderY;

		public MinimalNavigationBehaviour( final InertialScreenTransformEventHandler transformEventHandler, final int screenBorderX, final int screenBorderY )
		{
			this.transformEventHandler = transformEventHandler;
			this.screenBorderX = screenBorderX;
			this.screenBorderY = screenBorderY;
		}

		@Override
		public void navigateToVertex( final TrackSchemeVertex v, final ScreenTransform currentTransform )
		{
			final double lx = v.getLayoutX();
			final double ly = v.getTimepoint();

			/*
			 * TODO: check for compatibility of screenBorder and screenWidth,
			 * screenHeight. Fall back to CENTER_IF_INVISIBLE if screen size too
			 * small.
			 */
//			final int screenWidth = currentTransform.getScreenWidth();
//			final int screenHeight = currentTransform.getScreenHeight();

			final double minX = currentTransform.getMinX();
			final double maxX = currentTransform.getMaxX();
			final double minY = currentTransform.getMinY();
			final double maxY = currentTransform.getMaxY();
			final double bx = screenBorderX / currentTransform.getScaleX();
			final double by = screenBorderY / currentTransform.getScaleY();

			double sx = 0;
			if ( lx > maxX - bx )
				sx = lx - maxX + bx;
			else if ( lx < minX + bx )
				sx = lx - minX - bx;
			double sy = 0;
			if ( ly > maxY - by )
				sy = ly - maxY + by;
			else if ( ly < minY + by )
				sy = ly - minY - by;

			if ( sx != 0 || sy != 0 )
			{
				final double cx = ( minX + maxX ) / 2 + sx;
				final double cy = ( minY + maxY ) / 2 + sy;
				transformEventHandler.centerOn( cx, cy );
			}
		}

		@Override
		public void navigateToEdge( final TrackSchemeEdge e, final TrackSchemeVertex source, final TrackSchemeVertex target, final ScreenTransform currentTransform )
		{
			/*
			 * TODO: check for compatibility of screenBorder and screenWidth,
			 * screenHeight. Fall back to CENTER_IF_INVISIBLE if screen size too
			 * small.
			 */
//			final int screenWidth = currentTransform.getScreenWidth();
//			final int screenHeight = currentTransform.getScreenHeight();

			final double minX = currentTransform.getMinX();
			final double maxX = currentTransform.getMaxX();
			final double minY = currentTransform.getMinY();
			final double maxY = currentTransform.getMaxY();
			final double bx = screenBorderX / currentTransform.getScaleX();
			final double by = screenBorderY / currentTransform.getScaleY();

			final double sourceX = source.getLayoutX();
			final double targetX = target.getLayoutX();
			final double eMinX = Math.min( sourceX, targetX );
			final double eMaxX = Math.max( sourceX, targetX );
			final double eMinY = source.getTimepoint();
			final double eMaxY = target.getTimepoint();
			final double lx = 0.5 * ( eMinX + eMaxX );
			final double ly = 0.5 * ( eMinY + eMaxY );

			double sx = 0;
			if ( ( eMaxX - eMinX ) > ( maxX - minX - 2 * bx ) )
				sx = lx - ( minX + maxX ) / 2;
			else if ( eMaxX > maxX - bx )
				sx = eMaxX - maxX + bx;
			else if ( eMinX < minX + bx )
				sx = eMinX - minX - bx;

			double sy = 0;
			if ( ( eMaxY - eMinY ) > ( maxY - minY - 2 * by ) )
				sy = ly - ( minY + maxY ) / 2;
			else if ( eMaxY > maxY - by )
				sy = eMaxY - maxY + by;
			else if ( eMinY < minY + by )
				sy = eMinY - minY - by;

			if ( sx != 0 || sy != 0 )
			{
				final double cx = ( minX + maxX ) / 2 + sx;
				final double cy = ( minY + maxY ) / 2 + sy;
				transformEventHandler.centerOn( cx, cy );
			}
		}
	}

	public ScreenTransformState getScreenTransform()
	{
		return screenTransform;
	}

	// TODO remove??? revise TrackSchemePanel / TrackSchemeFrame construction.
	public InteractiveDisplayCanvas getDisplay()
	{
		return display;
	}

	// TODO remove??? revise TrackSchemePanel / TrackSchemeFrame construction.
	public void setTrackSchemeStyle( final TrackSchemeStyle s )
	{
		throw new UnsupportedOperationException("TODO: this shouldn't be called. Should go through TrackSchemeOptions.getTrackSchemeOverlayFactory.");
	}

	// TODO remove??? revise TrackSchemePanel / TrackSchemeFrame construction.
	protected OffsetHeaders getOffsetHeaders()
	{
		return offsetHeaders;
	}

	// TODO is this needed? does it have to be public?
	protected LineageTreeLayout getLineageTreeLayout()
	{
		return layout;
	}

	// TODO is this needed? does it have to be public?
	protected TrackSchemeGraph< ?, ? > getGraph()
	{
		return graph;
	}

	public InertialScreenTransformEventHandler getTransformEventHandler()
	{
		return transformEventHandler;
	}

	public TrackSchemeNavigationBehaviours getNavigationBehaviours()
	{
		return navigationBehaviours;
	}

	public TrackSchemeNavigationActions getNavigationActions()
	{
		return navigationActions;
	}

	/**
	 * Exposes the graph overlay renderer of this panel.
	 *
	 * @return the graph overlay renderer of this panel.
	 */
	public TrackSchemeOverlay getGraphOverlay()
	{
		return graphOverlay;
	}

	class ScreenEntityAnimator extends AbstractAnimator
	{
		private ScreenEntities screenEntities;

		private ScreenEntities screenEntities2;

		private final ScreenEntities screenEntitiesIpStart;

		private ScreenEntities screenEntitiesIpEnd;

		private ScreenEntitiesInterpolator interpolator;

		private ScreenEntities lastComputedScreenEntities;

		private final int capacity = 1000;

		public ScreenEntityAnimator()
		{
			super( 0 );
			screenEntities = new ScreenEntities( graph, capacity );
			screenEntities2 = new ScreenEntities( graph, capacity );
			screenEntitiesIpStart = new ScreenEntities( graph, capacity );
			screenEntitiesIpEnd = new ScreenEntities( graph, capacity );
			interpolator = null;
			lastComputedScreenEntities = screenEntities;
		}

		/**
		 * Swap screenEntities and screenEntities2.
		 */
		private void swapPools()
		{
			final ScreenEntities tmp = screenEntities;
			screenEntities = screenEntities2;
			screenEntities2 = tmp;
			screenEntities.clear();
		}

		/**
		 * Swap screenEntities and screenEntitiesIpStart.
		 */
		private void copyIpStart()
		{
			screenEntitiesIpStart.set( lastComputedScreenEntities );
			screenEntities.clear();
		}

		/**
		 * Swap screenEntities and screenEntitiesIpEnd.
		 */
		private void swapIpEnd()
		{
			final ScreenEntities tmp = screenEntities;
			screenEntities = screenEntitiesIpEnd;
			screenEntitiesIpEnd = tmp;
			screenEntities.clear();
		}

		/**
		 *
		 * @param transform
		 * @param duration animation duration (in time units), may be 0.
		 */
		public void startAnimation( final ScreenTransform transform, final long duration )
		{
			reset( duration );
			if (duration > 0 )
			{
				copyIpStart();
				layout.cropAndScale( transform, screenEntities, offsetHeaders.getWidth(), offsetHeaders.getHeight(), colorGenerator );
				swapIpEnd();
				interpolator = new ScreenEntitiesInterpolator( screenEntitiesIpStart, screenEntitiesIpEnd );
			}
			else
			{
				interpolator = null;
				swapPools();
				layout.cropAndScale( transform, screenEntities, offsetHeaders.getWidth(), offsetHeaders.getHeight(), colorGenerator );
				lastComputedScreenEntities = screenEntities;
			}
		}

		public void continueAnimation( final ScreenTransform transform, final long duration )
		{
			if ( interpolator != null )
			{
				layout.cropAndScale( transform, screenEntities, offsetHeaders.getWidth(), offsetHeaders.getHeight(), colorGenerator );
				swapIpEnd();
				interpolator = new ScreenEntitiesInterpolator(
						screenEntitiesIpStart,
						screenEntitiesIpEnd,
						ScreenEntitiesInterpolator.getIncrementalY( screenEntitiesIpStart, screenEntitiesIpEnd ) );
			}
			else
			{
				startAnimation( transform, duration );
//				swapPools();
//				layout.cropAndScale( transform, screenEntities );
//				lastComputedScreenEntities = screenEntities;
			}
		}

		@Override
		public void setTime( final long time )
		{
			super.setTime( time );
			if ( interpolator != null )
			{
				swapPools();
				interpolator.interpolate( ratioComplete(), screenEntities );
				if ( isComplete() )
					interpolator = null;
				lastComputedScreenEntities = screenEntities;
			}
		}

		/**
		 * Set entities for painting into the specified double-buffered
		 * {@link TrackSchemeOverlay}. (This swaps
		 * {@link #screenEntities} with pending entities from the overlay.)
		 *
		 * @param overlay
		 *            the overlay to paint in.
		 */
		public void setPaintEntities( final TrackSchemeOverlay overlay )
		{
			if ( lastComputedScreenEntities == screenEntities )
			{
				final ScreenEntities tmp = overlay.setScreenEntities( screenEntities );
				if ( tmp == null )
				{
					screenEntities = new ScreenEntities( graph, capacity );
				}
				else
					screenEntities = tmp;
				screenEntities.clear();
			}
		}

		private ScreenEntities getLastComputedScreenEntities()
		{
			return lastComputedScreenEntities;
		}
	}

	static class Flags
	{
		private boolean transformChanged;
		private boolean selectionChanged;
		private boolean graphChanged;
		private boolean contextChanged;
		private boolean entitiesAttributesChanged;

		public Flags()
		{
			transformChanged = false;
			selectionChanged = false;
			graphChanged = false;
			contextChanged = false;
			entitiesAttributesChanged = false;
		}

		public Flags( final Flags f )
		{
			transformChanged = f.transformChanged;
			selectionChanged = f.selectionChanged;
			graphChanged = f.graphChanged;
			contextChanged = f.contextChanged;
			entitiesAttributesChanged = f.entitiesAttributesChanged;
		}

		public synchronized void setTransformChanged()
		{
			transformChanged = true;
		}

		public synchronized void setSelectionChanged()
		{
			selectionChanged = true;
		}

		public synchronized void setGraphChanged()
		{
			graphChanged = true;
		}

		public synchronized void setContextChanged()
		{
			contextChanged = true;
		}

		public synchronized void setEntitiesAttributesChanged()
		{
			entitiesAttributesChanged = true;
		}

		public synchronized Flags clear()
		{
			final Flags copy = new Flags( this );
			transformChanged = false;
			selectionChanged = false;
			graphChanged = false;
			contextChanged = false;
			entitiesAttributesChanged = false;
			return copy;
		}

		@Override
		public String toString()
		{
			final StringBuilder str = new StringBuilder(super.toString());
			str.append( '\n' );
			str.append( "  - transformChanged: " + transformChanged + "\n" );
			str.append( "  - selectionChanged: " + selectionChanged + "\n" );
			str.append( "  - graphChanged:     " + graphChanged + "\n" );
			str.append( "  - contextChanged:   " + contextChanged + "\n" );
			str.append( "  - entitiesAttributesChanged:   " + entitiesAttributesChanged + "\n" );
			return str.toString();
		}
	}
}
