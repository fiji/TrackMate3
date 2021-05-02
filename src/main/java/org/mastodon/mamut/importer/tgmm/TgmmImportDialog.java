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
package org.mastodon.mamut.importer.tgmm;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import org.jdom2.JDOMException;
import org.mastodon.app.MastodonIcons;
import org.mastodon.mamut.model.Model;
import org.mastodon.ui.util.FileChooser;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.TimePointsPattern;

public class TgmmImportDialog
{
	private final Frame owner;

	private AbstractSpimData< ? > spimData;

	private Model model;

	private Dialog dialog;

	public TgmmImportDialog( final Frame owner )
	{
		this.owner = owner;
	}

	public synchronized void showImportDialog( final AbstractSpimData< ? > spimData, final Model model )
	{
		if ( dialog == null )
			dialog = new Dialog();

		this.spimData = spimData;
		this.model = model;
		dialog.showImportDialog();
	}

	class Dialog extends JDialog
	{
		private static final long serialVersionUID = 1L;

		private final JTextField pathTextField;

		private final JTextField filenamePatternTextField;

		private final JSpinner spinnerSetup;

		private final JTextField timepointPatternTextField;

		private final JCheckBox covCheckBox;

		private final JTextField covTextField;

		private final JTextField nSigmasTextField;

		Dialog()
		{
			super( owner, true );
			setIconImage( MastodonIcons.TGMM_IMPORT_ICON_MEDIUM.getImage() );
			setLocationByPlatform( true );
			setLocationRelativeTo( null );

			final JPanel content = new JPanel();
			getContentPane().add( content, BorderLayout.CENTER );
			content.setLayout( new GridBagLayout() );

			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets( 0, 5, 0, 5 );
			c.ipadx = 0;
			c.ipady = 0;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.HORIZONTAL;

			c.gridy = 0;
			c.gridx = 0;
			content.add( new JLabel( "import from" ), c );
			pathTextField = new JTextField( "/Users/pietzsch/Downloads/data/TGMMruns_testRunToCheckOutput/XML_finalResult_lht/" );
			c.gridx = 1;
			content.add( pathTextField, c );
			pathTextField.setColumns( 20 );
			final JButton browseButton = new JButton( "Browse" );
			c.gridx = 2;
			content.add( browseButton, c );

			c.gridy++;
			c.gridx = 0;
			content.add( new JLabel( "filename pattern" ), c );
			filenamePatternTextField = new JTextField( "GMEMfinalResult_frame%04d.xml" );
			c.gridx = 1;
			content.add( filenamePatternTextField, c );

			c.gridy++;
			c.gridx = 0;
			content.add( new JLabel( "transform of setup" ), c );
			spinnerSetup = new JSpinner();
			spinnerSetup.setModel( new SpinnerNumberModel( 0, 0, 0, 1 ) );
			c.gridx = 1;
			content.add( spinnerSetup, c );

			c.gridy++;
			c.gridx = 0;
			content.add( new JLabel( "timepoint pattern" ), c );
			timepointPatternTextField = new JTextField( "0" );
			c.gridx = 1;
			content.add( timepointPatternTextField, c );

			c.gridy++;
			c.gridx = 0;
			content.add( Box.createVerticalStrut( 15 ), c );

			c.gridy++;
			c.gridx = 0;
			covCheckBox = new JCheckBox( "set covariance" );
			content.add( covCheckBox, c );
			covTextField = new JTextField( "1" );
			c.gridx = 1;
			content.add( covTextField, c );

			c.gridy++;
			c.gridx = 0;
			content.add( new JLabel( "cov scale" ), c );
			nSigmasTextField = new JTextField( "2" );
			c.gridx = 1;
			content.add( nSigmasTextField, c );

			c.gridy++;
			c.gridx = 0;
			content.add( Box.createVerticalStrut( 15 ), c );

			c.gridy++;
			c.gridx = 1;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.EAST;
			final JButton importButton = new JButton( "Import" );
			content.add( importButton, c );

			c.gridx = 2;
			c.anchor = GridBagConstraints.WEST;
			final JButton cancelButton = new JButton( "Cancel" );
			content.add( cancelButton, c );

			browseButton.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					final File file = FileChooser.chooseFile(
							Dialog.this,
							pathTextField.getText(),
							null,
							null,
							FileChooser.DialogType.LOAD,
							FileChooser.SelectionMode.DIRECTORIES_ONLY );
					if ( file != null )
						pathTextField.setText( file.getAbsolutePath() );
				}
			} );

			importButton.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					importButton.setEnabled( false );
					cancelButton.setEnabled( false );
					doImport();
					setVisible( false );
					importButton.setEnabled( true );
					cancelButton.setEnabled( true );
				}
			} );

			cancelButton.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					setVisible( false );
				}
			} );

			pack();
			setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		}

		private void doImport()
		{
			try
			{
				String tgmmFiles = pathTextField.getText();
				if ( !tgmmFiles.endsWith( "/" ) )
					tgmmFiles = tgmmFiles + "/";
				tgmmFiles = tgmmFiles + filenamePatternTextField.getText();

				final TimePoints timepoints = new TimePointsPattern( timepointPatternTextField.getText() );

				final ViewRegistrations viewRegistrations = spimData.getViewRegistrations();

				final int setupIndex = ( Integer ) spinnerSetup.getValue();
				final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
				final int setupID = seq.getViewSetupsOrdered().get( setupIndex ).getId();

				final double nSigmas = Double.parseDouble( nSigmasTextField.getText() );

				if ( covCheckBox.isSelected() )
				{
					final double[][] cov = parseCov( covTextField.getText() );
					TgmmImporter.read( tgmmFiles, timepoints, TgmmImporter.getTimepointToIndex( spimData ), viewRegistrations, setupID, nSigmas, cov, model );
				}
				else
					TgmmImporter.read( tgmmFiles, timepoints, TgmmImporter.getTimepointToIndex( spimData ), viewRegistrations, setupID, nSigmas, model );
			}
			catch ( final ParseException | JDOMException | IOException e )
			{
				e.printStackTrace();
			}
		}

		private double[][] parseCov( final String text ) throws ParseException
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
			{}
			throw new ParseException( "Cannot parse: '" + text + "'", 0 );
		}

		void showImportDialog()
		{
			final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
			final int numSetups = seq.getViewSetupsOrdered().size();
			spinnerSetup.setModel( new SpinnerNumberModel( 0, 0, numSetups - 1, 1 ) );

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
			setVisible( true );
		}
	}
}
