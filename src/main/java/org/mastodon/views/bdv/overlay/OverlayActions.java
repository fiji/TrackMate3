package org.mastodon.views.bdv.overlay;

import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.bdv.overlay.Visibilities.VisibilityMode;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;

import bdv.viewer.ViewerPanel;

/**
 * Install actions related to the track overlay over a BDV window.
 */
public class OverlayActions
{
	public static final String CYCLE_VISIBILITY_MODE = "cycle visibility mode";

	public static final String[] CYCLE_VISIBILITY_MODE_KEYS = new String[] { "V" };

	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( CYCLE_VISIBILITY_MODE, CYCLE_VISIBILITY_MODE_KEYS,
					"Cycle across the visibility modes for the overlay in BDV." );
		}
	}

	public static void install(
			final Actions actions,
			final ViewerPanel viewerPanel,
			final OverlayGraphRenderer< ?, ? > renderer )
	{
		actions.runnableAction( () -> {
			// Cycle mode.
			final VisibilityMode mode = renderer.nextVisibilityMode();
			// Show message.
			viewerPanel.showMessage( "Overlay visibility: " + mode );
		}, CYCLE_VISIBILITY_MODE, CYCLE_VISIBILITY_MODE_KEYS );
	}
}
