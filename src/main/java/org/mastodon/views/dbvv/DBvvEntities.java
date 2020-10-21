package org.mastodon.views.dbvv;

import gnu.trove.map.TIntObjectArrayMap;
import org.mastodon.graph.GraphListener;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;

public class DBvvEntities implements GraphListener< Spot, Link >, SceneEntitiesPerTimepoint
{
	public DBvvEntities( final ModelGraph graph )
	{
		this.graph = graph;
		graph.addGraphListener( this );
		graphRebuilt();
	}

	@Override
	public void graphRebuilt()
	{
		timepoints.clear();

		for ( final Spot vertex : graph.vertices() )
			forTimepoint( vertex ).ellipsoids.addOrUpdate( vertex );

		final Spot ref = graph.vertexRef();
		for ( final Link edge : graph.edges() )
			forTimepoint( edge, ref ).cylinders.addOrUpdate( edge );
		graph.releaseRef( ref );
	}

	@Override
	public void vertexAdded( final Spot vertex )
	{
		forTimepoint( vertex ).ellipsoids.addOrUpdate( vertex );
	}

	@Override
	public void vertexRemoved( final Spot vertex )
	{
		forTimepoint( vertex ).ellipsoids.remove( vertex );
	}

	@Override
	public void edgeAdded( final Link edge )
	{
		forTimepoint( edge ).cylinders.addOrUpdate( edge );
	}

	@Override
	public void edgeRemoved( final Link edge )
	{
		forTimepoint( edge ).cylinders.remove( edge );
	}

	private final ModelGraph graph;

	// Maps timepoint to SceneEntities
	private final TIntObjectArrayMap< SceneEntities > timepoints = new TIntObjectArrayMap<>();

	@Override
	public SceneEntities forTimepoint( final int timepoint )
	{
		SceneEntities entities = timepoints.get( timepoint );
		if ( entities == null )
		{
			entities = new SceneEntities( graph );
			timepoints.put( timepoint, entities );
		}
		return entities;
	}

	private SceneEntities forTimepoint( final Spot v )
	{
		return forTimepoint( v.getTimepoint() );
	}

	private SceneEntities forTimepoint( final Link edge )
	{
		final Spot ref = graph.vertexRef();
		final SceneEntities entities = forTimepoint( edge, ref );
		graph.releaseRef( ref );
		return entities;
	}

	private SceneEntities forTimepoint( final Link edge, final Spot ref )
	{
		return forTimepoint( edge.getTarget( ref ).getTimepoint() );
	}
}
