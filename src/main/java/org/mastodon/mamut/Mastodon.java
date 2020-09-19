package org.mastodon.mamut;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.mastodon.mamut.project.MamutProject;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Plugin;

import mpicbg.spim.data.SpimDataException;

@Plugin( type = Command.class, menuPath = "Plugins>Mastodon (preview)" )
public class Mastodon extends ContextCommand
{
	private WindowManager windowManager;

	private MainWindow mainWindow;

	@Override
	public void run()
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		windowManager = new WindowManager( getContext() );
		mainWindow = new MainWindow( windowManager );
		mainWindow.setVisible( true );
	}

	// FOR TESTING ONLY!
	public void openProject( final MamutProject project ) throws IOException, SpimDataException
	{
		windowManager.projectManager.open( project );
	}

	// FOR TESTING ONLY!
	public void setExitOnClose()
	{
		mainWindow.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
	}

	// FOR TESTING ONLY!
	public WindowManager getWindowManager()
	{
		return windowManager;
	}

	public static void main( final String[] args ) throws Exception
	{
		Locale.setDefault( Locale.US );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final Mastodon mastodon = new Mastodon();
		new Context().inject( mastodon );
		mastodon.run();
		mastodon.mainWindow.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

		final WindowManager windowManager = mastodon.windowManager;

		final String bdvFile = "samples/datasethdf5.xml";
		final MamutProject project = new MamutProject( null, new File( bdvFile ) );
//		final String bdvFile = "samples/datasethdf5.xml";
//		final String bdvFile = "/Users/tinevez/Projects/JYTinevez/MaMuT/MaMuT_demo_dataset/MaMuT_Parhyale_demo.xml";
//		final MamutProject project = new MamutProject( null, new File( bdvFile ) );
//		final MamutProject project = new MamutProjectIO().load( "/Volumes/External/Data/Mastodon/Tassos200" );
//		final MamutProject project = new MamutProject( null, new File( "x=1000 y=1000 z=100 sx=1 sy=1 sz=10 t=400.dummy" ) );
//		final MamutProject project = new MamutProjectIO().load( "samples/drosophila_crop.mastodon" );

		windowManager.projectManager.open( project );
//		mw.proposedProjectFile = new File( "/Users/pietzsch/Desktop/data/TGMM_METTE/project2.xml" );
//		mw.loadProject( new File( "/Users/pietzsch/Desktop/data/TGMM_METTE/project.xml" ) );
//		mw.createProject();
//		mw.loadProject();
		SwingUtilities.invokeAndWait( () -> {
			windowManager.createBigDataViewer();
			windowManager.createTrackScheme();
//			YamlConfigIO.write( new InputTriggerDescriptionsBuilder( keyconf ).getDescriptions(), new PrintWriter( System.out ) );
		} );

//		DumpInputConfig.writeDefaultConfigToYaml( System.getProperty( "user.home" ) + "/Desktop/DEFAULT.keyconfig.yaml", new Context() );
	}
}
