package org.mastodon.mamut.feature;

import org.mastodon.mamut.model.Model;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = LinkTargetIdFeatureComputer.class, name = "Link target IDs" )
public class LinkTargetIdFeatureComputer implements MamutFeatureComputer
{

	@Parameter
	private Model model;

	@Parameter( type = ItemIO.OUTPUT )
	private LinkTargetIdFeature output;

	@Override
	public void createOutput()
	{
		if ( null == output )
			output = new LinkTargetIdFeature( model.getGraph() );
	}

	@Override
	public void run()
	{}
}
