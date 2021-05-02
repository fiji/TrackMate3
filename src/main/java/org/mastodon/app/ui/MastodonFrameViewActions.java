package org.mastodon.app.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;

import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.util.HasSelectedState;
import org.scijava.listeners.Listeners;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

public class MastodonFrameViewActions
{
	public static final String TOGGLE_SETTINGS_PANEL = "toggle settings panel";
	public static final String CLOSE_WINDOW = "close window";

	static final String[] TOGGLE_SETTINGS_PANEL_KEYS = new String[] { "T" };
	static final String[] CLOSE_WINDOW_KEYS = new String[] { "ctrl W", "meta W" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.BIGDATAVIEWER, KeyConfigContexts.TRACKSCHEME, KeyConfigContexts.TABLE );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( TOGGLE_SETTINGS_PANEL, TOGGLE_SETTINGS_PANEL_KEYS, "Toggle the view settings toolbar of the active window." );
			descriptions.add( CLOSE_WINDOW, CLOSE_WINDOW_KEYS, "Close the active window." );
		}
	}

	private final MastodonFrameView< ?, ?, ?, ?, ?, ? > view;

	private final ToggleSettingsPanelAction toggleSettingsPanelAction;

	private final CloseWindowAction closeWindowAction;

	/**
	 * Create Mastodon view actions and install them in the specified
	 * {@link Actions}.
	 *
	 * @param actions
	 *            Actions are added here.
	 * @param view
	 *            Actions are targeted at this view.
	 */
	public static void install(
			final Actions actions,
			final MastodonFrameView< ?, ?, ?, ?, ?, ? > view )
	{
		final MastodonFrameViewActions ba = new MastodonFrameViewActions( view );

		actions.namedAction( ba.toggleSettingsPanelAction, TOGGLE_SETTINGS_PANEL_KEYS );
		actions.namedAction( ba.closeWindowAction, CLOSE_WINDOW_KEYS );
	}

	private MastodonFrameViewActions( final MastodonFrameView< ?, ?, ?, ?, ?, ? > view )
	{
		this.view = view;
		toggleSettingsPanelAction = new ToggleSettingsPanelAction( TOGGLE_SETTINGS_PANEL );
		closeWindowAction = new CloseWindowAction( CLOSE_WINDOW );
		// TODO: add group (lock...) select actions
	}

	private class CloseWindowAction extends AbstractNamedAction
	{

		private static final long serialVersionUID = 1L;

		public CloseWindowAction( final String name )
		{
			super( name );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			/*
			 * To properly close the view, we send it a WINDOW_CLOSING event.
			 * This way, the listeners of the JFrame are called and the closing
			 * happens gracefully within Mastodon.
			 */
			view.getFrame().dispatchEvent( new WindowEvent( view.getFrame(), WindowEvent.WINDOW_CLOSING ) );
		}
	}

	private class ToggleSettingsPanelAction extends AbstractNamedAction implements HasSelectedState
	{
		private static final long serialVersionUID = 1L;

		protected final Listeners.List< Listener > selectListeners;

		public ToggleSettingsPanelAction( final String name )
		{
			super( name );
			selectListeners = new Listeners.SynchronizedList<>();
			view.frame.settingsPanel.addComponentListener( new ComponentAdapter()
			{
				@Override
				public void componentShown( final ComponentEvent e )
				{
					selectListeners.list.forEach( l -> l.setSelected( true ) );
				}

				@Override
				public void componentHidden( final ComponentEvent e )
				{
					selectListeners.list.forEach( l -> l.setSelected( false ) );
				}
			} );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			view.frame.setSettingsPanelVisible( !view.frame.isSettingsPanelVisible() );
		}

		@Override
		public boolean isSelected()
		{
			return view.frame.isSettingsPanelVisible();
		}

		@Override
		public Listeners< Listener > selectListeners()
		{
			return selectListeners;
		}
	}
}
