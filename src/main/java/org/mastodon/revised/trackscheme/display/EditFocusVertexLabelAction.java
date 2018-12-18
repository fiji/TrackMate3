package org.mastodon.revised.trackscheme.display;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.mastodon.model.FocusModel;
import org.mastodon.revised.mamut.KeyConfigContexts;
import org.mastodon.revised.trackscheme.ScreenTransform;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.trackscheme.display.OffsetHeaders.OffsetHeadersListener;
import org.mastodon.revised.ui.keymap.CommandDescriptionProvider;
import org.mastodon.revised.ui.keymap.CommandDescriptions;
import org.mastodon.undo.UndoPointMarker;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

import net.imglib2.ui.TransformListener;

/**
 * Edit vertex label in TrackScheme.
 *
 * @author Jean-Yves Tinevez
 */
public class EditFocusVertexLabelAction extends AbstractNamedAction implements TransformListener< ScreenTransform >, OffsetHeadersListener
{
	private static final long serialVersionUID = 1L;

	public static final String EDIT_FOCUS_LABEL = "edit vertex label";

	private static final String[] EDIT_FOCUS_LABEL_KEYS = new String[] { "ENTER" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.TRACKSCHEME );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( EDIT_FOCUS_LABEL, EDIT_FOCUS_LABEL_KEYS, "Edit the label of the current spot." );
		}
	}

	private static final Font FONT = new Font( "SansSerif", Font.BOLD, 10 );

	private final UndoPointMarker undoPointMarker;

	private final TrackSchemePanel panel;

	private final ScreenTransform screenTransform;

	private Editor editor;

	private final FontMetrics fontMetrics;

	private final FocusModel< TrackSchemeVertex > focus;

	/**
	 * current width of vertical header.
	 */
	private int headerWidth;

	/**
	 * current height of horizontal header.
	 */
	private int headerHeight;

	public static void install(
			final Actions actions,
			final TrackSchemePanel panel,
			final FocusModel< TrackSchemeVertex > focus,
			final UndoPointMarker undoPointMarker )
	{
		final EditFocusVertexLabelAction editFocusVertexLabelAction = new EditFocusVertexLabelAction( focus, undoPointMarker, panel );
		panel.getDisplay().addTransformListener( editFocusVertexLabelAction );
		panel.getOffsetHeaders().listeners().add( editFocusVertexLabelAction );
		actions.namedAction( editFocusVertexLabelAction, EDIT_FOCUS_LABEL_KEYS );
	}

	private EditFocusVertexLabelAction(
			final FocusModel< TrackSchemeVertex > focus,
			final UndoPointMarker undoPointMarker,
			final TrackSchemePanel panel )
	{
		super( EDIT_FOCUS_LABEL );
		this.focus = focus;
		this.panel = panel;
		this.undoPointMarker = undoPointMarker;
		this.screenTransform = new ScreenTransform();
		this.fontMetrics = panel.getDisplay().getFontMetrics( FONT );
	}

	@Override
	public void actionPerformed( final ActionEvent e )
	{
		final TrackSchemeVertex ref = panel.getGraph().vertexRef();
		final TrackSchemeVertex vertex = focus.getFocused( ref );
		if ( vertex == null )
		{
			panel.getGraph().releaseRef( ref );
			return;
		}
		if ( null != editor )
		{
			editor.kill();
		}

		editor = new Editor( vertex );
		// vertex ref will have to be released in the editor class.
		panel.getDisplay().add( editor );
		editor.blockKeys();
		panel.getDisplay().repaint();
	}

	@Override
	public void transformChanged( final ScreenTransform transform )
	{
		synchronized ( screenTransform )
		{
			screenTransform.set( transform );
		}
		if ( null != editor )
		{
			editor.reposition();
			panel.getDisplay().repaint();
		}
	}

	@Override
	public void updateHeaderSize( final int width, final int height )
	{
		headerWidth = width;
		headerHeight = height;
	}

	private class Editor extends JTextField
	{
		private static final long serialVersionUID = 1L;

		private final TrackSchemeVertex vertex;

		private boolean killed = false;

		public Editor( final TrackSchemeVertex vertex )
		{
			this.vertex = vertex;
			final String label = vertex.getLabel();
			setText( label );
			setHorizontalAlignment( SwingConstants.CENTER );

			reposition();
			setFocusable( true );
			setEditable( true );
			setOpaque( true );
			setBorder( BorderFactory.createLineBorder( Color.ORANGE, 1 ) );
			setFont( FONT );
			selectAll();
			SwingUtilities.invokeLater( () -> requestFocusInWindow() );

			addActionListener( e -> {
				commit();
				kill();
			} );
			addKeyListener( new KeyListener()
			{
				@Override
				public void keyTyped( final KeyEvent e )
				{
					reposition();
				}

				@Override
				public void keyReleased( final KeyEvent e )
				{}

				@Override
				public void keyPressed( final KeyEvent e )
				{
					if ( e.getExtendedKeyCode() == KeyEvent.VK_ESCAPE )
						kill();
				}
			} );
			addFocusListener( new FocusListener()
			{
				@Override
				public void focusLost( final FocusEvent e )
				{
					kill();
				}

				@Override
				public void focusGained( final FocusEvent e )
				{}
			} );
		}

		/**
		 * Adapted from Jan Funke's code in
		 * https://github.com/saalfeldlab/bigcat/blob/janh5/src/main/java/bdv/bigcat/ui/BigCatTable.java#L112-L143
		 */
		private void blockKeys()
		{
			// Get all keystrokes that are mapped to actions in higher components
			final ArrayList< KeyStroke > allTableKeys = new ArrayList<>();
			for ( Container c = this; c != null; c = c.getParent() )
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
			getActionMap().put( "nothing", nada );

			// Replace every table key binding with nothing, thus creating an
			// event-barrier.
			final InputMap inputMap = getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
			for ( final KeyStroke key : allTableKeys )
				inputMap.put( key, "nothing" );
		}

		private void commit()
		{
			vertex.setLabel( getText().trim() );
			undoPointMarker.setUndoPoint();
			panel.entitiesAttributesChanged();
		}

		private void kill()
		{
			if ( killed )
				return;
			panel.getDisplay().remove( editor );
			panel.repaint();
			panel.getDisplay().requestFocusInWindow();
			panel.getGraph().releaseRef( vertex );
			killed = true;
		}

		private void reposition()
		{
			final int ly = vertex.getTimepoint();
			final double lx = vertex.getLayoutX();

			final double[] screenPos = new double[ 2 ];
			screenTransform.apply( new double[] { lx, ly }, screenPos );
			screenPos[ 0 ] += headerWidth;
			screenPos[ 1 ] += headerHeight;

			final int h = fontMetrics.getHeight() + 10;
			final int w = fontMetrics.stringWidth( getText() ) + 30;
			final int x = Math.min( panel.getDisplay().getWidth() - w, Math.max( 0, ( int ) screenPos[ 0 ] - w / 2 ) );
			final int y = Math.min( panel.getDisplay().getHeight() - h, Math.max( 0, ( int ) screenPos[ 1 ] - h / 2 ) );
			setBounds( x, y, w, h );
		}
	}
}
