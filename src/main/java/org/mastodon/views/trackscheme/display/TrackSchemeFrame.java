package org.mastodon.views.trackscheme.display;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.mastodon.app.ui.GroupLocksPanel;
import org.mastodon.app.ui.ViewFrame;
import org.mastodon.grouping.GroupHandle;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.model.TimepointModel;
import org.mastodon.ui.context.ContextChooserPanel;
import org.mastodon.undo.UndoPointMarker;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.trackscheme.TrackSchemeEdge;
import org.mastodon.views.trackscheme.TrackSchemeGraph;
import org.mastodon.views.trackscheme.TrackSchemeVertex;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

public class TrackSchemeFrame extends ViewFrame
{
	private static final long serialVersionUID = 1L;

	private final TrackSchemePanel trackschemePanel;

	public TrackSchemeFrame(
			final TrackSchemeGraph< ?, ? > graph,
			final HighlightModel< TrackSchemeVertex, TrackSchemeEdge > highlight,
			final FocusModel< TrackSchemeVertex, TrackSchemeEdge > focus,
			final TimepointModel timepoint,
			final SelectionModel< TrackSchemeVertex, TrackSchemeEdge > selection,
			final NavigationHandler< TrackSchemeVertex, TrackSchemeEdge > navigation,
			final UndoPointMarker undoPointMarker,
			final GroupHandle groupHandle,
			final ContextChooser< ? > contextChooser,
			final TrackSchemeOptions optional )
	{
		super( "TrackScheme" );

		trackschemePanel = new TrackSchemePanel(
				graph,
				highlight,
				focus,
				timepoint,
				selection,
				navigation,
				optional );
		add( trackschemePanel, BorderLayout.CENTER );

		final GroupLocksPanel navigationLocksPanel = new GroupLocksPanel( groupHandle );
		settingsPanel.add( navigationLocksPanel );
		settingsPanel.add( Box.createHorizontalGlue() );

		final ContextChooserPanel< ? > contextChooserPanel = new ContextChooserPanel<>( contextChooser );
		settingsPanel.add( contextChooserPanel );

		pack();
		setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				trackschemePanel.stop();
			}
		} );

		SwingUtilities.replaceUIActionMap( trackschemePanel, keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( trackschemePanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		mouseAndKeyHandler.setKeypressManager( optional.values.getKeyPressedManager(), trackschemePanel.getDisplay() );
		trackschemePanel.getDisplay().addHandler( mouseAndKeyHandler );
		setLocation( optional.values.getX(), optional.values.getY() );
	}

	public TrackSchemePanel getTrackschemePanel()
	{
		return trackschemePanel;
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}

	public TriggerBehaviourBindings getTriggerbindings()
	{
		return triggerbindings;
	}
}
