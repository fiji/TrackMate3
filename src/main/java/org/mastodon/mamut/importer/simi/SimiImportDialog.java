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
package org.mastodon.mamut.importer.simi;

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
import java.util.function.BiFunction;
import java.util.function.IntUnaryOperator;

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

import org.mastodon.app.MastodonIcons;
import org.mastodon.mamut.model.Model;
import org.mastodon.ui.util.ExtensionFileFilter;
import org.mastodon.ui.util.FileChooser;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;

public class SimiImportDialog
{
	private final Frame owner;

	private AbstractSpimData< ? > spimData;

	private Model model;

	private Dialog dialog;

	public SimiImportDialog( final Frame owner )
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

		private final JSpinner spinnerSetup;

		private final JTextField frameOffsetTextField;

		private final JCheckBox interpolateCheckbox;

		private final JTextField radiusTextField;

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
			pathTextField = new JTextField( "/Users/pietzsch/Desktop/SIMI/040612_300312_A190_TL71-418_AP_DV_boundary_curated.sbd" );
			c.gridx = 1;
			content.add( pathTextField, c );
			pathTextField.setColumns( 20 );
			final JButton browseButton = new JButton( "Browse" );
			c.gridx = 2;
			content.add( browseButton, c );

			c.gridy++;
			c.gridx = 0;
			content.add( new JLabel( "transform of setup" ), c );
			spinnerSetup = new JSpinner();
			spinnerSetup.setModel( new SpinnerNumberModel( 0, 0, 0, 1 ) );
			c.gridx = 1;
			content.add( spinnerSetup, c );

			c.gridy++;
			c.gridx = 0;
			content.add( new JLabel( "time offset (simi frame - bdv timepoint)" ), c );
			frameOffsetTextField = new JTextField( "0" );
			c.gridx = 1;
			content.add( frameOffsetTextField, c );

			c.gridy++;
			c.gridx = 1;
			interpolateCheckbox = new JCheckBox( "interpolate missing frames", true );
			content.add( interpolateCheckbox, c );

			c.gridy++;
			c.gridx = 0;
			content.add( new JLabel( "spot radius" ), c );
			radiusTextField = new JTextField( "10" );
			c.gridx = 1;
			content.add( radiusTextField, c );

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
							null,
							new ExtensionFileFilter( "sbd" ),
							"Open .sbd File",
							FileChooser.DialogType.LOAD );
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
				final String sbdFilename = pathTextField.getText();

				final int setupIndex = ( Integer ) spinnerSetup.getValue();
				final ViewRegistrations regs = spimData.getViewRegistrations();
				final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
				final List< TimePoint > timePointsOrdered = seq.getTimePoints().getTimePointsOrdered();
				final int maxtp = timePointsOrdered.size() - 1;
				final int setupID = seq.getViewSetupsOrdered().get( setupIndex ).getId();

				final int frameOffset = Integer.parseInt( frameOffsetTextField.getText() );

				// maps frame to timepoint index
				final IntUnaryOperator frameToTimepointFunction = frame -> {
					int tp = frame - frameOffset;
					if ( tp < 0 )
					{
						System.err.println( "WARNING: simi frame " + frame + " translates to out-of-bounds timepoint index " + tp + ". Clipping to 0." );
						tp = 0;
					}
					else if ( tp > maxtp )
					{
						System.err.println( "WARNING: simi frame " + frame + " translates to out-of-bounds timepoint index " + tp + ". Clipping to " + maxtp + "." );
						tp = maxtp;
					}
					return tp;
				};
				final SimiImporter.LabelFunction labelFunction = ( generic_name, generation_name, name ) -> {
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
				final int radius = Integer.parseInt( radiusTextField.getText() );
				final boolean interpolateMissingSpots = interpolateCheckbox.isSelected();
				SimiImporter.read( sbdFilename, frameToTimepointFunction, labelFunction, positionFunction, radius, interpolateMissingSpots, model );
			}
			catch ( final ParseException | IOException e )
			{
				e.printStackTrace();
			}
		}

		void showImportDialog()
		{
			final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
			final int numSetups = seq.getViewSetupsOrdered().size();
			spinnerSetup.setModel( new SpinnerNumberModel( 0, 0, numSetups - 1, 1 ) );
			setVisible( true );
		}
	}
}
