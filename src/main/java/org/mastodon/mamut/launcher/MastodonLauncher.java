package org.mastodon.mamut.launcher;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.IntUnaryOperator;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.jdom2.JDOMException;
import org.mastodon.app.MastodonIcons;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.importer.simi.SimiImporter;
import org.mastodon.mamut.importer.simi.SimiImporter.LabelFunction;
import org.mastodon.mamut.importer.tgmm.TgmmImporter;
import org.mastodon.mamut.importer.trackmate.TrackMateImporter;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.project.MamutProject;
import org.mastodon.mamut.project.MamutProjectIO;
import org.mastodon.ui.util.EverythingDisablerAndReenabler;
import org.mastodon.ui.util.ExtensionFileFilter;
import org.mastodon.ui.util.FileChooser;
import org.mastodon.ui.util.FileChooser.SelectionMode;
import org.mastodon.ui.util.XmlFileFilter;
import org.scijava.Context;
import org.scijava.util.VersionUtils;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.TimePointsPattern;


public class MastodonLauncher extends JFrame
{

	private static final long serialVersionUID = 1L;

	static final String MASTODON_VERSION = VersionUtils.getVersion( MastodonLauncher.class );

	private final LauncherGUI gui;

	private final Context context;


	public MastodonLauncher(final Context context)
	{
		super("Mastodon launcher");
		this.context = Optional.ofNullable( context ).orElse( new Context() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		setIconImage( MastodonIcons.MASTODON_ICON_LARGE.getImage() );
		gui = new LauncherGUI();

		gui.btnNew.addActionListener( l -> newMastodonProject() );
		gui.btnLoad.addActionListener( l -> loadMastodonProject() );
		gui.btnImportTgmm.addActionListener( l -> showImportTgmmPanel() );
		gui.btnImportMamut.addActionListener( l -> importMaMuT() );
		gui.btnImportSimi.addActionListener( l -> showImportSimiPanel() );
		gui.btnHelp.addActionListener( l -> showHelpPanel() );

		gui.newMastodonProjectPanel.btnCreate.addActionListener( l -> createNewProject() );
		gui.importTGMMPanel.btnImport.addActionListener( l -> importTgmm() );
		gui.importSimiBioCellPanel.btnImport.addActionListener( l -> importSimi() );

		getContentPane().add( gui );
		setSize( 630, 660 );
	}

	private void importSimi()
	{
		if ( !gui.importSimiBioCellPanel.checkBDVFile() )
			return;

		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();

		new Thread( () -> {
			try
			{
				// Create new blank project from BDV file.
				final File bdvFile = new File( gui.importSimiBioCellPanel.textAreaBDVFile.getText() );
				final WindowManager windowManager = createWindowManager();
				windowManager.getProjectManager().open( new MamutProject( null, bdvFile ) );
				final Model model = windowManager.getAppModel().getModel();
				final AbstractSpimData< ? > spimData = windowManager.getAppModel().getSharedBdvData().getSpimData();

				final String sbdFilename = gui.importSimiBioCellPanel.textAreaSimiFile.getText();
				final int setupIndex = gui.importSimiBioCellPanel.setupComboBox.getSelectedIndex();
				final ViewRegistrations regs = spimData.getViewRegistrations();
				final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
				final List< TimePoint > timePointsOrdered = seq.getTimePoints().getTimePointsOrdered();
				final int maxtp = timePointsOrdered.size() - 1;
				final int setupID = seq.getViewSetupsOrdered().get( setupIndex ).getId();

				final int frameOffset = ( ( Number ) gui.importSimiBioCellPanel.spinnerTimeOffset.getValue() ).intValue();

				// maps frame to timepoint index
				final IntUnaryOperator frameToTimepointFunction = frame -> {
					int tp = frame - frameOffset;
					if ( tp < 0 )
					{
						gui.importSimiBioCellPanel.labelInfo.setText(
								"<html>WARNING: simi frame " + frame + " translates to out-of-bounds "
										+ "timepoint index " + tp + ". Clipping to 0.</html>" );
						tp = 0;
					}
					else if ( tp > maxtp )
					{
						gui.importSimiBioCellPanel.labelInfo.setText(
								"<html>WARNING: simi frame " + frame + " translates to out-of-bounds "
										+ "timepoint index " + tp + ". Clipping to " + maxtp + ".</html>" );
						tp = maxtp;
					}
					return tp;
				};

				final LabelFunction labelFunction = ( generic_name, generation_name, name ) -> {
					if ( name != null )
						return name;
					if ( generic_name != null )
						return generic_name;
					if ( generation_name != null )
						return generation_name;
					return null;
				};

				final BiFunction< Integer, double[], double[] > positionFunction = ( frame, lPos ) -> {
					final double[] gPos = new double[ 3 ];
					final int tp = frameToTimepointFunction.applyAsInt( frame );
					final int timepointId = timePointsOrdered.get( tp ).getId();
					regs.getViewRegistration( timepointId, setupID ).getModel().apply( lPos, gPos );
					return gPos;
				};
				final int radius = Integer.parseInt( gui.importSimiBioCellPanel.spotRadiusTextField.getText() );
				final boolean interpolateMissingSpots = gui.importSimiBioCellPanel.interpolateCheckBox.isSelected();
				SimiImporter.read( sbdFilename, frameToTimepointFunction, labelFunction, positionFunction, radius, interpolateMissingSpots, model );
				new MainWindow( windowManager ).setVisible( true );
				dispose();
			}
			catch ( final IOException e )
			{
				gui.importSimiBioCellPanel.labelInfo.setText(
						"<html>Problem reading the SimiBioCell file.<p>" + toHtml( e ) + "</html>" );
			}
			catch ( final ParseException e )
			{
				gui.importSimiBioCellPanel.labelInfo.setText(
						"<html>Problem parsing the SimiBioCell file.<p>" + toHtml( e ) + "</html>" );
			}
			catch ( final SpimDataException e )
			{
				gui.importSimiBioCellPanel.labelInfo.setText( "<html>Invalid BDV xml/h5 file.<p>" + toHtml( e ) + "</html>" );
			}
			finally
			{
				disabler.reenable();
			}
		} ).start();
	}

	private void importTgmm()
	{
		if ( !gui.importTGMMPanel.checkBDVFile( false ) || !gui.importTGMMPanel.checkTGMMFolder() )
			return;

		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();

		new Thread( () -> {
			try
			{
				String tgmmFiles = gui.importTGMMPanel.textAreaTGMM.getText();
				if ( !tgmmFiles.endsWith( "/" ) )
					tgmmFiles = tgmmFiles + "/";
				tgmmFiles = tgmmFiles + gui.importTGMMPanel.filenamePatternTextField.getText();

				// Create new blank project from BDV file.
				final File bdvFile = new File( gui.importTGMMPanel.textAreaBDVFile.getText() );
				final WindowManager windowManager = createWindowManager();
				windowManager.getProjectManager().open( new MamutProject( null, bdvFile ) );
				final Model model = windowManager.getAppModel().getModel();
				final AbstractSpimData< ? > spimData = windowManager.getAppModel().getSharedBdvData().getSpimData();

				// Read setup id.
				final ViewRegistrations viewRegistrations = spimData.getViewRegistrations();
				final int setupIndex = gui.importTGMMPanel.setupComboBox.getSelectedIndex();
				final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
				final int setupID = seq.getViewSetupsOrdered().get( setupIndex ).getId();

				// Run import.
				final TimePoints timepoints = new TimePointsPattern( gui.importTGMMPanel.timepointPatternTextField.getText() );
				final double nSigmas = Double.parseDouble( gui.importTGMMPanel.nSigmasTextField.getText() );
				if ( gui.importTGMMPanel.covCheckBox.isSelected() )
				{
					final double[][] cov = parseCov( gui.importTGMMPanel.covTextField.getText() );
					if ( null == cov )
					{
						gui.importTGMMPanel.labelInfo.setText( "<html>Cannot parse the covariance pattern.</html>" );
						return;
					}
					TgmmImporter.read( tgmmFiles, timepoints, TgmmImporter.getTimepointToIndex( spimData ), viewRegistrations, setupID, nSigmas, cov, model );
				}
				else
					TgmmImporter.read( tgmmFiles, timepoints, TgmmImporter.getTimepointToIndex( spimData ), viewRegistrations, setupID, nSigmas, model );

				// Success? We move on.
				new MainWindow( windowManager ).setVisible( true );
				dispose();
			}
			catch ( final ParseException e )
			{
				gui.importTGMMPanel.labelInfo.setText( "<html>Could not parse timepoint pattern.<p>" + toHtml( e ) + "</html>" );
			}
			catch ( JDOMException | IOException e )
			{
				gui.importTGMMPanel.labelInfo.setText( "<html>Malformed TGMM dataset.<p>" + toHtml( e ) + "</html>" );
			}
			catch ( final SpimDataException e )
			{
				gui.importTGMMPanel.labelInfo.setText( "<html>Invalid BDV xml/h5 file.<p>" + toHtml( e ) + "</html>" );
			}
			disabler.reenable();
		} ).start();
	}

	private double[][] parseCov( final String text )
	{
		try
		{
			final String[] entries = text.split( "\\s+" );
			if ( entries.length == 1 )
			{
				if ( !entries[ 0 ].isEmpty() )
				{
					final double v = Double.parseDouble( entries[ 0 ] );
					return new double[][] { { v, 0, 0 }, { 0, v, 0 }, { 0, 0, v } };
				}
			}
			else if ( entries.length == 3 )
			{
				final double[][] m = new double[ 3 ][ 3 ];
				for ( int r = 0; r < 3; ++r )
					m[ r ][ r ] = Double.parseDouble( entries[ r ] );
				return m;
			}
			else if ( entries.length == 9 )
			{
				final double[][] m = new double[ 3 ][ 3 ];
				for ( int r = 0; r < 3; ++r )
					for ( int c = 0; c < 3; ++c )
						m[ r ][ c ] = Double.parseDouble( entries[ r * 3 + c ] );
				return m;
			}
		}
		catch ( final Exception e )
		{
		}
		return null;
	}

	private void createNewProject()
	{
		if ( !gui.newMastodonProjectPanel.checkBDVFile() )
			return;

		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();
		final File file = new File( gui.newMastodonProjectPanel.textAreaFile.getText() );
		new Thread( () -> {
			try
			{
				final WindowManager windowManager = createWindowManager();
				windowManager.getProjectManager().open( new MamutProject( null, file ) );
				new MainWindow( windowManager ).setVisible( true );
				dispose();
			}
			catch ( IOException | SpimDataException e )
			{
				gui.newMastodonProjectPanel.labelInfo.setText( "<html>Invalid BDV xml/h5 file.<p>" + toHtml( e ) + "</html>" );
			}
			finally
			{
				disabler.reenable();
			}
		} ).start();
	}

	private void newMastodonProject()
	{
		gui.showPanel( LauncherGUI.NEW_MASTODON_PROJECT_KEY );
	}

	private void showImportTgmmPanel()
	{
		gui.showPanel( LauncherGUI.IMPORT_TGMM_KEY );
	}

	private void showImportSimiPanel()
	{
		gui.showPanel( LauncherGUI.IMPORT_SIMI_KEY );
	}

	private void showHelpPanel()
	{
		gui.showPanel( LauncherGUI.WELCOME_PANEL_KEY );
	}

	private void importMaMuT()
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();
		final File file = FileChooser.chooseFile(
				this,
				null,
				new XmlFileFilter(),
				"Import MaMuT Project",
				FileChooser.DialogType.LOAD );
		if ( file == null )
		{
			disabler.reenable();
			return;
		}

		gui.showPanel( LauncherGUI.IMPORT_MAMUT_KEY );
		new Thread( () -> {
			try
			{

				final TrackMateImporter importer = new TrackMateImporter( file );
				final WindowManager windowManager = createWindowManager();
				windowManager.getProjectManager().open( importer.createProject() );
				importer.readModel( windowManager.getAppModel().getModel(), windowManager.getFeatureSpecsService() );
				new MainWindow( windowManager ).setVisible( true );
				dispose();
			}
			catch ( final IOException | SpimDataException e )
			{
				gui.importMamutPanel.lblInfo.setText( "<html>Invalid MaMuT file.<p>" + toHtml( e ) + "</html>" );
			}
			finally
			{
				disabler.reenable();
			}
		} ).start();
	}

