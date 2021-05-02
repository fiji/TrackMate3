package org.mastodon.mamut;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;

import java.util.function.Function;

import javax.swing.ActionMap;

import org.mastodon.app.IdentityViewGraph;
import org.mastodon.app.ViewGraph;
import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.ViewFrame;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.feature.FeatureModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.ui.SelectionActions;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.table.TableViewActions;
import org.mastodon.views.table.TableViewFrame;

public class MamutViewTable extends MamutView< ViewGraph< Spot, Link, Spot, Link >, Spot, Link >
{

	private static final String[] CONTEXTS = new String[] { KeyConfigContexts.TABLE };

	public MamutViewTable( final MamutAppModel appModel )

	{
		super( appModel, IdentityViewGraph.wrap( appModel.getModel().getGraph(), appModel.getModel().getGraphIdBimap() ), CONTEXTS );

		final GraphColorGeneratorAdapter< Spot, Link, Spot, Link > coloring = new GraphColorGeneratorAdapter<>( viewGraph.getVertexMap(), viewGraph.getEdgeMap() );

		final TableViewFrame< MamutAppModel, ViewGraph< Spot, Link, Spot, Link >, Spot, Link > frame = new TableViewFrame<>(
				appModel,
				viewGraph,
				appModel.getModel().getFeatureModel(),
				appModel.getModel().getTagSetModel(),
				( v ) -> v.getLabel(),
				new Function< Link, String >()
				{

					private final Spot ref = appModel.getModel().getGraph().vertexRef();

					@Override
					public String apply( final Link t )
					{
						return t.getSource( ref ).getLabel() + " \u2192 " + t.getTarget( ref ).getLabel();
					}
				},
				( v, lbl ) -> v.setLabel( lbl ),
				null,
				groupHandle,
				navigationHandler,
				appModel.getModel(),
				coloring );
		setFrame( frame );

		final Model model = appModel.getModel();
		final FeatureModel featureModel = model.getFeatureModel();
		final TagSetModel< Spot, Link > tagSetModel = model.getTagSetModel();

		focusModel.listeners().add( frame );
		highlightModel.listeners().add( frame );
		featureModel.listeners().add( frame );
		tagSetModel.listeners().add( frame );

		MastodonFrameViewActions.install( viewActions, this );
		TableViewActions.install( viewActions, frame );

		onClose( () -> {
			focusModel.listeners().remove( frame );
			highlightModel.listeners().remove( frame );
			featureModel.listeners().remove( frame );
			tagSetModel.listeners().remove( frame );
		} );

		final ViewMenu menu = new ViewMenu( this );
		final ActionMap actionMap = frame.getKeybindings().getConcatenatedActionMap();

		final JMenuHandle menuHandle = new JMenuHandle();

		MamutMenuBuilder.build( menu, actionMap,
				MamutMenuBuilder.fileMenu(
						item( TableViewActions.EXPORT_TO_CSV ),
						separator() ) );
		MainWindow.addMenus( menu, actionMap );
		MamutMenuBuilder.build( menu, actionMap,
				MamutMenuBuilder.viewMenu(
						MamutMenuBuilder.colorMenu( menuHandle ),
						separator(),
						item( MastodonFrameViewActions.TOGGLE_SETTINGS_PANEL )
						),
				MamutMenuBuilder.editMenu(
						item( TableViewActions.EDIT_LABEL ),
						item( TableViewActions.TOGGLE_TAG ),
						separator(),
						item( SelectionActions.DELETE_SELECTION ) ) );
		appModel.getPlugins().addMenus( menu );

		registerColoring( coloring, menuHandle, () -> {
			frame.getEdgeTable().repaint();
			frame.getVertexTable().repaint();
		} );

		frame.setSize( 400, 400 );
		frame.setVisible( true );
	}

	@Override
	public TableViewFrame< MamutAppModel, ViewGraph< Spot, Link, Spot, Link >, Spot, Link > getFrame()
	{
		final ViewFrame f = super.getFrame();
		@SuppressWarnings( "unchecked" )
		final TableViewFrame< MamutAppModel, ViewGraph< Spot, Link, Spot, Link >, Spot, Link > vf = ( TableViewFrame< MamutAppModel, ViewGraph< Spot, Link, Spot, Link >, Spot, Link > ) f;
		return vf;
	}

	public ContextChooser< Spot > getContextChooser()
	{
		return getFrame().getContextChooser();
	}
}
