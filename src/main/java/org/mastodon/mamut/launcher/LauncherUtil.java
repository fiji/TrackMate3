package org.mastodon.mamut.launcher;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.mastodon.app.MastodonIcons;
import org.mastodon.ui.util.EverythingDisablerAndReenabler;
import org.mastodon.ui.util.FileChooser;
import org.mastodon.ui.util.XmlFileFilter;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.spimdata.SpimDataMinimal;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;

class LauncherUtil
{

	static final void showHelp( final URL helpURL, final String title, final Component parent )
	{

		final JEditorPane editorPane = new JEditorPane();
		editorPane.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		editorPane.setEditable( false );
		editorPane.addHyperlinkListener( new HyperlinkListener()
		{
			@Override
			public void hyperlinkUpdate( final HyperlinkEvent e )
			{
				if ( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && Desktop.isDesktopSupported() )
				{
					try
					{
						Desktop.getDesktop().browse( e.getURL().toURI() );
					}
					catch ( IOException | URISyntaxException e1 )
					{
						e1.printStackTrace();
					}
				}
			}
		} );

		try

		{
			editorPane.setPage( helpURL );
		}
		catch (

		final IOException e )
		{
			editorPane.setText( "Attempted to read a bad URL: " + helpURL );
		}

		final JScrollPane editorScrollPane = new JScrollPane( editorPane );

		editorScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		editorScrollPane.setPreferredSize( new Dimension( 600, 300 ) );
		editorScrollPane.setMinimumSize( new Dimension( 10, 10 ) );

		final JFrame f = new JFrame();
		f.setIconImage( MastodonIcons.MASTODON_ICON_MEDIUM.getImage() );
		f.setTitle( title );
		f.setSize( 600, 400 );
		f.setLocationRelativeTo( parent );
		f.getContentPane().add( editorScrollPane, BorderLayout.CENTER );
		f.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		f.setResizable( true );
		f.setVisible( true );
	}

	static final void browseToBDVFile( final String suggestedFile, final JTextArea target, final Runnable onSucess, final JComponent parent )
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( parent, new Class[] { JLabel.class } );
		disabler.disable();
		try
		{
			final File file = FileChooser.chooseFile(
					parent,
					suggestedFile,
					new XmlFileFilter(),
					"Open BigDataViewer File",
					FileChooser.DialogType.LOAD );
			if ( file == null )
				return;

			target.setText( file.getAbsolutePath() );
			onSucess.run();
		}
		finally
		{
			disabler.reenable();
		}
	}

	static final void decorateJComponent( final JComponent component, final Runnable toExecute )
	{
		final KeyStroke enter = KeyStroke.getKeyStroke( "ENTER" );
		final String TEXT_SUBMIT = "PRESSED_ENTER";
		final InputMap input = component.getInputMap();
		input.put( enter, TEXT_SUBMIT );
		final ActionMap actions = component.getActionMap();
		actions.put( TEXT_SUBMIT, new RunnableAction( "EnterPressed", toExecute ) );
	}

	static final String buildInfoString( final SpimDataMinimal spimData )
	{
		final StringBuilder str = new StringBuilder();
		str.append( "<html>" );
		str.append( "<ul>" );

		final int nTimePoints = spimData.getSequenceDescription().getTimePoints().size();
		str.append( "<li>N time-points: " + nTimePoints );

		final int nViews = spimData.getSequenceDescription().getViewSetupsOrdered().size();
		str.append( "<li>N views: " + nViews );

		if ( nViews > 0 )
		{
			str.append( "<ol start=\"0\">" );
			for ( int i = 0; i < nViews; i++ )
			{
				final BasicViewSetup setup = spimData.getSequenceDescription().getViewSetupsOrdered().get( i );
				str.append( "<li>" );
				if ( setup.hasName() )
					str.append( setup.getName() + ": " );

				if ( setup.hasSize() && setup.getSize().numDimensions() > 0 )
				{
					final Dimensions size = setup.getSize();
					for ( int d = 0; d < size.numDimensions(); d++ )
						str.append( size.dimension( d ) + " x " );
					str.delete( str.length() - 3, str.length() );

					if ( setup.hasVoxelSize() && setup.getVoxelSize().numDimensions() > 0 )
					{
						str.append( "; " );
						final VoxelDimensions voxelSize = setup.getVoxelSize();
						for ( int d = 0; d < voxelSize.numDimensions(); d++ )
							str.append( voxelSize.dimension( d ) + " x " );

						str.delete( str.length() - 2, str.length() );
						str.append( voxelSize.unit() );
					}
				}

			}
			str.append( "</ol>" );
		}

		str.append( "</ul>" );
		str.append( "</html>" );
		return str.toString();
	}
}