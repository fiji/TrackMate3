package org.mastodon.adapter;

import java.util.Set;

import org.mastodon.features.Feature;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.model.feature.FeatureComputer;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.revised.model.feature.FeatureProjection;

public class FeatureModelAdapter< V extends Vertex< E >, E extends Edge< V >, WV extends Vertex< WE >, WE extends Edge< WV > >
		implements FeatureModel< WV, WE >
{

	private final FeatureModel< V, E > featureModel;

	private final RefBimap< V, WV > vertexMap;

	private final RefBimap< E, WE > edgeMap;

	public FeatureModelAdapter(
			final FeatureModel< V, E > featureModel,
			final RefBimap< V, WV > vertexMap,
			final RefBimap< E, WE > edgeMap )
	{
		this.featureModel = featureModel;
		this.vertexMap = vertexMap;
		this.edgeMap = edgeMap;
	}

	@Override
	public Set< String > getEdgeProjectionKeys()
	{
		return featureModel.getEdgeProjectionKeys();
	}

	@Override
	public Set< String > getEdgeFeatureKeys()
	{
		return featureModel.getEdgeFeatureKeys();
	}

	@Override
	public Set< String > getVertexProjectionKeys()
	{
		return featureModel.getVertexProjectionKeys();
	}

	@Override
	public Set< String > getVertexFeatureKeys()
	{
		return featureModel.getVertexFeatureKeys();
	}

	@Override
	public FeatureTarget getProjectionTarget( final String projectionKey )
	{
		return featureModel.getProjectionTarget( projectionKey );
	}

	@Override
	public FeatureTarget getFeatureTarget( final String featureKey )
	{
		return featureModel.getFeatureTarget( featureKey );
	}

	@Override
	public FeatureProjection< WE > getEdgeProjection( final String projectionKey )
	{
		return new FeatureProjectionAdapter< E, WE >( featureModel.getEdgeProjection( projectionKey ), edgeMap );
	}

	@Override
	public FeatureProjection< WV > getVertexProjection( final String projectionKey )
	{
		return new FeatureProjectionAdapter< V, WV >( featureModel.getVertexProjection( projectionKey ), vertexMap );
	}

	@Override
	public Feature< ?, ?, ? > getFeature( final String featureKey )
	{
		return featureModel.getFeature( featureKey );
	}

	@Override
	public void clear()
	{
		featureModel.clear();
	}

	@Override
	public void declareFeature( final FeatureComputer< ?, ?, ? > fc )
	{
		featureModel.declareFeature( fc );
	}

	private final static class FeatureProjectionAdapter< K, WK > implements FeatureProjection< WK >
	{

		private final FeatureProjection< K > projection;

		private final RefBimap< K, WK > map;

		public FeatureProjectionAdapter( final FeatureProjection< K > projection, final RefBimap< K, WK > map )
		{
			this.projection = projection;
			this.map = map;
		}

		@Override
		public boolean isSet( final WK obj )
		{
			return projection.isSet( map.getLeft( obj ) );
		}

		@Override
		public double value( final WK obj )
		{
			return projection.value( map.getLeft( obj ) );
		}
	}
}
