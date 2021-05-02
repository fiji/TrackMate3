package org.mastodon.views.trackscheme.display;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.function.Supplier;

import org.mastodon.model.tag.TagSetStructure.Tag;
import org.mastodon.model.tag.TagSetStructure.TagSet;
import org.mastodon.ui.coloring.ColorMap;
import org.mastodon.ui.coloring.ColoringModel;
import org.mastodon.ui.coloring.feature.FeatureColorMode;
import org.mastodon.ui.coloring.feature.FeatureProjectionId;

import net.imglib2.ui.OverlayRenderer;

/**
 * An {@link OverlayRenderer} that displays a color-bar for the
 * {@link FeatureColorMode} currently selected in the {@link ColoringModel}
 * provided at construction.
 *
 * @author Jean-Yves Tinevez
 */
public class ColorBarOverlay implements OverlayRenderer
{

	public static final Position DEFAULT_POSITION = Position.BOTTOM_RIGHT;

	public static final boolean DEFAULT_VISIBLE = false;

	private static final int DEFAULT_WIDTH = 70;

	private static final int X_CORNER_SPACE = 40;

	private static final int Y_CORNER_SPACE = 5;

	private static final int COLORBARS_SPACING = 10;

	private static final String VERTEX_HEADER = "V";

	private static final String EDGE_HEADER = "E";

	private static final String BOTH_HEADER = "V/E";

	private final static int INSET = 5;

	/**
	 * Specifies the {@link ColorBarOverlay} position in the display.
	 *
	 * @author Jean-Yves Tinevez
	 *
	 */
	public static enum Position
	{
		TOP_LEFT( "Top-left" ),
		TOP_RIGHT( "Top-right" ),
		BOTTOM_LEFT( "Bottom-left" ),
		BOTTOM_RIGHT( "Bottom-right" );

		private final String str;

		private Position( final String str )
		{
			this.str = str;
		}

		@Override
		public String toString()
		{
			return str;
		}

		public int xOrigin( final int canvasWidth, final int barWidth )
		{
			switch(this)
			{
			default:
			case BOTTOM_LEFT:
			case TOP_LEFT:
				return X_CORNER_SPACE;
			case BOTTOM_RIGHT:
			case TOP_RIGHT:
				return canvasWidth - barWidth - X_CORNER_SPACE;
			}
		}

		public int yOrigin( final int canvasHeight, final int barHeight )
		{
			switch ( this )
			{
			default:
			case TOP_LEFT:
			case TOP_RIGHT:
				return X_CORNER_SPACE;
			case BOTTOM_LEFT:
			case BOTTOM_RIGHT:
				return canvasHeight - barHeight - Y_CORNER_SPACE;
			}
		}
	}

	private final int minWidth = DEFAULT_WIDTH;

	private Position position = DEFAULT_POSITION;

	private final ColoringModel coloringModel;

	private int canvasWidth;

	private int canvasHeight;

	private boolean visible = DEFAULT_VISIBLE;

	private final Supplier< Color > bgColorSupplier;

	public ColorBarOverlay( final ColoringModel coloringModel, final Supplier< Color > bgColorSupplier )
	{
		this.coloringModel = coloringModel;
		this.bgColorSupplier = bgColorSupplier;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		if ( !visible || coloringModel.noColoring() )
			return;

		final FeatureColorMode featureColorMode = coloringModel.getFeatureColorMode();
		if ( null != featureColorMode )
		{
			draw( featureColorMode, g );
			return;
		}

		final TagSet tagSet = coloringModel.getTagSet();
		if ( null != tagSet )
		{
			draw( tagSet, g );
			return;
		}
	}

	private void draw( final TagSet tagSet, final Graphics g )
	{
		final int totalWidth = totalWidth( tagSet, g );
		final int totalHeight = totalHeight( tagSet, g );
		int x = position.xOrigin( canvasWidth, totalWidth );
		final int y = position.yOrigin( canvasHeight, totalHeight );
		final FontMetrics fm = g.getFontMetrics();
		final int height = fm.getHeight();

		final Color panelBGColor = bgColorSupplier.get();
		final Color bgColor = new Color( panelBGColor.getRed(), panelBGColor.getGreen(), panelBGColor.getBlue(), 130 );
		final Color lineColor = panelBGColor.darker().darker();
		g.setColor( bgColor );
		g.fillRect(
				x - INSET,
				y - INSET - totalHeight / 3,
				totalWidth + 2 * INSET,
				totalHeight + 2 * INSET );
		g.setColor( lineColor );
		g.drawRect(
				x - INSET,
				y - INSET - totalHeight / 3,
				totalWidth + 2 * INSET,
				totalHeight + 2 * INSET );

		g.setColor( Color.BLACK );
		g.drawString( tagSet.getName(), x, y );

		for ( final Tag tag : tagSet.getTags() )
		{
			g.setColor( new Color( tag.color(), true ) );
			g.fillRect( x, y, height, height );
			x += height + 2;
			g.drawString( tag.label(), x, y + height );
			x += fm.stringWidth( tag.label() ) + 5;
		}
	}

