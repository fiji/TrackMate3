package org.mastodon.revised.ui.coloring;

import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.model.feature.FeatureProjection;

public class FeatureColorGeneratorTargetVertex< V extends Vertex< E >, E extends Edge< V > > implements ColorGenerator< E >
{

	private final FeatureProjection< V > featureProjection;

	private final ColorMap colorMap;

	private final double min;

	private final double max;

	private final V ref;

	public FeatureColorGeneratorTargetVertex( final FeatureProjection< V > featureProjection, final ColorMap colorMap, final double min, final double max, final V ref )
	{
		this.featureProjection = featureProjection;
		this.colorMap = colorMap;
		this.min = min;
		this.max = max;
		this.ref = ref;
	}

	@Override
	public int color( final E edge )
	{
		final V v = edge.getTarget( ref );
		if ( !featureProjection.isSet( v ) )
			return 0;

		final double alpha = ( featureProjection.value( v ) - min ) / ( max - min );
		return colorMap.get( alpha );

	}

}
