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

import static org.mastodon.app.MastodonIcons.BDV_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.FEATURES_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.LOAD_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.MAINWINDOW_BG;
import static org.mastodon.app.MastodonIcons.MAMUT_EXPORT_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.MAMUT_IMPORT_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.MASTODON_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.NEW_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.SAVE_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.TAGS_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.TGMM_IMPORT_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.TRACKSCHEME_ICON_MEDIUM;
import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;
import static org.mastodon.mamut.MamutMenuBuilder.fileMenu;
import static org.mastodon.mamut.MamutMenuBuilder.windowMenu;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.mastodon.app.ui.ViewMenu;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.ui.keymap.Keymap;

public class MainWindow extends JFrame
{
	private static final long serialVersionUID = 1L;

	protected final JMenuBar menubar;

	private final ViewMenu menu;

	public MainWindow( final WindowManager windowManager )
	{
		super( "Mastodon" );
		setIconImage( MASTODON_ICON_MEDIUM.getImage() );
		setLocationByPlatform( true );
		setLocationRelativeTo( null );

		final ActionMap actionMap = windowManager.getGlobalAppActions().getActionMap();

		final JPanel buttonsPanel = new JPanel();
		final GridBagLayout gbl = new GridBagLayout();
		gbl.columnWeights = new double[] { 1.0, 1.0 };
		gbl.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		buttonsPanel.setLayout(gbl);

		final GridBagConstraints separator_gbc = new GridBagConstraints();
		separator_gbc.fill = GridBagConstraints.HORIZONTAL;
		separator_gbc.gridwidth = 2;
		separator_gbc.insets = new Insets(5, 5, 5, 5);
		separator_gbc.gridx = 0;

		final GridBagConstraints label_gbc = new GridBagConstraints();
		label_gbc.fill = GridBagConstraints.HORIZONTAL;
		label_gbc.gridwidth = 2;
		label_gbc.insets = new Insets(5, 5, 5, 5);
		label_gbc.gridx = 0;

		final GridBagConstraints button_gbc_right = new GridBagConstraints();
		button_gbc_right.fill = GridBagConstraints.BOTH;
		button_gbc_right.insets = new Insets(0, 0, 5, 10);
		button_gbc_right.gridx = 1;

		final GridBagConstraints button_gbc_left = new GridBagConstraints();
		button_gbc_left.fill = GridBagConstraints.BOTH;
		button_gbc_left.insets = new Insets(0, 10, 5, 5);
		button_gbc_left.gridx = 0;

		int gridy = 0;

		label_gbc.gridy = gridy;
		final JLabel viewsLabel = new JLabel( "Views:" );
		viewsLabel.setFont( buttonsPanel.getFont().deriveFont( Font.BOLD ) );
		buttonsPanel.add( viewsLabel, label_gbc );

		++gridy;

		final JButton bdvButton = new JButton( actionMap.get( WindowManager.NEW_BDV_VIEW ) );
		prepareButton( bdvButton, "bdv", BDV_ICON_MEDIUM );
		button_gbc_right.gridy = gridy;
		buttonsPanel.add( bdvButton, button_gbc_right );

		++gridy;

		final JButton trackschemeButton = new JButton( actionMap.get( WindowManager.NEW_TRACKSCHEME_VIEW ) );
		prepareButton( trackschemeButton, "trackscheme", TRACKSCHEME_ICON_MEDIUM );
		button_gbc_right.gridy = gridy;
		buttonsPanel.add( trackschemeButton, button_gbc_right );

		++gridy;

		separator_gbc.gridy = gridy;
		buttonsPanel.add( new JSeparator(), separator_gbc );

		++gridy;

		label_gbc.gridy = gridy;
		final JLabel processingLabel = new JLabel( "Processing:" );
		processingLabel.setFont( buttonsPanel.getFont().deriveFont( Font.BOLD ) );
		buttonsPanel.add( processingLabel, label_gbc );

		++gridy;

		final JButton featureComputationButton = new JButton( actionMap.get( WindowManager.COMPUTE_FEATURE_DIALOG ) );
		prepareButton( featureComputationButton, "compute features", FEATURES_ICON_MEDIUM );
		button_gbc_right.gridy = gridy;
		buttonsPanel.add( featureComputationButton, button_gbc_right );

		++gridy;

		final JButton editTagSetsButton = new JButton( actionMap.get( WindowManager.TAGSETS_DIALOG ) );
		prepareButton( editTagSetsButton, "configure tags", TAGS_ICON_MEDIUM );
		button_gbc_right.gridy = gridy;
		buttonsPanel.add( editTagSetsButton, button_gbc_right );

		++gridy;

		separator_gbc.gridy = gridy;
		buttonsPanel.add( new JSeparator(), separator_gbc );

		++gridy;

		label_gbc.gridy = gridy;
		final JLabel ioLabel = new JLabel( "Input / Output:" );
		ioLabel.setFont( buttonsPanel.getFont().deriveFont( Font.BOLD ) );
		buttonsPanel.add( ioLabel, label_gbc );

		++gridy;

		final JButton createProjectButton = new JButton( actionMap.get( ProjectManager.CREATE_PROJECT ) );
		prepareButton( createProjectButton, "new project", NEW_ICON_MEDIUM );
		button_gbc_left.gridy = gridy;
		buttonsPanel.add( createProjectButton, button_gbc_left );

		final JButton importTgmmButton = new JButton( actionMap.get( ProjectManager.IMPORT_TGMM ) );
		prepareButton( importTgmmButton, "import tgmm", TGMM_IMPORT_ICON_MEDIUM );
		button_gbc_right.gridy = gridy;
		buttonsPanel.add( importTgmmButton, button_gbc_right );

		++gridy;

		final JButton importMamutButton = new JButton( actionMap.get( ProjectManager.IMPORT_MAMUT ) );
		prepareButton( importMamutButton, "import mamut", MAMUT_IMPORT_ICON_MEDIUM );
		button_gbc_left.gridy = gridy;
		buttonsPanel.add( importMamutButton, button_gbc_left );

		final JButton exportMamutButton = new JButton( actionMap.get( ProjectManager.EXPORT_MAMUT ) );
		prepareButton( exportMamutButton, "export mamut", MAMUT_EXPORT_ICON_MEDIUM );
		button_gbc_right.gridy = gridy;
		buttonsPanel.add( exportMamutButton, button_gbc_right );

		++gridy;

		final JButton saveProjectButton = new JButton( actionMap.get( ProjectManager.SAVE_PROJECT ) );
		prepareButton( saveProjectButton, "save project", SAVE_ICON_MEDIUM );
		button_gbc_left.gridy = gridy;
		buttonsPanel.add( saveProjectButton, button_gbc_left );

		final JButton loadProjectButton = new JButton( actionMap.get( ProjectManager.LOAD_PROJECT ) );
		prepareButton( loadProjectButton, "load project", LOAD_ICON_MEDIUM );
		button_gbc_right.gridy = gridy;
		buttonsPanel.add( loadProjectButton, button_gbc_right );


		/*
		 * Background with an image.
		 */

		final JComponent content = new JPanel()
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent( final Graphics g )
			{
				super.paintComponent( g );
				g.drawImage( MAINWINDOW_BG, 0, 0, this );
			}
		};
		setContentPane( content );

