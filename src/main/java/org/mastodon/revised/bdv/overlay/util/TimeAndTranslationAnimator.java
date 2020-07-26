package org.mastodon.revised.bdv.overlay.util;

import java.util.function.IntConsumer;

import bdv.viewer.animate.TranslationAnimator;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Translation animator that also linearly interpolates over time-points.
 * <p>
 * Start and target time-points are to be specified. Because the animator
 * AbstractTransformAnimator hierarchy does not specify animating time, this
 * class works with a hook in the shape of an {@link IntConsumer}.
 * <p>
 * The consumer will be called at every step of the animation, and it is the
 * caller responsibility to ensure that this consumer sets the time of the
 * object to animate.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class TimeAndTranslationAnimator extends TranslationAnimator
{

	private final long startTimePoint;

	private final long targetTimePoint;

	private final IntConsumer timePointSetter;

	public TimeAndTranslationAnimator(
			final AffineTransform3D transformStart,
			final double[] targetTranslation,
			final int startTimePoint,
			final int targetTimePoint,
			final IntConsumer timePointSetter,
			final long duration )
	{
		super( transformStart, targetTranslation, duration );
		this.startTimePoint = startTimePoint;
		this.targetTimePoint = targetTimePoint;
		this.timePointSetter = timePointSetter;
	}

	@Override
	public AffineTransform3D get( final double t )
	{
		timePointSetter.accept( getTimePoint( t ) );
		return super.get( t );
	}

	private int getTimePoint( final double t )
	{
		return ( int ) Math.round( startTimePoint + t * ( targetTimePoint - startTimePoint ) );
	}
}
