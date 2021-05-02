/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.mamut;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.mastodon.app.ui.CloseWindowActions;
import org.mastodon.app.ui.settings.SettingsPage;
import org.mastodon.app.ui.settings.SettingsPanel;
import org.mastodon.ui.keymap.Keymap;
import org.scijava.ui.behaviour.util.Actions;

public class PreferencesDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	private final SettingsPanel settingsPanel;

	public PreferencesDialog(
			final Frame owner,
			final Keymap keymap,
			final String[] keyConfigContexts )
	{
		super( owner, "Preferences", false );
		setLocationByPlatform( true );
		setLocationRelativeTo( null );
		settingsPanel = new SettingsPanel();
		settingsPanel.onOk( () -> setVisible( false ) );
		settingsPanel.onCancel( () -> setVisible( false ) );

		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				settingsPanel.cancel();
			}
		} );

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Actions actions = new Actions( im, am, keymap.getConfig(), keyConfigContexts );
		CloseWindowActions.install( actions, this );

		keymap.updateListeners().add( () -> actions.updateKeyConfig( keymap.getConfig() ) );

		getContentPane().add( settingsPanel, BorderLayout.CENTER );
		pack();
	}

	public void addPage( final SettingsPage page )
	{
		settingsPanel.addPage( page );
		pack();
	}

	/**
	 * Removes the settings page with the specified path. Does nothing if there is
	 * not settings page for the path.
	 *
	 * @param path
	 *                 the path of the settings page to remove.
	 */
	public void removePage( final String path )
	{
		settingsPanel.removePage( path );
		pack();
	}
}
