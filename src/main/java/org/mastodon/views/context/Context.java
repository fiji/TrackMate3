package org.mastodon.views.context;

import java.util.concurrent.locks.Lock;

public interface Context< V >
{
	public Lock readLock();

	public Iterable< V > getInsideVertices( final int timepoint );

	public int getTimepoint();
}
