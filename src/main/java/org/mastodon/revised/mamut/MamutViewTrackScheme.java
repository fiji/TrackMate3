package org.mastodon.revised.mamut;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;
import static org.mastodon.revised.mamut.MamutMenuBuilder.colorMenu;
import static org.mastodon.revised.mamut.MamutMenuBuilder.editMenu;
import static org.mastodon.revised.mamut.MamutMenuBuilder.viewMenu;

import javax.swing.ActionMap;

import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.model.AutoNavigateFocusModel;
import org.mastodon.revised.model.feature.Feature;
import org.mastodon.revised.model.feature.FeatureModel;
import org.mastodon.revised.model.feature.FeatureProjection;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraphTrackSchemeProperties;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.revised.model.tag.TagSetModel;
import org.mastodon.revised.trackscheme.TrackSchemeContextListener;
import org.mastodon.revised.trackscheme.TrackSchemeEdge;
import org.mastodon.revised.trackscheme.TrackSchemeGraph;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.trackscheme.display.EditFocusVertexLabelAction;
import org.mastodon.revised.trackscheme.display.ToggleLinkBehaviour;
import org.mastodon.revised.trackscheme.display.TrackSchemeFrame;
import org.mastodon.revised.trackscheme.display.TrackSchemeNavigationActions;
import org.mastodon.revised.trackscheme.display.TrackSchemeOptions;
import org.mastodon.revised.trackscheme.display.TrackSchemeZoom;
import org.mastodon.revised.trackscheme.display.style.TrackSchemeStyle;
import org.mastodon.revised.ui.EditTagActions;
import org.mastodon.revised.ui.FocusActions;
import org.mastodon.revised.ui.HighlightBehaviours;
import org.mastodon.revised.ui.SelectionActions;
import org.mastodon.revised.ui.coloring.ColorGenerator;
import org.mastodon.revised.ui.coloring.ColorMap;
import org.mastodon.revised.ui.coloring.ColoringMenu;
import org.mastodon.revised.ui.coloring.ColoringModel;
import org.mastodon.revised.ui.coloring.ComposedGraphColorGenerator;
import org.mastodon.revised.ui.coloring.DefaultColorGenerator;
import org.mastodon.revised.ui.coloring.FeatureColorGenerator;
import org.mastodon.revised.ui.coloring.FeatureColorGeneratorIncomingEdge;
import org.mastodon.revised.ui.coloring.FeatureColorGeneratorOutgoingEdge;
import org.mastodon.revised.ui.coloring.FeatureColorGeneratorSourceVertex;
import org.mastodon.revised.ui.coloring.FeatureColorGeneratorTargetVertex;
import org.mastodon.revised.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.revised.ui.coloring.TagSetGraphColorGenerator;
import org.mastodon.revised.ui.coloring.feature.FeatureColorMode;
import org.mastodon.revised.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.views.context.ContextChooser;
import org.scijava.ui.behaviour.KeyPressedManager;

public class MamutViewTrackScheme extends MamutView< TrackSchemeGraph< Spot, Link >, TrackSchemeVertex, TrackSchemeEdge >
{
	private final ContextChooser< Spot > contextChooser;