	private int totalHeight( final TagSet tagSet, final Graphics g )
	{
		final FontMetrics fm = g.getFontMetrics();
		return 2 * fm.getHeight() + 2;
	}

	private int totalWidth( final TagSet tagSet, final Graphics g )
	{
		final FontMetrics fm = g.getFontMetrics();
		int totalWidth = 0;
		for ( final Tag tag : tagSet.getTags() )
			totalWidth += fm.stringWidth( tag.label() ) + 2 + fm.getHeight() + 5;
		return totalWidth;
	}

	private void draw( final FeatureColorMode featureColorMode, final Graphics g )
	{
		final int x = position.xOrigin( canvasWidth, totalWidth( featureColorMode, g ) );
		final int y = position.yOrigin( canvasHeight, totalHeight( featureColorMode, g ) );
		final String vertexColorMap = featureColorMode.getVertexColorMap();
		final String vertexProjectionKey = toString( featureColorMode.getVertexFeatureProjection() );
		final double vertexRangeMin = featureColorMode.getVertexRangeMin();
		final double vertexRangeMax = featureColorMode.getVertexRangeMax();
		final Color panelBGColor = bgColorSupplier.get();
		final Color bgColor = new Color( panelBGColor.getRed(), panelBGColor.getGreen(), panelBGColor.getBlue(), 130 );
		final Color lineColor = panelBGColor.darker().darker();

		if ( areVandEequal( featureColorMode ) )
		{
			final int[] wh = computeWidthHeight( BOTH_HEADER, vertexProjectionKey, g.getFontMetrics() );
			g.setColor( bgColor );
			g.fillRect(
					x - INSET,
					y - INSET - wh[ 1 ] / 3,
					wh[ 0 ] + 2 * INSET,
					wh[ 1 ] + 2 * INSET );
			g.setColor( lineColor );
			g.drawRect(
					x - INSET,
					y - INSET - wh[ 1 ] / 3,
					wh[ 0 ] + 2 * INSET,
					wh[ 1 ] + 2 * INSET );

			draw( x, y, vertexColorMap, BOTH_HEADER, vertexProjectionKey, vertexRangeMin, vertexRangeMax, g );
		}
		else
		{
			final int[] wh1 = computeWidthHeight( VERTEX_HEADER, vertexProjectionKey, g.getFontMetrics() );
			final int[] wh2 = computeWidthHeight( EDGE_HEADER, vertexProjectionKey, g.getFontMetrics() );
			g.setColor( bgColor );
			g.fillRect(
					x - INSET,
					y - INSET - wh1[ 1 ] / 3,
					wh1[ 0 ] + wh2[ 0 ] + COLORBARS_SPACING + 2 * INSET,
					wh1[ 1 ] + 2 * INSET );
			g.setColor( lineColor );
			g.drawRect(
					x - INSET,
					y - INSET - wh1[ 1 ] / 3,
					wh1[ 0 ] + wh2[ 0 ] + COLORBARS_SPACING + 2 * INSET,
					wh1[ 1 ] + 2 * INSET );

			final int xShift = draw( x, y, vertexColorMap, VERTEX_HEADER, vertexProjectionKey, vertexRangeMin, vertexRangeMax, g );
			final String edgeColorMap = featureColorMode.getEdgeColorMap();
			final String edgeProjectionKey = toString( featureColorMode.getEdgeFeatureProjection() );
			final double edgeRangeMin = featureColorMode.getEdgeRangeMin();
			final double edgeRangeMax = featureColorMode.getEdgeRangeMax();
			draw( x + xShift + COLORBARS_SPACING, y, edgeColorMap, EDGE_HEADER, edgeProjectionKey, edgeRangeMin, edgeRangeMax, g );
		}
	}

	private static boolean areVandEequal( final FeatureColorMode featureColorMode )
	{
		if ( !featureColorMode.getVertexColorMap().equals( featureColorMode.getEdgeColorMap() ) )
			return false;
		if ( !featureColorMode.getVertexFeatureProjection().equals( featureColorMode.getEdgeFeatureProjection() ) )
			return false;
		if ( featureColorMode.getVertexRangeMin() != featureColorMode.getEdgeRangeMin() )
			return false;
		if ( featureColorMode.getVertexRangeMax() != featureColorMode.getEdgeRangeMax() )
			return false;

		return true;
	}

