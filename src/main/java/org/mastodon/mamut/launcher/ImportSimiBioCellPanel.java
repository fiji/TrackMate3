package org.mastodon.mamut.launcher;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.URL;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.mastodon.ui.util.EverythingDisablerAndReenabler;
import org.mastodon.ui.util.ExtensionFileFilter;
import org.mastodon.ui.util.FileChooser;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;

class ImportSimiBioCellPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final URL HELP_URL = ImportSimiBioCellPanel.class.getResource( "ImportSimiBioCellHelp.html" );

	private static String suggestedFile = null;

	final JTextArea textAreaBDVFile;

	final JTextArea textAreaSimiFile;

	final JComboBox< String > setupComboBox;

	final JSpinner spinnerTimeOffset;

	final JCheckBox interpolateCheckBox;

	final JTextField spotRadiusTextField;

	final JLabel labelInfo;

	final JButton btnImport;

	public ImportSimiBioCellPanel()
	{
		final GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 70, 100 };
		layout.columnWeights = new double[] { 1., 0.2 };
		layout.rowHeights = new int[] { 0, 0, 75, 0, 0, 75 };
		setLayout( layout );

		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 5, 5, 5, 5 );
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;

		final JLabel lblTitle = new JLabel( "Import Simi-BIoCell data" );
		lblTitle.setFont( lblTitle.getFont().deriveFont( Font.BOLD ) );
		add( lblTitle, c );

		c.gridy++;
		c.anchor = GridBagConstraints.SOUTHWEST;
		add( new JLabel( "Browse to a BDV file (xml/h5 pair):" ), c );

		textAreaBDVFile = new JTextArea();
		textAreaBDVFile.setLineWrap( true );
		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		add( textAreaBDVFile, c );

		final JButton btnBrowseBDV = new JButton( "browse" );
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		add( btnBrowseBDV, c );

		c.anchor = GridBagConstraints.SOUTHWEST;
		c.gridy++;
		add( new JLabel( "Browse to a Simi-BioCell file:" ), c );

		textAreaSimiFile = new JTextArea();
		textAreaSimiFile.setLineWrap( true );
		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		add( textAreaSimiFile, c );

		final JButton btnBrowseSimi = new JButton( "browse" );
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		add( btnBrowseSimi, c );

		c.gridy++;
		c.anchor = GridBagConstraints.WEST;
		add( new JLabel( "Transform of setup:" ), c );

		setupComboBox = new JComboBox<>();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 1;
		add( setupComboBox, c );

		c.gridy++;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		add( new JLabel( "Time offset (simi frame - bdv timepoint):" ), c );

		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		spinnerTimeOffset = new JSpinner();
		spinnerTimeOffset.setModel( new SpinnerNumberModel( 0, 0, 1000, 1 ) );
		c.gridx = 1;
		add( spinnerTimeOffset, c );

		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridwidth = 0;
		c.fill = GridBagConstraints.BOTH;
		interpolateCheckBox = new JCheckBox( "Interpolate missing time-points" );
		add( interpolateCheckBox, c );

		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		add( new JLabel( "Spot radius:" ), c );

		c.gridx = 1;
		spotRadiusTextField = new JTextField( "10" );
		spotRadiusTextField.setHorizontalAlignment( JTextField.RIGHT );
		add( spotRadiusTextField, c );

		labelInfo = new JLabel( "" );
		labelInfo.setPreferredSize( new Dimension( 450, 140 ) );
		c.gridwidth = 2;
		c.gridy++;
		c.gridx = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1.;
		add( labelInfo, c );

		btnImport = new JButton( "import" );
		c.anchor = GridBagConstraints.SOUTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridx = 1;
		c.weighty = 0.;
		add( btnImport, c );

		c.gridx = 0;
		final JButton btnHelp = new JButton( "help" );
		c.anchor = GridBagConstraints.SOUTHWEST;
		add( btnHelp, c );

		/*
		 * Wire listeners.
		 */

		btnHelp.addActionListener( l -> LauncherUtil.showHelp( HELP_URL, "Help for the Simie-BioCell importer", this ) );
		btnBrowseBDV.addActionListener( l -> LauncherUtil.browseToBDVFile(
				suggestedFile,
				textAreaBDVFile,
				() -> checkBDVFile(),
				this ) );
		btnBrowseSimi.addActionListener( l -> browseToSimiFile() );
		LauncherUtil.decorateJComponent( textAreaBDVFile, () -> checkBDVFile() );
	}

	private void browseToSimiFile()
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( this, new Class[] { JLabel.class } );
		disabler.disable();
		final File file = FileChooser.chooseFile(
				this,
				suggestedFile,
				new ExtensionFileFilter( "sbd" ),
				"Browse to a Simi-BioCell file (.sbd)",
				FileChooser.DialogType.LOAD,
				FileChooser.SelectionMode.FILES_ONLY );
		if ( file == null )
		{
			disabler.reenable();
			return;
		}
		suggestedFile = file.getAbsolutePath();
		textAreaSimiFile.setText( file.getAbsolutePath() );
		disabler.reenable();
	}

	boolean checkBDVFile()
	{
		final File file = new File( textAreaBDVFile.getText() );
		try
		{
			final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( file.getAbsolutePath() );
			final String str = LauncherUtil.buildInfoString( spimData );
			labelInfo.setText( str );

			final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
			final int numSetups = seq.getViewSetupsOrdered().size();
			final Vector< String > names = new Vector<>( numSetups );
			for ( int i = 0; i < numSetups; i++ )
			{
				final BasicViewSetup setup = seq.getViewSetupsOrdered().get( i );
				final String name = setup.hasName() ? setup.getName() : "Setup " + i;
				names.add( name );
			}
			setupComboBox.setModel( new DefaultComboBoxModel<>( names ) );
			setupComboBox.setEnabled( true );
			suggestedFile = file.getAbsolutePath();
			return true;
		}
		catch ( final SpimDataException | RuntimeException e )
		{
			labelInfo.setText( "<html>Invalid BDV xml/h5 file.<p>" + e.getMessage() + "</html>" );
			setupComboBox.setEnabled( false );
			return false;
		}
	}

}