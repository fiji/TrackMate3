package org.mastodon.util;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Set of static utilities related to key presses, actions and behaviors
 * management.
 */
public class KeyConfigUtils
{
	/**
	 * Preserves the specified component from behavior and action related key
	 * presses. This useful <i>e.g.</i> for text fields; user input can be
	 * confused with behaviors key shortcuts.
	 * <p>
	 * Adapted from Jan Funke's code in <a href=
	 * "https://github.com/saalfeldlab/bigcat/blob/janh5/src/main/java/bdv/bigcat/ui/BigCatTable.java#L112-L143">
	 * BigCat repo</a>
	 *
	 * @param ctn
	 *            the JComponent to preserve from key presses.
	 */
	public static void blockKeys( final JComponent ctn )
	{
		// Get all keystrokes that are mapped to actions in higher components
		final ArrayList< KeyStroke > allTableKeys = new ArrayList<>();
		for ( Container c = ctn.getParent(); c != null; c = c.getParent() )
		{
			if ( c instanceof JComponent )
			{
				final InputMap inputMap = ( ( JComponent ) c ).getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
				final KeyStroke[] tableKeys = inputMap.allKeys();
				if ( tableKeys != null )
					allTableKeys.addAll( Arrays.asList( tableKeys ) );
			}
		}

		// An action that does nothing. We can not just map to "none",
		// as this is not interrupting the action-name -> action search.
		// We have to map to a proper action, "nothing" in this case.
		final Action nada = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{}
		};
		ctn.getActionMap().put( "nothing", nada );

		// Replace every table key binding with nothing, thus creating an
		// event-barrier.
		final InputMap inputMap = ctn.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		for ( final KeyStroke key : allTableKeys )
			inputMap.put( key, "nothing" );

		ctn.getActionMap().put( "nothing", nada );
	}

	private KeyConfigUtils()
	{}
}