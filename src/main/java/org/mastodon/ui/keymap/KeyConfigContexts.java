package org.mastodon.ui.keymap;

/**
 * Constants that specify to what context an action or a behaviour applies.
 */
// TODO make this an interface
// TODO move to somewhere in mamut package
public final class KeyConfigContexts
{

	/**
	 * The action or behaviour applies to the whole app.
	 */
	public static final String MASTODON = "mastodon";

	/**
	 * The action or behaviour applies to the TrackScheme views.
	 */
	public static final String TRACKSCHEME = "ts";

	/**
	 * The action or behaviour applies to the BDV views.
	 */
	public static final String BIGDATAVIEWER = "bdv";

	/**
	 * The action or behaviour applies to the table views.
	 */
	public static final String TABLE = "table";

	private KeyConfigContexts()
	{}
}