	private static String toString( final FeatureProjectionId featureProjectionId )
	{
		final StringBuilder sb = new StringBuilder( featureProjectionId.getProjectionKey() );
		final int[] sourceIndices;
		switch ( featureProjectionId.getMultiplicity() )
		{
		case SINGLE:
		default:
			sourceIndices = new int[] {};
			break;
		case ON_SOURCES:
			sourceIndices = new int[] { featureProjectionId.getI0() };
			break;
		case ON_SOURCE_PAIRS:
			sourceIndices = new int[] { featureProjectionId.getI0(), featureProjectionId.getI1() };
			break;
		}

		for ( final int sourceIndex : sourceIndices )
		{
			sb.append( " ch" );
			sb.append( sourceIndex );
		}
		return sb.toString();
	}

	private int[] computeWidthHeight(
			final String header,
			final String featureName,
			final FontMetrics fm )
	{
		final int localWidth = Math.max( minWidth, fm.stringWidth( featureName ) );
		final int height = fm.getHeight();
		final int vWidth = fm.stringWidth( header ) + 2;
		return new int[] { localWidth + vWidth, 3 * height };
	}

	private int draw(
			final int xOrigin,
			final int yOrigin,
			final String strCmap,
			final String header,
			final String featureName,
			final double rangeMin,
			final double rangeMax,
			final Graphics g )
	{
		final FontMetrics fm = g.getFontMetrics();
		final int localWidth = Math.max( minWidth, fm.stringWidth( featureName ) );
		final int lw = ( int ) ( 0.85 * localWidth );
		final int height = fm.getHeight();

		int x = xOrigin;
		final int y = yOrigin;

		g.setColor( Color.BLACK );
		g.drawString( header, x, y + height );
		final int vWidth = fm.stringWidth( header ) + 2;
		x += vWidth;
		final ColorMap vCmap = ColorMap.getColorMap( strCmap );
		for ( int i = 0; i < lw; i++ )
		{
			g.setColor( new Color( vCmap.get( ( double ) i / lw ), true ) );
			g.drawLine( x + i, y, x + i, y + height );
		}
		g.setColor( new Color( vCmap.get( Double.NaN ) ) );
		g.fillRect( ( int ) ( x + 0.9 * localWidth ), y, ( int ) ( 0.1 * localWidth ), height );

		// Feature name.
		g.setColor( Color.BLACK );
		final int xf = xOrigin + vWidth;
		final int yf = yOrigin - fm.getDescent();
		g.drawString( featureName, xf, yf );

		// Ticks.
		final int xt = xOrigin + vWidth;
		final int yt = yOrigin + height;
		g.drawLine( xt, yt, xt, yt + 2 );
		g.drawLine( xt + lw / 2, yt, xt + lw / 2, yt + 2 );
		g.drawLine( xt + lw - 1, yt, xt + lw - 1, yt + 2 );
		final String strVMin = String.format( "%.1f", rangeMin );
		final int stringWidthVMin = fm.stringWidth( strVMin );
		g.drawString( strVMin, xt - stringWidthVMin / 2, yt + 2 + height );

		final String strVMax = String.format( "%.1f", rangeMax );
		final int stringWidthVMax = fm.stringWidth( strVMax );
		g.drawString( strVMax, xt + lw - 1 - stringWidthVMax / 2, yt + 2 + height );

		return localWidth + vWidth;
	}

	private int totalHeight( final FeatureColorMode featureColorMode, final Graphics g )
	{
		final FontMetrics fm = g.getFontMetrics();
		return 3 * fm.getHeight() + 2;
	}

	private int totalWidth( final FeatureColorMode featureColorMode, final Graphics g )
	{
		final FontMetrics fm = g.getFontMetrics();
		int totalWidth = 0;
		if ( areVandEequal( featureColorMode ) )
		{
			totalWidth += fm.stringWidth( BOTH_HEADER ) + 2;
			totalWidth += Math.max( minWidth, fm.stringWidth( toString( featureColorMode.getVertexFeatureProjection() ) ) );
		}
		else
		{
			totalWidth += fm.stringWidth( VERTEX_HEADER ) + 2;
			totalWidth += Math.max( minWidth, fm.stringWidth( toString( featureColorMode.getVertexFeatureProjection() ) ) );
			totalWidth += COLORBARS_SPACING;
			totalWidth += fm.stringWidth( EDGE_HEADER ) + 2;
			totalWidth += Math.max( minWidth, fm.stringWidth( toString( featureColorMode.getEdgeFeatureProjection() ) ) );
		}
		return totalWidth;
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.canvasWidth = width;
		this.canvasHeight = height;
	}

	public void setVisible( final boolean visible )
	{
		this.visible = visible;
	}

	public void setPosition( final Position position )
	{
		this.position = position;
	}
}
