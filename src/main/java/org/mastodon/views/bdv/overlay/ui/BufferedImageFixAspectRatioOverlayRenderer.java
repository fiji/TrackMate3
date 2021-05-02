package org.mastodon.views.bdv.overlay.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.overlay.BufferedImageOverlayRenderer;

/**
 * {@link OverlayRenderer} that draws a {@link BufferedImage} on a canvas,
 * maintaining its aspect-ratio as the canvas is resized. The image is drawn
 * from the top-left corner. It is resized as the canvas changes its size,
 * honoring the largest rescaling factor in width or height. The initial size of
 * the canvas must be set by the {@link #setOriginalSize(Dimension)} method.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class BufferedImageFixAspectRatioOverlayRenderer extends BufferedImageOverlayRenderer
{

	private Dimension origSize;

	@Override
	public void drawOverlays( final Graphics g )
	{
		synchronized ( this )
		{
			if ( pending )
			{
				final BufferedImage tmp = bufferedImage;
				bufferedImage = pendingImage;
				pendingImage = tmp;
				pending = false;
			}
		}
		if ( bufferedImage != null )
		{
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );

			final double s1 = ( double ) width / origSize.width;
			final double s2 = ( double ) height / origSize.height;
			final double s = Math.max( s1, s2 );
			final int scaleWidth = ( int ) Math.round( bufferedImage.getWidth() * s );
			final int scaleHeight = ( int ) Math.round( bufferedImage.getHeight() * s );
			g.drawImage( bufferedImage, 0, 0, scaleWidth, scaleHeight, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null );
		}
	}

	public void setOriginalSize( final Dimension origSize )
	{
		this.origSize = origSize;
	}

	public Dimension getOriginalSize()
	{
		return origSize;
	}
}
