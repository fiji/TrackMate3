package org.mastodon.mamut.feature;

import static org.mastodon.feature.FeatureProjectionKey.key;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mastodon.feature.Dimension;
import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.feature.FeatureProjectionSpec;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.Multiplicity;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.scijava.plugin.Plugin;

public class LinkTargetIdFeature implements Feature< Link >
{

	public static final String KEY = "Link target IDs";

	private static final FeatureProjectionSpec SOURCE_PROJECTION_SPEC =
			new FeatureProjectionSpec( "Source spot id", Dimension.NONE );

	private static final FeatureProjectionSpec TARGET_PROJECTION_SPEC =
			new FeatureProjectionSpec( "Target spot id", Dimension.NONE );

	public static final Spec SPEC = new Spec();

	private static final String HELP_STRING = "Exposes the link source and target spot ids.";

	private final ModelGraph graph;

	@Plugin( type = FeatureSpec.class )
	public static class Spec extends FeatureSpec< LinkTargetIdFeature, Link >
	{
		public Spec()
		{
			super(
					KEY,
					HELP_STRING,
					LinkTargetIdFeature.class,
					Link.class,
					Multiplicity.SINGLE,
					SOURCE_PROJECTION_SPEC,
					TARGET_PROJECTION_SPEC );
		}
	}

	LinkTargetIdFeature( final ModelGraph graph )
	{
		this.graph = graph;
	}

	@Override
	public FeatureProjection< Link > project( final FeatureProjectionKey key )
	{
		if ( key( SOURCE_PROJECTION_SPEC ).equals( key ) )
			return new SourceIdProjection( graph.vertexRef() );
		if ( key(TARGET_PROJECTION_SPEC ).equals( key ) )
			return new TargetIdProjection( graph.vertexRef() );
		return null;
	}


	@Override
	public FeatureSpec< ? extends Feature< Link >, Link > getSpec()
	{
		return SPEC;
	}

	@Override
	public Set< FeatureProjection< Link > > projections()
	{
		final Set< FeatureProjection< Link > > projections = new HashSet<>();
		projections.add( new SourceIdProjection( graph.vertexRef() ) );
		projections.add( new TargetIdProjection( graph.vertexRef() ) );
		return Collections.unmodifiableSet( projections );
	}

	private static final class SourceIdProjection implements FeatureProjection< Link >
	{

		private final Spot ref;

		public SourceIdProjection( final Spot ref )
		{
			this.ref = ref;
		}

		@Override
		public boolean isSet( final Link obj )
		{
			return true;
		}

		@Override
		public double value( final Link obj )
		{
			return obj.getSource( ref ).getInternalPoolIndex();
		}

		@Override
		public String units()
		{
			return Dimension.NONE_UNITS;
		}

		@Override
		public FeatureProjectionKey getKey()
		{
			return key( SOURCE_PROJECTION_SPEC );
		}
	}

	private static final class TargetIdProjection implements FeatureProjection< Link >
	{

		private final Spot ref;

		public TargetIdProjection( final Spot ref )
		{
			this.ref = ref;
		}

		@Override
		public boolean isSet( final Link obj )
		{
			return true;
		}

		@Override
		public double value( final Link obj )
		{
			return obj.getTarget( ref ).getInternalPoolIndex();
		}

		@Override
		public String units()
		{
			return Dimension.NONE_UNITS;
		}

		@Override
		public FeatureProjectionKey getKey()
		{
			return key( TARGET_PROJECTION_SPEC );
		}
	}
}
