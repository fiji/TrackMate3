package org.mastodon.mamut.launcher;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.mastodon.ui.util.EverythingDisablerAndReenabler;
import org.mastodon.ui.util.FileChooser;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.TimePointsPattern;

class ImportTGMMPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final URL HELP_URL = ImportTGMMPanel.class.getResource( "ImportTGMMHelp.html" );

	private static String suggestedFile = null;

	final JTextArea textAreaTGMM;

	final JTextField filenamePatternTextField;

	final JComboBox< String > setupComboBox;

	final JTextField timepointPatternTextField;

	final JCheckBox covCheckBox;

	final JTextField covTextField;

	final JTextField nSigmasTextField;

	final JTextArea textAreaBDVFile;

	final JLabel labelInfo;

	final JButton btnImport;

	public ImportTGMMPanel()
	{

		final GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 70, 100 };
		layout.columnWeights = new double[] { 1., 0. };
		layout.rowHeights = new int[] { 0, 0, 75, 0, 0, 75 };
		setLayout( layout );

		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 5, 5, 5, 5 );
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;

		final JLabel lblTitle = new JLabel( "Import TGMM data" );
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
		add( new JLabel( "Browse to a TGMM folder:" ), c );

		textAreaTGMM = new JTextArea();
		textAreaTGMM.setLineWrap( true );
		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		add( textAreaTGMM, c );

		final JButton btnBrowseTGMM = new JButton( "browse" );
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		add( btnBrowseTGMM, c );

		c.gridy++;
		c.anchor = GridBagConstraints.SOUTHWEST;
		add( new JLabel( "Filename pattern:" ), c );

		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		filenamePatternTextField = new JTextField( "GMEMfinalResult_frame%04d.xml" );
		add( filenamePatternTextField, c );

		c.gridy++;
		c.anchor = GridBagConstraints.SOUTHWEST;
		add( new JLabel( "Timepoint pattern:" ), c );

		timepointPatternTextField = new JTextField( "0" );
		c.gridy++;;
		c.fill = GridBagConstraints.BOTH;
		add( timepointPatternTextField, c );

		c.gridy++;
		add( new JLabel( "Transform of setup:" ), c );

		setupComboBox = new JComboBox<>();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridy++;
		add( setupComboBox, c );

		c.gridy++;
		add( Box.createVerticalStrut( 15 ), c );

		c.gridy++;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;
		covCheckBox = new JCheckBox( "Set covariance:" );
		add( covCheckBox, c );

		covTextField = new JTextField( "1" );
		covTextField.setHorizontalAlignment( JTextField.RIGHT );
		c.gridx = 1;
		add( covTextField, c );

		c.gridy++;
		c.gridx = 0;
		add( new JLabel( "Covariance scale:" ), c );

		nSigmasTextField = new JTextField( "2" );
		nSigmasTextField.setHorizontalAlignment( JTextField.RIGHT );
		c.gridx = 1;
		add( nSigmasTextField, c );

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

		btnHelp.addActionListener( l -> LauncherUtil.showHelp( HELP_URL, "Help for the TGMM importer", this ) );
		btnBrowseBDV.addActionListener( l -> LauncherUtil.browseToBDVFile(
				suggestedFile,
				textAreaBDVFile,
				() -> checkBDVFile( true ),
				this ) );
		LauncherUtil.decorateJComponent( textAreaBDVFile, () -> checkBDVFile( true ) );
		LauncherUtil.decorateJComponent( timepointPatternTextField, () -> checkTGMMFolder() );
		btnBrowseTGMM.addActionListener( l -> browseToTGMMFolder() );
		covTextField.setEnabled( covCheckBox.isSelected() );
		covCheckBox.addActionListener( l -> covTextField.setEnabled( covCheckBox.isSelected() ) );
	}

	/**
	 * Checks whether the current set BDV file is a valid one.
	 *
	 * @param resetTimepointPattern
	 *                                  if <code>true</code>, will generate a
	 *                                  time-point pattern from the number of
	 *                                  time-points found in the BDV file.
	 * @return <code>true</code> if the the current set BDV file is a valid one.
	 */
	boolean checkBDVFile( final boolean resetTimepointPattern )
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

			if ( resetTimepointPattern )
			{
				final List< TimePoint > tps = seq.getTimePoints().getTimePointsOrdered();
				final int first = tps.get( 0 ).getId();
				boolean isOrdered = true;
				for ( int i = 1; i < tps.size(); ++i )
				{
					if ( tps.get( i ).getId() != tps.get( i - 1 ).getId() + 1 )
					{
						isOrdered = false;
						break;
					}
				}
				final String pattern;
				if ( isOrdered )
				{
					final int last = tps.get( tps.size() - 1 ).getId();
					pattern = Integer.toString( first ) + "-" + Integer.toString( last );
				}
				else
				{
					final StringBuilder sb = new StringBuilder();
					sb.append( tps.get( 0 ).getId() );
					for ( int i = 1; i < tps.size(); ++i )
					{
						sb.append( "," );
						sb.append( tps.get( i ).getId() );
					}
					pattern = sb.toString();
				}
				timepointPatternTextField.setText( pattern );
			}
			return true;
		}
		catch ( final SpimDataException | RuntimeException e )
		{
			labelInfo.setText( "<html>Invalid BDV xml/h5 file.<p>" + e.getMessage() + "</html>" );
			setupComboBox.setEnabled( false );
			return false;
		}
	}

	private void browseToTGMMFolder()
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( this, new Class[] { JLabel.class } );
		disabler.disable();
		final File file = FileChooser.chooseFile(
				this,
				suggestedFile,
				null,
				"Browse to a TGMM folder",
				FileChooser.DialogType.LOAD,
				FileChooser.SelectionMode.DIRECTORIES_ONLY );
		if ( file == null )
		{
			disabler.reenable();
			return;
		}
		suggestedFile = file.getAbsolutePath();
		textAreaTGMM.setText( file.getAbsolutePath() );
		checkTGMMFolder();
		disabler.reenable();
	}

	boolean checkTGMMFolder()
	{
		String tgmmFiles = textAreaTGMM.getText();
		if ( !tgmmFiles.endsWith( "/" ) )
			tgmmFiles = tgmmFiles + "/";
		tgmmFiles = tgmmFiles + filenamePatternTextField.getText();

		try
		{
			final TimePoints timepoints = new TimePointsPattern( timepointPatternTextField.getText() );
			final StringBuilder str = new StringBuilder();
			str.append( "<html><ul>" );
			int nFiles = 0;
			for ( final TimePoint timepoint : timepoints.getTimePointsOrdered() )
			{
				final int timepointId = timepoint.getId();
				final String tgmmFileName = String.format( tgmmFiles, timepointId );
				if ( !new File( tgmmFileName ).exists() )
					str.append( "<li>Cannot find file " + tgmmFileName + " in TGMM folder." );
				else if ( !new File( tgmmFileName ).canRead() )
					str.append( "<li>Cannot read file " + tgmmFileName + " in TGMM folder." );
				else
					nFiles++;
			}
			str.append( "</ul><p>" );
			str.append( "Found " + nFiles + " files matching the TGMM filename and timepoint patterns in folder.</html>" );
			labelInfo.setText( str.toString() );
			return true;
		}
		catch ( final ParseException | NumberFormatException e )
		{
			labelInfo.setText( "<html>Could not parse timepoint pattern.<p>" + e.getMessage() + "</html>" );
			return false;
		}
	}
}