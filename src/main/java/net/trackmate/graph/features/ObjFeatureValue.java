package net.trackmate.graph.features;

import java.util.Map;

import net.trackmate.graph.FeatureValue;
import net.trackmate.graph.features.unify.NotifyFeatureValueChange;

/**
 * TODO
 *
 * @param <O>
 * @param <T>
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public final class ObjFeatureValue< O, T > implements FeatureValue< T >
{
	private final Map< O, T > featureMap;

	private final O object;

	private final NotifyFeatureValueChange notify;

	protected ObjFeatureValue( final Map< O, T > featureMap, final O object, final NotifyFeatureValueChange notify )
	{
		this.featureMap = featureMap;
		this.object = object;
		this.notify = notify;
	}

	@Override
	public void set( final T value )
	{
		notify.notifyBeforeFeatureChange();
		featureMap.put( object, value );
	}

	@Override
	public void remove()
	{
		notify.notifyBeforeFeatureChange();
		featureMap.remove( object );
	}

	@Override
	public T get()
	{
		return featureMap.get( object );
	}

	@Override
	public boolean isSet()
	{
		return featureMap.containsKey( object );
	}
}