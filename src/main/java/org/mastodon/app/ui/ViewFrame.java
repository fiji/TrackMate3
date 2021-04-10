package org.mastodon.app.ui;

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
import net.imglib2.ui.util.GuiUtil;

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
		super( windowTitle, GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL ) );
		getRootPane().setDoubleBuffered( true );

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

	public JPanel getSettingsPanel()
	{
		return settingsPanel;
	}
}