		buttonsPanel.setOpaque( false );
		content.add( buttonsPanel, BorderLayout.NORTH );

		menubar = new JMenuBar();
		setJMenuBar( menubar );

		final Keymap keymap = windowManager.getKeymapManager().getForwardDefaultKeymap();
		menu = new ViewMenu( menubar, keymap, KeyConfigContexts.MASTODON );
		keymap.updateListeners().add( menu::updateKeymap );
		addMenus( menu, actionMap );
		windowManager.getPlugins().addMenus( menu );

		setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosed( final WindowEvent e )
			{
				if ( windowManager != null )
					windowManager.closeAllWindows();
			}
		} );

		pack();
		setResizable( false );
	}

	private static void prepareButton( final JButton button, final String txt, final ImageIcon icon )
	{
		final JLabel iconLabel = new JLabel( icon );
		final JLabel clickMe = new JLabel( txt, SwingConstants.CENTER );
		button.setText( "" );
		button.setLayout( new BorderLayout() );
		button.add( iconLabel, BorderLayout.WEST );
		button.add( clickMe, BorderLayout.CENTER );
	}

	public static void addMenus( final ViewMenu menu, final ActionMap actionMap )
	{
		MamutMenuBuilder.build( menu, actionMap,
				fileMenu(
						item( ProjectManager.CREATE_PROJECT ),
						item( ProjectManager.LOAD_PROJECT ),
						item( ProjectManager.SAVE_PROJECT ),
						separator(),
						item( ProjectManager.IMPORT_TGMM ),
						item( ProjectManager.IMPORT_SIMI ),
						item( ProjectManager.IMPORT_MAMUT ),
						item( ProjectManager.EXPORT_MAMUT ),
						separator(),
						item( WindowManager.PREFERENCES_DIALOG )
				),
				windowMenu(
						item( WindowManager.NEW_BDV_VIEW ),
						item( WindowManager.NEW_TRACKSCHEME_VIEW )
				)
		);
	}
}