	public MamutViewTrackScheme( final MamutAppModel appModel )
	{
		super( appModel,
				new TrackSchemeGraph<>(
						appModel.getModel().getGraph(),
						appModel.getModel().getGraphIdBimap(),
						new ModelGraphTrackSchemeProperties( appModel.getModel().getGraph() ),
						appModel.getModel().getGraph().getLock() ),
				new String[] { KeyConfigContexts.TRACKSCHEME } );

		/*
		 * TrackScheme ContextChooser
		 */
		final TrackSchemeContextListener< Spot > contextListener = new TrackSchemeContextListener<>( viewGraph );
		contextChooser = new ContextChooser<>( contextListener );

		final KeyPressedManager keyPressedManager = appModel.getKeyPressedManager();
		final Model model = appModel.getModel();

		/*
		 * show TrackSchemeFrame
		 */
		final TrackSchemeStyle forwardDefaultStyle = appModel.getTrackSchemeStyleManager().getForwardDefaultStyle();
		final GraphColorGeneratorAdapter< Spot, Link, TrackSchemeVertex, TrackSchemeEdge > coloring = new GraphColorGeneratorAdapter<>( viewGraph.getVertexMap(), viewGraph.getEdgeMap() );
		final TrackSchemeOptions options = TrackSchemeOptions.options()
				.shareKeyPressedEvents( keyPressedManager )
				.style( forwardDefaultStyle )
				.graphColorGenerator( coloring );
		final AutoNavigateFocusModel< TrackSchemeVertex, TrackSchemeEdge > navigateFocusModel = new AutoNavigateFocusModel<>( focusModel, navigationHandler );
		final TrackSchemeFrame frame = new TrackSchemeFrame(
				viewGraph,
				highlightModel,
				navigateFocusModel,
				timepointModel,
				selectionModel,
				navigationHandler,
				model,
				groupHandle,
				contextChooser,
				options );
		frame.getTrackschemePanel().setTimepointRange( appModel.getMinTimepoint(), appModel.getMaxTimepoint() );
		frame.getTrackschemePanel().graphChanged();
		contextListener.setContextListener( frame.getTrackschemePanel() );

		final TrackSchemeStyle.UpdateListener updateListener = () -> frame.getTrackschemePanel().repaint();
		forwardDefaultStyle.updateListeners().add( updateListener );
		onClose( () -> forwardDefaultStyle.updateListeners().remove( updateListener ) );

		setFrame( frame );
		frame.setVisible( true );

		MastodonFrameViewActions.install( viewActions, this );
		HighlightBehaviours.install( viewBehaviours, viewGraph, viewGraph.getLock(), viewGraph, highlightModel, model );
		ToggleLinkBehaviour.install( viewBehaviours, frame.getTrackschemePanel(),	viewGraph, viewGraph.getLock(),	viewGraph, model );
		EditFocusVertexLabelAction.install( viewActions, frame.getTrackschemePanel(), focusModel, model );
		FocusActions.install( viewActions, viewGraph, viewGraph.getLock(), navigateFocusModel, selectionModel );
		TrackSchemeZoom.install( viewBehaviours, frame.getTrackschemePanel() );
		EditTagActions.install( viewActions, frame.getKeybindings(), frame.getTriggerbindings(), model.getTagSetModel(), appModel.getSelectionModel(), frame.getTrackschemePanel(), frame.getTrackschemePanel().getDisplay(), model );
		viewActions.runnableAction( () -> System.out.println( model.getTagSetModel() ), "output tags", "U" ); // DEBUG TODO: REMOVE

		// TODO Let the user choose between the two selection/focus modes.
		frame.getTrackschemePanel().getNavigationActions().install( viewActions, TrackSchemeNavigationActions.NavigatorEtiquette.FINDER_LIKE );
		frame.getTrackschemePanel().getNavigationBehaviours().install( viewBehaviours );
		frame.getTrackschemePanel().getTransformEventHandler().install( viewBehaviours );

		final ViewMenu menu = new ViewMenu( this );
		final ActionMap actionMap = frame.getKeybindings().getConcatenatedActionMap();

		final JMenuHandle tagSetColoringMenuHandle = new JMenuHandle();

		MainWindow.addMenus( menu, actionMap );
		MamutMenuBuilder.build( menu, actionMap,
				viewMenu(
						colorMenu( tagSetColoringMenuHandle ),
						separator(),
						item( MastodonFrameViewActions.TOGGLE_SETTINGS_PANEL )
				),
				editMenu(
						item( UndoActions.UNDO ),
						item( UndoActions.REDO ),
						separator(),
						item( SelectionActions.DELETE_SELECTION ),
						item( SelectionActions.SELECT_WHOLE_TRACK ),
						item( SelectionActions.SELECT_TRACK_DOWNWARD ),
						item( SelectionActions.SELECT_TRACK_UPWARD ),
						separator(),
						item( TrackSchemeNavigationActions.SELECT_NAVIGATE_CHILD ),
						item( TrackSchemeNavigationActions.SELECT_NAVIGATE_PARENT ),
						item( TrackSchemeNavigationActions.SELECT_NAVIGATE_LEFT ),
						item( TrackSchemeNavigationActions.SELECT_NAVIGATE_RIGHT ),
						separator(),
						item( TrackSchemeNavigationActions.NAVIGATE_CHILD ),
						item( TrackSchemeNavigationActions.NAVIGATE_PARENT ),
						item( TrackSchemeNavigationActions.NAVIGATE_LEFT ),
						item( TrackSchemeNavigationActions.NAVIGATE_RIGHT ),
						separator(),
						item( EditFocusVertexLabelAction.EDIT_FOCUS_LABEL )
				)
		);
		appModel.getPlugins().addMenus( menu );

		final TagSetModel< Spot, Link > tagSetModel = appModel.getModel().getTagSetModel();
		final FeatureColorModeManager featureColorModeManager = appModel.getFeatureColorModeManager();
		final ColoringModel coloringModel = new ColoringModel( tagSetModel, featureColorModeManager );

		tagSetModel.listeners().add( coloringModel );
		onClose( () -> tagSetModel.listeners().remove( coloringModel ) );

		featureColorModeManager.getForwardDefaultMode().updateListeners().add( coloringModel );
		onClose( () -> featureColorModeManager.getForwardDefaultMode().updateListeners().remove( coloringModel ) );

		final ColoringMenu coloringMenu = new ColoringMenu( tagSetColoringMenuHandle.getMenu(), coloringModel,
				appModel.getModel().getFeatureModel(), Spot.class, Link.class );

		tagSetModel.listeners().add( coloringMenu );
		onClose( () -> tagSetModel.listeners().remove( coloringMenu ) );

		featureColorModeManager.getForwardDefaultMode().updateListeners().add( coloringMenu );
		onClose( () -> featureColorModeManager.getForwardDefaultMode().updateListeners().remove( coloringMenu ) );

		@SuppressWarnings( "unchecked" )
		final ColoringModel.ColoringChangedListener coloringChangedListener = () ->
		{
			if ( coloringModel.noColoring() )
				coloring.setColorGenerator( null );
			else if ( coloringModel.getTagSet() != null)
				coloring.setColorGenerator( new TagSetGraphColorGenerator<>( tagSetModel, coloringModel.getTagSet() ) );
			else if ( coloringModel.getFeatureColorMode() != null )
			{
				final FeatureModel featureModel = appModel.getModel().getFeatureModel();
				final FeatureColorMode fcm = coloringModel.getFeatureColorMode();

				// Vertex.
				final ColorGenerator< Spot > vertexColorGenerator;
				final String[] vertexKeys = fcm.getVertexFeatureProjection();
				final Feature< ?, ? > vertexFeature = featureModel.getFeature( vertexKeys[ 0 ] );
				if ( null == vertexFeature || null == vertexFeature.getProjections().get( vertexKeys[ 1 ] ) )
					vertexColorGenerator = new DefaultColorGenerator< Spot >();
				else
				{
					final FeatureProjection< ? > vertexProjection = vertexFeature.getProjections().get( vertexKeys[ 1 ] );
					final String vertexColorMap = fcm.getVertexColorMap();
					final double vertexRangeMin = fcm.getVertexRangeMin();
					final double vertexRangeMax = fcm.getVertexRangeMax();
					switch ( fcm.getVertexColorMode() )
					{
					case INCOMING_EDGE:
						vertexColorGenerator = new FeatureColorGeneratorIncomingEdge< Spot, Link >(
								( FeatureProjection< Link > ) vertexProjection,
								ColorMap.getColorMap( vertexColorMap ),
								vertexRangeMin, vertexRangeMax,
								appModel.getModel().getGraph().edgeRef() );
						break;
					case OUTGOING_EDGE:
						vertexColorGenerator = new FeatureColorGeneratorOutgoingEdge< Spot, Link >(
								( FeatureProjection< Link > ) vertexProjection,
								ColorMap.getColorMap( vertexColorMap ),
								vertexRangeMin, vertexRangeMax,
								appModel.getModel().getGraph().edgeRef() );
						break;
					case VERTEX:
						vertexColorGenerator = new FeatureColorGenerator< Spot >(
								( FeatureProjection< Spot > ) vertexProjection,
								ColorMap.getColorMap( vertexColorMap ),
								vertexRangeMin, vertexRangeMax );
						break;
					case NONE:
					default:
						vertexColorGenerator = new DefaultColorGenerator<>();
						break;
					}
				}

				// Edge.
				final ColorGenerator< Link > edgeColorGenerator;
				final String[] edgeKeys = fcm.getEdgeFeatureProjection();
				final Feature< ?, ? > edgeFeature = featureModel.getFeature( edgeKeys[ 0 ] );
				if ( null == edgeFeature || null == edgeFeature.getProjections().get( edgeKeys[ 1 ] ) )
					edgeColorGenerator = new DefaultColorGenerator< Link >();
				else
				{
					final FeatureProjection< ? > edgeProjection = edgeFeature.getProjections().get( edgeKeys[ 1 ] );
					final String edgeColorMap = fcm.getEdgeColorMap();
					final double edgeRangeMin = fcm.getEdgeRangeMin();
					final double edgeRangeMax = fcm.getEdgeRangeMax();
					switch ( fcm.getEdgeColorMode() )
					{
					case SOURCE_VERTEX:
						edgeColorGenerator = new FeatureColorGeneratorSourceVertex< Spot, Link >(
								( FeatureProjection< Spot > ) edgeProjection,
								ColorMap.getColorMap( edgeColorMap ),
								edgeRangeMin, edgeRangeMax,
								appModel.getModel().getGraph().vertexRef() );
						break;
					case TARGET_VERTEX:
						edgeColorGenerator = new FeatureColorGeneratorTargetVertex< Spot, Link >(
								( FeatureProjection< Spot > ) edgeProjection,
								ColorMap.getColorMap( edgeColorMap ),
								edgeRangeMin, edgeRangeMax,
								appModel.getModel().getGraph().vertexRef() );
						break;
					case EDGE:
						edgeColorGenerator = new FeatureColorGenerator< Link >(
								( FeatureProjection< Link > ) edgeProjection,
								ColorMap.getColorMap( edgeColorMap ),
								edgeRangeMin, edgeRangeMax );
						break;
					case NONE:
					default:
						edgeColorGenerator = new DefaultColorGenerator<>();
						break;
					}
				}

				coloring.setColorGenerator( new ComposedGraphColorGenerator<>( vertexColorGenerator, edgeColorGenerator ) );
			}
			frame.getTrackschemePanel().entitiesAttributesChanged();
		};
		coloringModel.listeners().add( coloringChangedListener );

		frame.getTrackschemePanel().repaint();
	}

	public ContextChooser< Spot > getContextChooser()
	{
		return contextChooser;
	}
}
