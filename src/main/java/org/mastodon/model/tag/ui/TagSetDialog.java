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
package org.mastodon.model.tag.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.mastodon.app.ui.CloseWindowActions;
import org.mastodon.app.ui.settings.SimpleSettingsPage;
import org.mastodon.app.ui.settings.SingleSettingsPanel;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.model.tag.TagSetStructure;
import org.mastodon.model.tag.TagSetStructure.TagSet;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.undo.UndoPointMarker;
import org.scijava.ui.behaviour.util.Actions;

public class TagSetDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	private final TagSetDialog.TagSetManager manager;

	private final ArrayList< Runnable > runOnDispose;

	private final TagSetPanel tagSetPanel;

	public interface TagSetManager
	{
		TagSetStructure getTagSetStructure();

		void setTagSetStructure( final TagSetStructure tagSetStructure );
	}

	public TagSetDialog(
			final Frame owner,
			final TagSetModel< ?, ? > model,
			final UndoPointMarker undoPointMarker,
			final Keymap keymap,
			final String[] keyConfigContexts )
	{
		this( owner, new TagSetManager() {
			@Override
			public TagSetStructure getTagSetStructure()
			{
				return model.getTagSetStructure();
			}

			@Override
			public void setTagSetStructure( final TagSetStructure tagSetStructure )
			{
				System.out.println( "TagSetDialog.setTagSetStructure" );
				model.setTagSetStructure( tagSetStructure );
				undoPointMarker.setUndoPoint();
			}
		} );
		setLocationByPlatform( true );
		setLocationRelativeTo( null );
		model.listeners().add( () -> tagSetPanel.setTagSetStructure( manager.getTagSetStructure() ) );

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Actions actions = new Actions( im, am, keymap.getConfig(), keyConfigContexts );
		CloseWindowActions.install( actions, this );

		final Keymap.UpdateListener listener = () -> actions.updateKeyConfig( keymap.getConfig() );
		keymap.updateListeners().add( listener );
		runOnDispose.add( () -> keymap.updateListeners().remove( listener ) );
	}

	@Override
	public void dispose()
	{
		runOnDispose.forEach( Runnable::run );
		runOnDispose.clear();
		super.dispose();
	}

	public TagSetDialog(
			final Frame owner,
			final TagSetManager manager )
	{
		super( owner, "Configure Tag Sets", false );
		this.manager = manager;
		this.runOnDispose = new ArrayList<>();

		tagSetPanel = new TagSetPanel();
		tagSetPanel.setTagSetStructure( manager.getTagSetStructure() );
		final SimpleSettingsPage page = new SimpleSettingsPage( "tag sets", tagSetPanel );

		tagSetPanel.updateListeners().add( () -> page.notifyModified() );
		page.onApply( () -> {
			tagSetPanel.stopEditing();
			manager.setTagSetStructure( tagSetPanel.getTagSetStructure() );
		} );
		page.onCancel( () -> {
			tagSetPanel.cancelEditing();
			tagSetPanel.setTagSetStructure( manager.getTagSetStructure() );
		} );

		final SingleSettingsPanel settingsPanel = new SingleSettingsPanel( page );
		settingsPanel.onOk( () -> setVisible( false ) );
		settingsPanel.onCancel( () -> setVisible( false ) );

		setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				settingsPanel.cancel();
			}
		} );

		getContentPane().add( settingsPanel, BorderLayout.CENTER );
		pack();
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final TagSetStructure tss = new TagSetStructure();

		final Random ran = new Random( 0l );
		final TagSet reviewedByTag = tss.createTagSet( "Reviewed by" );
		reviewedByTag.createTag( "Pavel", ran.nextInt() | 0xFF000000 );
		reviewedByTag.createTag( "Mette", ran.nextInt() | 0xFF000000 );
		reviewedByTag.createTag( "Tobias", ran.nextInt() | 0xFF000000 );
		reviewedByTag.createTag( "JY", ran.nextInt() | 0xFF000000 );
		final TagSet locationTag = tss.createTagSet( "Location" );
		locationTag.createTag( "Anterior", ran.nextInt() | 0xFF000000 );
		locationTag.createTag( "Posterior", ran.nextInt() | 0xFF000000 );

		final TagSetManager manager = new TagSetManager()
		{
			@Override
			public TagSetStructure getTagSetStructure()
			{
				return tss;
			}

			@Override
			public void setTagSetStructure( final TagSetStructure tagSetStructure )
			{
				tss.set( tagSetStructure );
			}
		};

		final TagSetDialog frame = new TagSetDialog( null, manager );
		frame.setVisible( true );
	}
}
