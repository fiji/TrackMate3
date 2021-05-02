package org.mastodon.mamut.launcher;

import static org.mastodon.app.MastodonIcons.HELP_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.LOAD_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.MAINWINDOW_BG;
import static org.mastodon.app.MastodonIcons.MAMUT_IMPORT_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.NEW_ICON_MEDIUM;
import static org.mastodon.app.MastodonIcons.TGMM_IMPORT_ICON_MEDIUM;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;

class LauncherGUI extends JPanel
{

	private static final long serialVersionUID = 1L;

	static final String WELCOME_PANEL_KEY = "Welcome";

	static final String NEW_MASTODON_PROJECT_KEY = "NewMastodonProject";

	static final String LOAD_MASTODON_PROJECT_KEY = "LoadMastodonProject";

	static final String IMPORT_TGMM_KEY = "ImportTGMM";

	static final String IMPORT_MAMUT_KEY = "ImportMaMuT";

	static final String IMPORT_SIMI_KEY = "ImportSimi";

	final JButton btnNew;

	final JButton btnLoad;

	final JButton btnImportTgmm;

	final JButton btnImportMamut;

	final JButton btnImportSimi;

	private final JPanel centralPanel;

	final JPanel sidePanel;

	final OpenBDVPanel newMastodonProjectPanel;

	final LoadMastodonPanel loadMastodonPanel;

	final ImportTGMMPanel importTGMMPanel;

	final ImportMaMUTPanel importMamutPanel;

	final ImportSimiBioCellPanel importSimiBioCellPanel;

	final JButton btnHelp;

	public LauncherGUI()
	{
		setLayout( new BorderLayout( 5, 5 ) );

		sidePanel = new JPanel();
		sidePanel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		add( sidePanel, BorderLayout.WEST );
		final GridBagLayout gbl = new GridBagLayout();
		gbl.columnWidths = new int[] { 53, 123 };
		gbl.rowHeights = new int[] { 44, 44, 26, 44, 16, 0, 20, 35, 0 };
		gbl.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		gbl.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		sidePanel.setLayout( gbl );

		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets( 5, 5, 5, 5 );
		c.gridx = 0;
		c.gridy = 0;

		btnNew = new JButton( NEW_ICON_MEDIUM );
		sidePanel.add( btnNew, c );

		c.gridx = 1;
		c.gridy = 0;
		sidePanel.add( new JLabel( "new Mastodon project" ), c );

		c.gridx = 0;
		c.gridy++;
		btnLoad = new JButton( LOAD_ICON_MEDIUM );
		sidePanel.add( btnLoad, c );

		c.gridx = 1;
		sidePanel.add( new JLabel( "open Mastodon project" ), c );

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		sidePanel.add( new JSeparator(), c );

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		btnImportTgmm = new JButton( TGMM_IMPORT_ICON_MEDIUM );
		sidePanel.add( btnImportTgmm, c );

		c.gridx = 1;
		sidePanel.add( new JLabel( "import TGMM" ), c );

		c.gridx = 0;
		c.gridy++;
		btnImportMamut = new JButton( MAMUT_IMPORT_ICON_MEDIUM );
		sidePanel.add( btnImportMamut, c );

		c.gridx = 1;
		sidePanel.add( new JLabel( "import MaMuT" ), c );

		c.gridx = 0;
		c.gridy++;
		btnImportSimi = new JButton( TGMM_IMPORT_ICON_MEDIUM );
		sidePanel.add( btnImportSimi, c );

		c.gridx = 1;
		sidePanel.add( new JLabel( "import Simi-BioCell" ), c );

		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		sidePanel.add( new JSeparator(), c );

		c.gridwidth = 1;
		c.gridy++;
		btnHelp = new JButton( HELP_ICON_MEDIUM );
		sidePanel.add( btnHelp, c );

		centralPanel = new JPanel();
		centralPanel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		add( centralPanel, BorderLayout.CENTER );
		final CardLayout cardLayout = new CardLayout( 0, 0 );
		centralPanel.setLayout( cardLayout );

		final WelcomePanel welcomePanel = new WelcomePanel();
		centralPanel.add( welcomePanel, WELCOME_PANEL_KEY );

		newMastodonProjectPanel = new OpenBDVPanel( "New Mastodon project", "create" );
		centralPanel.add( newMastodonProjectPanel, NEW_MASTODON_PROJECT_KEY );

		importTGMMPanel = new ImportTGMMPanel();
		centralPanel.add( importTGMMPanel, IMPORT_TGMM_KEY );

		loadMastodonPanel = new LoadMastodonPanel();
		centralPanel.add( loadMastodonPanel, LOAD_MASTODON_PROJECT_KEY );

		importMamutPanel = new ImportMaMUTPanel();
		centralPanel.add( importMamutPanel, IMPORT_MAMUT_KEY );

		importSimiBioCellPanel = new ImportSimiBioCellPanel();
		centralPanel.add( importSimiBioCellPanel, IMPORT_SIMI_KEY );

		showPanel( WELCOME_PANEL_KEY );
	}

