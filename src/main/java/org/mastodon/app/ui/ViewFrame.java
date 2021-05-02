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
package org.mastodon.app.ui;

import bdv.util.AWTUtils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.util.InvokeOnEDT;

/**
 * A {@code JFrame} with some stuff added. Used to display
 * {@link MastodonFrameView}.
 *
 * @author Tobias Pietzsch
 */
public class ViewFrame extends JFrame
{
	private static final long serialVersionUID = 1L;

	protected final JPanel settingsPanel;

	private boolean isSettingsPanelVisible;

	protected final InputActionBindings keybindings;

	protected final TriggerBehaviourBindings triggerbindings;

	protected final JMenuBar menubar;

	public ViewFrame( final String windowTitle )
	{
		super( windowTitle, AWTUtils.getSuitableGraphicsConfiguration( AWTUtils.RGB_COLOR_MODEL ) );
		getRootPane().setDoubleBuffered( true );
		setLocationByPlatform( true );
		setLocationRelativeTo( null );

		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();

		settingsPanel = new JPanel();
		settingsPanel.setLayout( new BoxLayout( settingsPanel, BoxLayout.LINE_AXIS ) );
		add( settingsPanel, BorderLayout.NORTH );
		isSettingsPanelVisible = true;

		SwingUtilities.replaceUIActionMap( settingsPanel, keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( settingsPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		menubar = new JMenuBar();
		setJMenuBar( menubar );
	}

	public boolean isSettingsPanelVisible()
	{
		return isSettingsPanelVisible;
	}

	public void setSettingsPanelVisible( final boolean visible )
	{
		try
		{
			InvokeOnEDT.invokeAndWait( () -> setSettingsPanelVisibleSynchronized( visible ) );
		}
		catch ( InvocationTargetException | InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	private synchronized void setSettingsPanelVisibleSynchronized( final boolean visible )
	{
		if ( isSettingsPanelVisible != visible )
		{
			final Dimension size = getSize();
			isSettingsPanelVisible = visible;
			if ( visible )
			{
				settingsPanel.setVisible( true );
				add( settingsPanel, BorderLayout.NORTH );
			}
			else
			{
				remove( settingsPanel );
				settingsPanel.setVisible( false );
			}
			invalidate();
			setPreferredSize( size );
			pack();
		}
	}

}
