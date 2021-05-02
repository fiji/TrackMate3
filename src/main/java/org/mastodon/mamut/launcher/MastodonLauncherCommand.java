package org.mastodon.mamut.launcher;

import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class, menuPath = "Plugins>Mastodon" )
public class MastodonLauncherCommand extends ContextCommand
{

	@Override
	public void run()
	{
		setSystemLookAndFeelAndLocale();
		final MastodonLauncher launcher = new MastodonLauncher( getContext() );
		launcher.setLocationByPlatform( true );
		launcher.setLocationRelativeTo( null );
		launcher.setVisible( true );
	}

	public static void main( final String[] args )
	{
		setSystemLookAndFeelAndLocale();
		final MastodonLauncherCommand launcher = new MastodonLauncherCommand();
		new Context().inject( launcher );
		launcher.run();
	}

	private static final void setSystemLookAndFeelAndLocale()
	{
		Locale.setDefault( Locale.ROOT );
		try
		{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}
		catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e )
		{
			e.printStackTrace();
		}
	}
}