	private void loadMastodonProject()
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();
		gui.showPanel( LauncherGUI.LOAD_MASTODON_PROJECT_KEY );
		new Thread( () -> {
			try
			{
				gui.loadMastodonPanel.lblInfo.setText( "" );
				final WindowManager windowManager = createWindowManager();
				SwingUtilities.invokeLater( () -> {

					final File file = FileChooser.chooseFile(
							true, // We have to use the JFileChooser to open folders.
							this,
							null,
							new ExtensionFileFilter( "mastodon" ),
							"Open Mastodon Project",
							FileChooser.DialogType.LOAD,
							SelectionMode.FILES_AND_DIRECTORIES );
					if ( file == null )
						return;

					try
					{
						final MamutProject project = new MamutProjectIO().load( file.getAbsolutePath() );
						windowManager.getProjectManager().open( project );
						new MainWindow( windowManager ).setVisible( true );
						dispose();
					}
					catch ( final IOException | SpimDataException e )
					{
						gui.loadMastodonPanel.lblInfo.setText( "<html>Invalid Mastodon file.<p>" + toHtml( e ) + "</html>" );
					}
				} );
			}
			finally
			{
				disabler.reenable();
			}
		} ).start();
	}

	private WindowManager createWindowManager()
	{
		return new WindowManager( context );
	}

	private static final String toHtml( final Exception e )
	{
		return e.getMessage()
				.replace( "<", "&lt;" )
				.replace( ">", "&gt;" );
	}
}