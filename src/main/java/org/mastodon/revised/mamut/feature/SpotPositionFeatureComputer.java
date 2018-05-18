package org.mastodon.revised.mamut.feature;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mastodon.graph.io.RawGraphIO.FileIdToGraphMap;
import org.mastodon.io.ObjectToFileIdMap;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.feature.FeatureProjection;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
import org.scijava.plugin.Plugin;

import net.imglib2.RealLocalizable;

@Plugin( type = SpotFeatureComputer.class, name = "Spot position" )
public class SpotPositionFeatureComputer implements SpotFeatureComputer
{

	private static final String KEY = "Spot position";

	@Override
	public Set< String > getDependencies()
	{
		return Collections.emptySet();
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public Feature< Spot, RealLocalizable > compute( final Model model )
	{
		final HashMap< String, FeatureProjection< Spot > > map = new HashMap<>();
		for ( int d = 0; d < 3; d++ )
		{
			final String pname = "Spot " + ( char ) ( 'X' + d ) + " position";
			final SpotPositionProjection projection = new SpotPositionProjection( d );
			map.put( pname, projection );
		}
		final Map< String, FeatureProjection< Spot > > projections = Collections.unmodifiableMap( map );
		final RealLocalizableFeature< Spot > feature = new RealLocalizableFeature< Spot >( KEY, Spot.class, projections );
		return feature;
	}

	private final static class RealLocalizableFeature< K extends RealLocalizable > implements Feature< K, RealLocalizable >
	{

		private final String key;

		private final Class< K > targetClass;

		private final Map< String, FeatureProjection< K > > projections;

		public RealLocalizableFeature( final String key, final Class< K > targetClass, final Map< String, FeatureProjection< K > > projections )
		{
			this.key = key;
			this.targetClass = targetClass;
			this.projections = projections;
		}

		@Override
		public String getKey()
		{
			return key;
		}

		@Override
		public Map< String, FeatureProjection< K > > getProjections()
		{
			return projections;
		}

		@Override
		public Class< K > getTargetClass()
		{
			return targetClass;
		}

		@Override
		public RealLocalizable get( final K o, final RealLocalizable ref )
		{
			return o;
		}

		@Override
		public RealLocalizable get( final K o )
		{
			return o;
		}

		@Override
		public boolean isSet( final K o )
		{
			return true;
		}

		@Override
		public void serialize( final File file, final ObjectToFileIdMap< K > idmap ) throws IOException
		{
			// Do nothing.
		}
	}

	private final static class SpotPositionProjection implements FeatureProjection< Spot >
	{

		private final int d;

		public SpotPositionProjection( final int d )
		{
			this.d = d;
		}

		@Override
		public boolean isSet( final Spot obj )
		{
			return true;
		}

		@Override
		public double value( final Spot obj )
		{
			return obj.getDoublePosition( d );
		}
	}

	@Override
	public Feature< Spot, RealLocalizable > deserialize( final File file, final Model support, final FileIdToGraphMap< ?, ? > fileIdToGraphMap ) throws IOException
	{
		return compute( support );
	}
}