	void showPanel( final String key )
	{
		final CardLayout layout = ( CardLayout ) centralPanel.getLayout();
		layout.show( centralPanel, key );
	}

	class LoadMastodonPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		final JLabel lblInfo;

		public LoadMastodonPanel()
		{
			this.lblInfo = new JLabel();
			add( lblInfo );
		}
	}

	class ImportMaMUTPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		final JLabel lblInfo;

		private ImportMaMUTPanel()
		{
			final GridBagLayout gridBagLayout = new GridBagLayout();
			setLayout( gridBagLayout );

			lblInfo = new JLabel();
			final GridBagConstraints gbc_lblMastodon = new GridBagConstraints();
			gbc_lblMastodon.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblMastodon.fill = GridBagConstraints.VERTICAL;
			gbc_lblMastodon.gridx = 0;
			gbc_lblMastodon.gridy = 0;
			add( lblInfo, gbc_lblMastodon );
		}
	}

	private class WelcomePanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public WelcomePanel()
		{
			final GridBagLayout gridBagLayout = new GridBagLayout();
			gridBagLayout.columnWidths = new int[] { 194, 0 };
			gridBagLayout.rowHeights = new int[] { 16, 0, 42, 0 };
			setLayout( gridBagLayout );

			final JLabel lblMastodon = new JLabel( "Mastodon" );
			lblMastodon.setFont( lblMastodon.getFont().deriveFont( lblMastodon.getFont().getSize() + 10f ) );
			final GridBagConstraints gbc_lblMastodon = new GridBagConstraints();
			gbc_lblMastodon.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblMastodon.fill = GridBagConstraints.VERTICAL;
			gbc_lblMastodon.gridx = 0;
			gbc_lblMastodon.gridy = 0;
			add( lblMastodon, gbc_lblMastodon );

			final JLabel lblV = new JLabel( "v" + MastodonLauncher.MASTODON_VERSION );
			final GridBagConstraints gbc_lblV = new GridBagConstraints();
			gbc_lblV.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblV.gridx = 0;
			gbc_lblV.gridy = 1;
			add( lblV, gbc_lblV );

			final JLabel lblTobiasPietzsch = new JLabel( "Tobias Pietzsch & Jean-Yves Tinevez" );
			final GridBagConstraints gbc_lblTobiasPietzsch = new GridBagConstraints();
			gbc_lblTobiasPietzsch.anchor = GridBagConstraints.SOUTH;
			gbc_lblTobiasPietzsch.gridx = 0;
			gbc_lblTobiasPietzsch.gridy = 2;
			add( lblTobiasPietzsch, gbc_lblTobiasPietzsch );
		}

		@Override
		public void paintComponent( final Graphics g )
		{
			super.paintComponent( g );
			g.drawImage( MAINWINDOW_BG, 0, 0, this );
		}

	}

	class OpenBDVPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		final JButton btnCreate;

		final JLabel labelInfo;

		final JTextArea textAreaFile;

		public OpenBDVPanel( final String panelTitle, final String buttonTitle )
		{
			final GridBagLayout gbl_newMastodonProjectPanel = new GridBagLayout();
			gbl_newMastodonProjectPanel.columnWidths = new int[] { 0, 0 };
			gbl_newMastodonProjectPanel.rowHeights = new int[] { 35, 70, 65, 0, 0, 0, 0 };
			gbl_newMastodonProjectPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
			gbl_newMastodonProjectPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
			setLayout( gbl_newMastodonProjectPanel );

			final JLabel lblNewMastodonProject_1 = new JLabel( panelTitle );
			lblNewMastodonProject_1.setFont( lblNewMastodonProject_1.getFont().deriveFont( lblNewMastodonProject_1.getFont().getStyle() | Font.BOLD ) );
			final GridBagConstraints gbc_lblNewMastodonProject_1 = new GridBagConstraints();
			gbc_lblNewMastodonProject_1.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblNewMastodonProject_1.gridx = 0;
			gbc_lblNewMastodonProject_1.gridy = 0;
			add( lblNewMastodonProject_1, gbc_lblNewMastodonProject_1 );

			final JLabel lblBrowseToA = new JLabel( "Browse to a BDV file (xml/h5 pair):" );
			final GridBagConstraints gbc_lblBrowseToA = new GridBagConstraints();
			gbc_lblBrowseToA.anchor = GridBagConstraints.SOUTHWEST;
			gbc_lblBrowseToA.insets = new Insets( 5, 5, 5, 5 );
			gbc_lblBrowseToA.gridx = 0;
			gbc_lblBrowseToA.gridy = 1;
			add( lblBrowseToA, gbc_lblBrowseToA );

			textAreaFile = new JTextArea();
			textAreaFile.setLineWrap( true );
			final GridBagConstraints gbc_textAreaFile = new GridBagConstraints();
			gbc_textAreaFile.insets = new Insets( 5, 5, 5, 5 );
			gbc_textAreaFile.fill = GridBagConstraints.BOTH;
			gbc_textAreaFile.gridx = 0;
			gbc_textAreaFile.gridy = 2;
			add( textAreaFile, gbc_textAreaFile );

			final JButton btnBrowse = new JButton( "browse" );
			final GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
			gbc_btnBrowse.insets = new Insets( 5, 5, 5, 5 );
			gbc_btnBrowse.anchor = GridBagConstraints.EAST;
			gbc_btnBrowse.gridx = 0;
			gbc_btnBrowse.gridy = 3;
			add( btnBrowse, gbc_btnBrowse );

			labelInfo = new JLabel( "" );
			final GridBagConstraints gbc_labelInfo = new GridBagConstraints();
			gbc_labelInfo.insets = new Insets( 5, 5, 5, 5 );
			gbc_labelInfo.fill = GridBagConstraints.BOTH;
			gbc_labelInfo.gridx = 0;
			gbc_labelInfo.gridy = 4;
			add( labelInfo, gbc_labelInfo );

			btnCreate = new JButton( buttonTitle );
			final GridBagConstraints gbc_btnCreate = new GridBagConstraints();
			gbc_btnCreate.anchor = GridBagConstraints.EAST;
			gbc_btnCreate.gridx = 0;
			gbc_btnCreate.gridy = 5;
			add( btnCreate, gbc_btnCreate );

			/*
			 * Wire listeners.
			 */

			btnBrowse.addActionListener( l -> LauncherUtil.browseToBDVFile(
					null,
					textAreaFile,
					() -> checkBDVFile(),
					this ) );
			LauncherUtil.decorateJComponent( textAreaFile, () -> checkBDVFile() );
		}

		boolean checkBDVFile()
		{
			final File file = new File( textAreaFile.getText() );
			try
			{
				final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( file.getAbsolutePath() );
				final String str = LauncherUtil.buildInfoString( spimData );
				labelInfo.setText( str );
				return true;
			}
			catch ( final SpimDataException | RuntimeException e )
			{
				labelInfo.setText( "<html>Invalid BDV xml/h5 file.<p>" + e.getMessage() + "</html>" );
				return false;
			}
		}
	}
}