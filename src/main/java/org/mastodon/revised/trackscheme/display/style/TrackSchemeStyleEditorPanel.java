package org.mastodon.revised.trackscheme.display.style;

import static org.mastodon.app.ui.settings.StyleElements.booleanElement;
import static org.mastodon.app.ui.settings.StyleElements.colorElement;
import static org.mastodon.app.ui.settings.StyleElements.linkedCheckBox;
import static org.mastodon.app.ui.settings.StyleElements.linkedColorButton;
import static org.mastodon.app.ui.settings.StyleElements.separator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

import org.mastodon.adapter.FocusModelAdapter;
import org.mastodon.adapter.HighlightModelAdapter;
import org.mastodon.adapter.RefBimap;
import org.mastodon.adapter.SelectionModelAdapter;
import org.mastodon.app.ui.settings.StyleElements.BooleanElement;
import org.mastodon.app.ui.settings.StyleElements.ColorElement;
import org.mastodon.app.ui.settings.StyleElements.Separator;
import org.mastodon.app.ui.settings.StyleElements.StyleElement;
import org.mastodon.app.ui.settings.StyleElements.StyleElementVisitor;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.model.DefaultFocusModel;
import org.mastodon.model.DefaultHighlightModel;
import org.mastodon.model.DefaultNavigationHandler;
import org.mastodon.model.DefaultTimepointModel;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.SelectionModel;
import org.mastodon.model.TimepointModel;
import org.mastodon.revised.trackscheme.TrackSchemeEdge;
import org.mastodon.revised.trackscheme.TrackSchemeEdgeBimap;
import org.mastodon.revised.trackscheme.TrackSchemeGraph;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.trackscheme.TrackSchemeVertexBimap;
import org.mastodon.revised.trackscheme.display.TrackSchemeOptions;
import org.mastodon.revised.trackscheme.display.TrackSchemePanel;
import org.mastodon.revised.trackscheme.display.style.dummygraph.DummyEdge;
import org.mastodon.revised.trackscheme.display.style.dummygraph.DummyGraph;
import org.mastodon.revised.trackscheme.display.style.dummygraph.DummyVertex;
import org.mastodon.revised.trackscheme.wrap.DefaultModelGraphProperties;
import org.mastodon.revised.trackscheme.wrap.ModelGraphProperties;

public class TrackSchemeStyleEditorPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final JColorChooser colorChooser;

	private final List< StyleElement > styleElements;

	public TrackSchemeStyleEditorPanel( final TrackSchemeStyle style )
	{
		super( new BorderLayout() );
		colorChooser = new JColorChooser();
		styleElements = styleElements( style );

		final DummyGraph.Examples ex = DummyGraph.Examples.CELEGANS;
		final DummyGraph example = ex.getGraph();
		final GraphIdBimap< DummyVertex, DummyEdge > idmap = example.getIdBimap();
		final ModelGraphProperties< DummyVertex, DummyEdge > dummyProps = new DefaultModelGraphProperties<>();
		final TrackSchemeGraph< DummyVertex, DummyEdge > graph = new TrackSchemeGraph<>( example, idmap, dummyProps );
		final RefBimap< DummyVertex, TrackSchemeVertex > vertexMap = new TrackSchemeVertexBimap<>( graph );
		final RefBimap< DummyEdge, TrackSchemeEdge > edgeMap = new TrackSchemeEdgeBimap<>( graph );
		final HighlightModel< TrackSchemeVertex, TrackSchemeEdge > highlight = new HighlightModelAdapter<>( new DefaultHighlightModel<>( idmap ), vertexMap, edgeMap );
		final FocusModel< TrackSchemeVertex > focus = new FocusModelAdapter<>( new DefaultFocusModel<>( idmap ), vertexMap );
		final TimepointModel timepoint = new DefaultTimepointModel();
		final SelectionModel< TrackSchemeVertex, TrackSchemeEdge > selection = new SelectionModelAdapter<>( ex.getSelectionModel(), vertexMap, edgeMap );
		final NavigationHandler< TrackSchemeVertex, TrackSchemeEdge > navigation = new DefaultNavigationHandler<>();
		final TrackSchemeOptions options = TrackSchemeOptions.options().style( style );
		final TrackSchemePanel previewPanel = new TrackSchemePanel( graph, highlight, focus, timepoint, selection, navigation, options );
		previewPanel.setTimepointRange( 0, 7 );
		timepoint.setTimepoint( 2 );
		previewPanel.graphChanged();
		previewPanel.getDisplay().setFocusable( false );
		previewPanel.setPreferredSize( new Dimension( 200, 200 ) );

		style.updateListeners().add( () -> {
			previewPanel.graphChanged();
			styleElements.forEach( StyleElement::update );
			repaint();
		} );

		final JPanel editPanel = new JPanel( new GridBagLayout() );
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 0, 5, 0, 5 );
		c.ipadx = 0;
		c.ipady = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;

		/*
		 * Fixed color setters.
		 */

		final int numCols = 2;

		c.gridwidth = 2;
		editPanel.add( Box.createVerticalStrut( 5 ), c );
		c.gridy++;
		c.gridwidth = 1;

		c.gridx = 0;

		styleElements.forEach( element -> element.accept(
				new StyleElementVisitor()
				{
					@Override
					public void visit( final Separator separator )
					{
						if ( c.gridx != 0 )
						{
							c.gridx = 0;
							++c.gridy;
						}
						c.gridwidth = 2;
						editPanel.add( Box.createVerticalStrut( 10 ), c );
						++c.gridy;
						c.gridwidth = 1;
					}

					@Override
					public void visit( final ColorElement element )
					{
						final JButton button = linkedColorButton( element, element.getLabel(), colorChooser );
						editPanel.add( button, c );
						if ( ++c.gridx == numCols )
						{
							c.gridx = 0;
							++c.gridy;
						}
					}

					@Override
					public void visit( final BooleanElement element )
					{
						final JCheckBox checkbox = linkedCheckBox( element, element.getLabel() );
						editPanel.add( checkbox, c );
						if ( ++c.gridx == numCols )
						{
							c.gridx = 0;
							++c.gridy;
						}
					}
				} ) );

		previewPanel.setBorder( new LineBorder( Color.LIGHT_GRAY, 1 ) );
		add( previewPanel, BorderLayout.CENTER );
		add( editPanel, BorderLayout.SOUTH );
	}

	private List< StyleElement > styleElements( final TrackSchemeStyle style )
	{
		return Arrays.asList(
				colorElement( "edge", style::getEdgeColor, style::edgeColor ),
				colorElement( "selected edge", style::getSelectedEdgeColor, style::selectedEdgeColor ),
				colorElement( "vertex fill", style::getVertexFillColor, style::vertexFillColor ),
				colorElement( "selected vertex fill", style::getSelectedVertexFillColor, style::selectedVertexFillColor ),
				colorElement( "vertex draw", style::getVertexDrawColor, style::vertexDrawColor ),
				colorElement( "selected vertex draw", style::getSelectedVertexDrawColor, style::selectedVertexDrawColor ),
				colorElement( "simplified vertex fill", style::getSimplifiedVertexFillColor, style::simplifiedVertexFillColor ),
				colorElement( "selected simplified vertex fill", style::getSelectedSimplifiedVertexFillColor, style::selectedSimplifiedVertexFillColor ),

				separator(),

				colorElement( "vertex range", style::getVertexRangeColor, style::vertexRangeColor ),

				separator(),

				colorElement( "background", style::getBackgroundColor, style::backgroundColor ),
				colorElement( "header background", style::getHeaderBackgroundColor, style::headerBackgroundColor ),
				colorElement( "decoration", style::getDecorationColor, style::decorationColor ),
				colorElement( "header decoration", style::getHeaderDecorationColor, style::headerDecorationColor ),
				colorElement( "current timepoint", style::getCurrentTimepointColor, style::currentTimepointColor ),
				colorElement( "header current timepoint", style::getHeaderCurrentTimepointColor, style::headerCurrentTimepointColor ),

				separator(),

				booleanElement( "paint rows", style::isPaintRows, style::paintRows ),
				booleanElement( "highlight current timepoint", style::isHighlightCurrentTimepoint, style::highlightCurrentTimepoint ),
				booleanElement( "paint columns", style::isPaintColumns, style::paintColumns ),
				booleanElement( "paint header shadow", style::isPaintHeaderShadow, style::paintHeaderShadow )
		);
	}

	public static void main( final String[] args )
	{
		final TrackSchemeStyle style = TrackSchemeStyle.defaultStyle();
		new TrackSchemeStyleEditorDialog( null, style ).setVisible( true );
	}

	public static class TrackSchemeStyleEditorDialog extends JDialog
	{
		private static final long serialVersionUID = 1L;

		private final TrackSchemeStyleEditorPanel stylePanel;

		public TrackSchemeStyleEditorDialog( final JDialog dialog, final TrackSchemeStyle style )
		{
			super( dialog, "TrackScheme style editor", false );

			stylePanel = new TrackSchemeStyleEditorPanel( style );

			getContentPane().add( stylePanel, BorderLayout.CENTER );

			final ActionMap am = getRootPane().getActionMap();
			final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
			final Object hideKey = new Object();
			final Action hideAction = new AbstractAction()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed( final ActionEvent e )
				{
					setVisible( false );
				}
			};
			im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
			am.put( hideKey, hideAction );

			pack();
			setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		}
	}
}
