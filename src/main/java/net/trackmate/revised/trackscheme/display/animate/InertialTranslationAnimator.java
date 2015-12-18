package net.trackmate.revised.trackscheme.display.animate;

import net.trackmate.revised.trackscheme.ScreenTransform;

public class InertialTranslationAnimator extends AbstractTransformAnimator< ScreenTransform >
{

	private final double tau;

	private final ScreenTransform transformStart;

	private final double vy0;

	private final double vx0;

	private final long d;

	public InertialTranslationAnimator( final ScreenTransform transformStart, final double vx0, final double vy0, final long duration )
	{
		super( duration );
		this.d = duration;
		this.transformStart = transformStart.copy();
		this.vx0 = -vx0;
		this.vy0 = -vy0;
		this.tau = duration / 3.; // in ms.
	}

	@Override
	protected ScreenTransform get( double t )
	{
		if ( t <= 0 ) { return transformStart; }
		if ( t > 1 )
		{
			t = 1;
		}

		final ScreenTransform transform = new ScreenTransform();
		transform.set( transformStart );

		final double inc = tau * ( 1 - Math.exp( -t * d / tau ) );
		final double dx = vx0 * inc;
		final double dy = vy0 * inc;

		transform.shiftLayoutX( dx );
		transform.shiftLayoutY( dy );

		return transform;
	}

}
