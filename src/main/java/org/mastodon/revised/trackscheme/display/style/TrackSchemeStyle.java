package org.mastodon.revised.trackscheme.display.style;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;

import org.mastodon.revised.ui.util.ColorMap;

public class TrackSchemeStyle
{
	private static final Stroke DEFAULT_FOCUS_STROKE = new BasicStroke( 2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 8f, 3f }, 0 );

	private static final Stroke DEFAULT_GHOST_STROKE = new BasicStroke( 1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 3.0f }, 0.0f );

	public enum ColorEdgeBy
	{
		FIXED, EDGE, SOURCE_VERTEX, TARGET_VERTEX;
	}

	public enum ColorVertexBy
	{
		FIXED, VERTEX, INCOMING_EDGE, OUTGOING_EDGE;
	}

	public String name;

	public Color edgeColor;

	public Color vertexFillColor;

	public Color vertexDrawColor;

	public Color selectedVertexFillColor;

	public Color selectedEdgeColor;

	public Color selectedVertexDrawColor;

	public Color simplifiedVertexFillColor;

	public Color selectedSimplifiedVertexFillColor;

	public Color ghostEdgeColor;

	public Color ghostVertexFillColor;

	public Color ghostVertexDrawColor;

	public Color ghostSelectedVertexFillColor;

	public Color ghostSelectedEdgeColor;

	public Color ghostSelectedVertexDrawColor;

	public Color ghostSimplifiedVertexFillColor;

	public Color ghostSelectedSimplifiedVertexFillColor;

	public Color backgroundColor;

	public Color currentTimepointColor;

	public Color decorationColor;

	public Color vertexRangeColor;

	public Color headerBackgroundColor;

	public Color headerDecorationColor;

	public Color headerCurrentTimepointColor;

	public Font font;

	public Font headerFont;

	public Stroke edgeStroke;

	public Stroke edgeGhostStroke;

	public Stroke edgeHighlightStroke;

	public Stroke vertexStroke;

	public Stroke vertexGhostStroke;

	public Stroke vertexHighlightStroke;

	public Stroke focusStroke;

	public boolean highlightCurrentTimepoint;

	public boolean paintRows;

	public boolean paintColumns;

	public boolean paintHeaderShadow;

	public ColorVertexBy colorVertexBy;

	/**
	 * Might be a key to a vertex or an edge feature, depending on
	 * {@link #colorVertexBy}.
	 */
	public String vertexColorFeatureKey;

	public ColorMap vertexColorMap;

	public double minVertexColorRange;

	public double maxVertexColorRange;

	public ColorEdgeBy colorEdgeBy;

	/**
	 * Might be a key to a vertex or an edge feature, depending on
	 * {@link #colorEdgeBy}.
	 */
	public String edgeColorFeatureKey;

	public ColorMap edgeColorMap;

	public double minEdgeColorRange;

	public double maxEdgeColorRange;

	public static Color mixGhostColor( final Color color, final Color backgroundColor )
	{
		return ( color == null || backgroundColor == null )
				? null
				: new Color(
						( color.getRed() + backgroundColor.getRed() ) / 2,
						( color.getGreen() + backgroundColor.getGreen() ) / 2,
						( color.getBlue() + backgroundColor.getBlue() ) / 2,
						color.getAlpha() );
	}

	private void updateGhostColors()
	{
		ghostEdgeColor = mixGhostColor( edgeColor, backgroundColor );
		ghostVertexFillColor = mixGhostColor( vertexFillColor, backgroundColor );
		ghostVertexDrawColor = mixGhostColor( vertexDrawColor, backgroundColor );
		ghostSelectedVertexFillColor = mixGhostColor( selectedVertexFillColor, backgroundColor );
		ghostSelectedEdgeColor = mixGhostColor( selectedEdgeColor, backgroundColor );
		ghostSelectedVertexDrawColor = mixGhostColor( selectedVertexDrawColor, backgroundColor );
		ghostSimplifiedVertexFillColor = mixGhostColor( simplifiedVertexFillColor, backgroundColor );
		ghostSelectedSimplifiedVertexFillColor = mixGhostColor( selectedSimplifiedVertexFillColor, backgroundColor );
	}

	/*
	 * GETTERS
	 */

	public Color getEdgeColor( final double val )
	{
		return edgeColorMap.get( val, minEdgeColorRange, maxEdgeColorRange );
	}

	public Color getVertexColor( final double val )
	{
		return vertexColorMap.get( val, minVertexColorRange, maxVertexColorRange );
	}

	/*
	 * SETTERS
	 */

	public TrackSchemeStyle name( final String n )
	{
		name = n;
		return this;
	}

	public TrackSchemeStyle colorEdgeBy( final ColorEdgeBy ceb )
	{
		colorEdgeBy = ceb;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle colorVertexBy( final ColorVertexBy cvb )
	{
		colorVertexBy = cvb;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle edgeColorMap( final ColorMap cm )
	{
		edgeColorMap = cm;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle vertexColorMap( final ColorMap cm )
	{
		vertexColorMap = cm;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle edgeColorFeatureKey( final String key )
	{
		edgeColorFeatureKey = key;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle vertexColorFeatureKey( final String key )
	{
		vertexColorFeatureKey = key;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle minEdgeColorRange( final double val )
	{
		minEdgeColorRange = val;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle maxEdgeColorRange( final double val )
	{
		maxEdgeColorRange = val;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle minVertexColorRange( final double val )
	{
		minVertexColorRange = val;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle maxVertexColorRange( final double val )
	{
		maxVertexColorRange = val;
		notifyListeners();
		return this;
	}
	public TrackSchemeStyle edgeColor( final Color c )
	{
		edgeColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle vertexFillColor( final Color c )
	{
		vertexFillColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle vertexDrawColor( final Color c )
	{
		vertexDrawColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle selectedVertexFillColor( final Color c )
	{
		selectedVertexFillColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle selectedEdgeColor( final Color c )
	{
		selectedEdgeColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle selectedVertexDrawColor( final Color c )
	{
		selectedVertexDrawColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle simplifiedVertexFillColor( final Color c )
	{
		simplifiedVertexFillColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle selectedSimplifiedVertexFillColor( final Color c )
	{
		selectedSimplifiedVertexFillColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle backgroundColor( final Color c )
	{
		backgroundColor = c;
		updateGhostColors();
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle currentTimepointColor( final Color c )
	{
		currentTimepointColor = c;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle decorationColor( final Color c )
	{
		decorationColor = c;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle vertexRangeColor( final Color c )
	{
		vertexRangeColor = c;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle headerBackgroundColor( final Color c )
	{
		headerBackgroundColor = c;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle headerDecorationColor( final Color c )
	{
		headerDecorationColor = c;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle headerCurrentTimepointColor( final Color c )
	{
		headerCurrentTimepointColor = c;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle font( final Font f )
	{
		font = f;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle headerFont( final Font f )
	{
		headerFont = f;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle edgeStroke( final Stroke s )
	{
		edgeStroke = s;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle edgeGhostStroke( final Stroke s )
	{
		edgeGhostStroke = s;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle edgeHighlightStroke( final Stroke s )
	{
		edgeHighlightStroke = s;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle vertexStroke( final Stroke s )
	{
		vertexStroke = s;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle vertexGhostStroke( final Stroke s )
	{
		vertexGhostStroke = s;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle vertexHighlightStroke( final Stroke s )
	{
		vertexHighlightStroke = s;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle focusStroke( final Stroke s )
	{
		focusStroke = s;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle highlightCurrentTimepoint( final boolean b )
	{
		highlightCurrentTimepoint = b;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle paintRows( final boolean b )
	{
		paintRows = b;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle paintColumns( final boolean b )
	{
		paintColumns = b;
		notifyListeners();
		return this;
	}

	public TrackSchemeStyle paintHeaderShadow( final boolean b )
	{
		paintHeaderShadow = b;
		notifyListeners();
		return this;
	}

	private TrackSchemeStyle()
	{
		updateListeners = new ArrayList<>();
	}

	@Override
	public String toString()
	{
		return name;
	}

	synchronized void set( final TrackSchemeStyle style )
	{
		this.name = style.name;
		this.colorVertexBy = style.colorVertexBy;
		this.vertexColorFeatureKey = style.vertexColorFeatureKey;
		this.vertexColorMap = style.vertexColorMap;
		this.minVertexColorRange = style.minVertexColorRange;
		this.maxVertexColorRange = style.maxVertexColorRange;
		this.colorEdgeBy = style.colorEdgeBy;
		this.edgeColorFeatureKey = style.edgeColorFeatureKey;
		this.edgeColorMap = style.edgeColorMap;
		this.minEdgeColorRange = style.minEdgeColorRange;
		this.maxEdgeColorRange = style.maxEdgeColorRange;
		this.edgeColor = style.edgeColor;
		this.vertexFillColor = style.vertexFillColor;
		this.vertexDrawColor = style.vertexDrawColor;
		this.selectedVertexFillColor = style.selectedVertexFillColor;
		this.selectedEdgeColor = style.selectedEdgeColor;
		this.selectedVertexDrawColor = style.selectedVertexDrawColor;
		this.simplifiedVertexFillColor = style.simplifiedVertexFillColor;
		this.selectedSimplifiedVertexFillColor = style.selectedSimplifiedVertexFillColor;
		this.ghostEdgeColor = style.ghostEdgeColor;
		this.ghostVertexFillColor = style.ghostVertexFillColor;
		this.ghostVertexDrawColor = style.ghostVertexDrawColor;
		this.ghostSelectedVertexFillColor = style.ghostSelectedVertexFillColor;
		this.ghostSelectedEdgeColor = style.ghostSelectedEdgeColor;
		this.ghostSelectedVertexDrawColor = style.ghostSelectedVertexDrawColor;
		this.ghostSimplifiedVertexFillColor = style.ghostSimplifiedVertexFillColor;
		this.ghostSelectedSimplifiedVertexFillColor = style.ghostSelectedSimplifiedVertexFillColor;
		this.backgroundColor = style.backgroundColor;
		this.currentTimepointColor = style.currentTimepointColor;
		this.decorationColor = style.decorationColor;
		this.vertexRangeColor = style.vertexRangeColor;
		this.headerBackgroundColor = style.headerBackgroundColor;
		this.headerDecorationColor = style.headerDecorationColor;
		this.headerCurrentTimepointColor = style.headerCurrentTimepointColor;
		this.font = style.font;
		this.headerFont = style.headerFont;
		this.edgeStroke = style.edgeStroke;
		this.edgeGhostStroke = style.edgeGhostStroke;
		this.edgeHighlightStroke = style.edgeHighlightStroke;
		this.vertexStroke = style.vertexStroke;
		this.vertexGhostStroke = style.vertexGhostStroke;
		this.vertexHighlightStroke = style.vertexHighlightStroke;
		this.focusStroke = style.focusStroke;
		this.highlightCurrentTimepoint = style.highlightCurrentTimepoint;
		this.paintRows = style.paintRows;
		this.paintColumns = style.paintColumns;
		this.paintHeaderShadow = style.paintHeaderShadow;
		notifyListeners();
	}

	public interface UpdateListener
	{
		public void trackSchemeStyleChanged();
	}

	private final ArrayList< UpdateListener > updateListeners;

	private void notifyListeners()
	{
		for ( final UpdateListener l : updateListeners )
			l.trackSchemeStyleChanged();
	}

	public synchronized boolean addUpdateListener( final UpdateListener l )
	{
		if ( !updateListeners.contains( l ) )
		{
			updateListeners.add( l );
			return true;
		}
		return false;
	}

	public synchronized boolean removeUpdateListener( final UpdateListener l )
	{
		return updateListeners.remove( l );
	}

	/**
	 * Returns a new style instance, copied from this style.
	 *
	 * @param name
	 *            the name for the copied style.
	 * @return a new style instance.
	 */
	TrackSchemeStyle copy( final String name )
	{
		final TrackSchemeStyle newStyle = new TrackSchemeStyle();
		newStyle.set( this );
		newStyle.name = name;
		return newStyle;
	}

	/**
	 * Returns the default TrackScheme style instance. Editing this instance
	 * will affect all view using this style.
	 *
	 * @return the single common instance for the default style.
	 */
	public static TrackSchemeStyle defaultStyle()
	{
		return df;
	}

	private static final TrackSchemeStyle df;
	static
	{
		final Color fill = new Color( 128, 255, 128 );
		df = new TrackSchemeStyle().name( "default" ).
				colorEdgeBy( ColorEdgeBy.FIXED ).
				edgeColorFeatureKey( "" ).
				vertexColorFeatureKey( "" ).
				edgeColorMap( ColorMap.JET ).
				vertexColorMap( ColorMap.JET ).
				minEdgeColorRange( 0. ).
				maxEdgeColorRange( 1. ).
				minVertexColorRange( 0. ).
				maxVertexColorRange( 1. ).
				colorVertexBy( ColorVertexBy.FIXED ).
				backgroundColor( Color.LIGHT_GRAY ).
				currentTimepointColor( new Color( 217, 217, 217 ) ).
				vertexFillColor( Color.WHITE ).
				selectedVertexFillColor( fill ).
				simplifiedVertexFillColor( Color.BLACK ).
				selectedSimplifiedVertexFillColor( new Color( 0, 128, 0 ) ).
				vertexDrawColor( Color.BLACK ).
				selectedVertexDrawColor( Color.BLACK ).
				edgeColor( Color.BLACK ).
				selectedEdgeColor( fill.darker() ).
				decorationColor( Color.YELLOW.darker().darker() ).
				vertexRangeColor( new Color( 128, 128, 128 ) ).
				headerBackgroundColor( new Color( 217, 217, 217 ) ). // new Color( 238, 238, 238 ) ).
				headerDecorationColor( Color.DARK_GRAY ).
				headerCurrentTimepointColor( Color.WHITE ).
				font( new Font( "SansSerif", Font.PLAIN, 9 ) ).
				headerFont( new Font( "SansSerif", Font.PLAIN, 9 ) ).
				edgeStroke( new BasicStroke() ).
				edgeGhostStroke( DEFAULT_GHOST_STROKE ).
				edgeHighlightStroke( new BasicStroke( 2f ) ).
				vertexStroke( new BasicStroke() ).
				vertexGhostStroke( DEFAULT_GHOST_STROKE ).
				vertexHighlightStroke( new BasicStroke( 3f ) ).
				focusStroke( DEFAULT_FOCUS_STROKE ).
				highlightCurrentTimepoint( true ).
				paintRows( true ).
				paintColumns( true ).
				paintHeaderShadow( true );
	}

	/**
	 * Returns the modern TrackScheme style instance. Editing this instance will
	 * affect all view using this style.
	 *
	 * @return the single common instance for the modern style.
	 */
	public static TrackSchemeStyle modernStyle()
	{
		return modern;
	}

	private static final TrackSchemeStyle modern;
	static
	{
		final Color bg = new Color( 163, 199, 197 );
		final Color fill = new Color( 64, 106, 102 );
		final Color selfill = new Color( 255, 128, 128 );
		final Color currenttp = new Color( 38, 175, 185 );
		modern = new TrackSchemeStyle().name( "modern" ).
				colorEdgeBy( ColorEdgeBy.FIXED ).
				colorVertexBy( ColorVertexBy.FIXED ).
				edgeColorFeatureKey( "" ).
				vertexColorFeatureKey( "" ).
				edgeColorMap( ColorMap.JET ).
				vertexColorMap( ColorMap.JET ).
				minEdgeColorRange( 0. ).
				maxEdgeColorRange( 1. ).
				minVertexColorRange( 0. ).
				maxVertexColorRange( 1. ).
				backgroundColor( bg ).
				currentTimepointColor( currenttp ).
				vertexFillColor( fill ).
				selectedVertexFillColor( selfill ).
				simplifiedVertexFillColor( fill ).
				selectedSimplifiedVertexFillColor( selfill ).
				vertexDrawColor( Color.WHITE ).
				selectedVertexDrawColor( Color.BLACK ).
				edgeColor( Color.WHITE ).
				selectedEdgeColor( selfill.darker() ).
				decorationColor( bg.darker() ).
				vertexRangeColor( Color.WHITE ).
				headerBackgroundColor( bg.brighter() ).
				headerDecorationColor( bg ).
				headerCurrentTimepointColor( bg.darker() ).
				font( new Font( "Calibri", Font.PLAIN, 12 ) ).
				headerFont( new Font( "Calibri", Font.PLAIN, 12 ) ).
				edgeStroke( new BasicStroke() ).
				edgeGhostStroke( DEFAULT_GHOST_STROKE ).
				edgeHighlightStroke( new BasicStroke( 2f ) ).
				vertexStroke( new BasicStroke() ).
				vertexGhostStroke( DEFAULT_GHOST_STROKE ).
				vertexHighlightStroke( new BasicStroke( 3f ) ).
				focusStroke( DEFAULT_FOCUS_STROKE ).
				highlightCurrentTimepoint( true ).
				paintRows( true ).
				paintColumns( true ).
				paintHeaderShadow( true );
	}

	/**
	 * Returns the lorry TrackScheme style instance. Editing this instance will
	 * affect all view using this style.
	 *
	 * @return the single common instance for the lorry style.
	 */
	public static TrackSchemeStyle lorryStyle()
	{
		return hmdyk;
	}

	private static final TrackSchemeStyle hmdyk;
	static
	{
		final Color bg = new Color( 163, 199, 197 );
		final Color fill = new Color( 225, 216, 183 );
		final Color selfill = new Color( 53, 107, 154 );
		final Color seldraw = new Color( 230, 245, 255 );
		final Color seledge = new Color( 91, 137, 158 );
		hmdyk = new TrackSchemeStyle().name( "lorry" ).
				colorEdgeBy( ColorEdgeBy.FIXED ).
				colorVertexBy( ColorVertexBy.FIXED ).
				edgeColorFeatureKey( "" ).
				vertexColorFeatureKey( "" ).
				edgeColorMap( ColorMap.JET ).
				vertexColorMap( ColorMap.JET ).
				minEdgeColorRange( 0. ).
				maxEdgeColorRange( 1. ).
				minVertexColorRange( 0. ).
				maxVertexColorRange( 1. ).
				backgroundColor( bg ).
				currentTimepointColor( bg.brighter() ).
				vertexFillColor( fill ).
				selectedVertexFillColor( selfill ).
				simplifiedVertexFillColor( Color.DARK_GRAY ).
				selectedSimplifiedVertexFillColor( selfill ).
				vertexDrawColor( Color.DARK_GRAY ).
				selectedVertexDrawColor( seldraw ).
				edgeColor( Color.DARK_GRAY ).
				selectedEdgeColor( seledge ).
				decorationColor( bg.darker() ).
				vertexRangeColor( Color.DARK_GRAY ).
				headerBackgroundColor( bg.brighter() ).
				headerDecorationColor( bg ).
				headerCurrentTimepointColor( bg.darker() ).
				font( new Font( "Calibri", Font.PLAIN, 12 ) ).
				headerFont( new Font( "Calibri", Font.PLAIN, 12 ) ).
				edgeStroke( new BasicStroke() ).
				edgeGhostStroke( DEFAULT_GHOST_STROKE ).
				edgeHighlightStroke( new BasicStroke( 2f ) ).
				vertexStroke( new BasicStroke() ).
				vertexGhostStroke( DEFAULT_GHOST_STROKE ).
				vertexHighlightStroke( new BasicStroke( 3f ) ).
				focusStroke( DEFAULT_FOCUS_STROKE ).
				highlightCurrentTimepoint( true ).
				paintRows( true ).
				paintColumns( true ).
				paintHeaderShadow( true );
	}

	public static Collection< TrackSchemeStyle > defaults;
	static
	{
		defaults = new ArrayList<>( 3 );
		defaults.add( df );
		defaults.add( hmdyk );
		defaults.add( modern );
	}

}