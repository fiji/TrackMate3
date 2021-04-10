package org.mastodon.mamut;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.separator;
import static org.mastodon.mamut.MamutMenuBuilder.colorMenu;
import static org.mastodon.mamut.MamutMenuBuilder.editMenu;
import static org.mastodon.mamut.MamutMenuBuilder.fileMenu;
import static org.mastodon.mamut.MamutMenuBuilder.tagSetMenu;
import static org.mastodon.mamut.MamutMenuBuilder.viewMenu;

import javax.swing.ActionMap;
import javax.swing.JPanel;

import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.SearchVertexLabel;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.ModelOverlayProperties;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.AutoNavigateFocusModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.ui.FocusActions;
import org.mastodon.ui.HighlightBehaviours;
import org.mastodon.ui.SelectionActions;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.views.bdv.BdvContextProvider;
import org.mastodon.views.bdv.BigDataViewerActionsMamut;
import org.mastodon.views.bdv.BigDataViewerMamut;
import org.mastodon.views.bdv.NavigationActionsMamut;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.mastodon.views.bdv.ViewerFrameMamut;
import org.mastodon.views.bdv.ViewerPanelMamut;
import org.mastodon.views.bdv.overlay.BdvHighlightHandler;
import org.mastodon.views.bdv.overlay.BdvSelectionBehaviours;
import org.mastodon.views.bdv.overlay.EditBehaviours;
import org.mastodon.views.bdv.overlay.EditSpecialBehaviours;
import org.mastodon.views.bdv.overlay.OverlayGraphRenderer;
import org.mastodon.views.bdv.overlay.OverlayNavigation;
import org.mastodon.views.bdv.overlay.RenderSettings;
import org.mastodon.views.bdv.overlay.RenderSettings.UpdateListener;
import org.mastodon.views.bdv.overlay.wrap.OverlayEdgeWrapper;
import org.mastodon.views.bdv.overlay.wrap.OverlayGraphWrapper;
import org.mastodon.views.bdv.overlay.wrap.OverlayVertexWrapper;
import org.mastodon.views.context.ContextProvider;

import bdv.tools.InitializeViewerState;

public class MamutViewBdv extends MamutView< OverlayGraphWrapper< Spot, Link >, OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > >
{
	// TODO
	private static int bdvName = 1;

	private final SharedBigDataViewerData sharedBdvData;

	private final BdvContextProvider< Spot, Link > contextProvider;

	private final ViewerPanelMamut viewer;

	public MamutViewBdv( final MamutAppModel appModel )
	{
		super( appModel,
				new OverlayGraphWrapper<>(
						appModel.getModel().getGraph(),
						appModel.getModel().getGraphIdBimap(),
						appModel.getModel().getSpatioTemporalIndex(),
						appModel.getModel().getGraph().getLock(),
						new ModelOverlayProperties( appModel.getModel().getGraph(), appModel.getRadiusStats() ) ),
				new String[] { KeyConfigContexts.BIGDATAVIEWER } );

		sharedBdvData = appModel.getSharedBdvData();

		final String windowTitle = "BigDataViewer " + ( bdvName++ ); // TODO: use JY naming scheme
		final BigDataViewerMamut bdv = new BigDataViewerMamut( sharedBdvData, windowTitle, groupHandle );
		final ViewerFrameMamut frame = bdv.getViewerFrame();
		setFrame( frame );

		MastodonFrameViewActions.install( viewActions, this );
		BigDataViewerActionsMamut.install( viewActions, bdv );

		final ViewMenu menu = new ViewMenu( this );
		final ActionMap actionMap = frame.getKeybindings().getConcatenatedActionMap();

		final JMenuHandle menuHandle = new JMenuHandle();
		final JMenuHandle tagSetMenuHandle = new JMenuHandle();
		MainWindow.addMenus( menu, actionMap );
		MamutMenuBuilder.build( menu, actionMap,
				fileMenu(
						separator(),
						item( BigDataViewerActionsMamut.LOAD_SETTINGS ),
						item( BigDataViewerActionsMamut.SAVE_SETTINGS )
				),
				viewMenu(
						colorMenu( menuHandle ),
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
						tagSetMenu( tagSetMenuHandle )
				),
				ViewMenuBuilder.menu( "Settings",
						item( BigDataViewerActionsMamut.BRIGHTNESS_SETTINGS ),
						item( BigDataViewerActionsMamut.VISIBILITY_AND_GROUPING )
				)
		);
		appModel.getPlugins().addMenus( menu );

		frame.setVisible( true );

		viewer = bdv.getViewer();
		InitializeViewerState.initTransform( viewer );

		final GraphColorGeneratorAdapter< Spot, Link, OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > coloring =
				new GraphColorGeneratorAdapter<>( viewGraph.getVertexMap(), viewGraph.getEdgeMap() );

		viewer.setTimepoint( timepointModel.getTimepoint() );
		final OverlayGraphRenderer< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > tracksOverlay = new OverlayGraphRenderer<>(
				viewGraph,
				highlightModel,
				focusModel,
				selectionModel,
				coloring );
		viewer.getDisplay().addOverlayRenderer( tracksOverlay );
		viewer.addRenderTransformListener( tracksOverlay );
		viewer.addTimePointListener( tracksOverlay );

		final Model model = appModel.getModel();
		final ModelGraph modelGraph = model.getGraph();

		registerColoring( coloring, menuHandle, () -> viewer.getDisplay().repaint() );
		registerTagSetMenu( tagSetMenuHandle, () -> viewer.getDisplay().repaint() );

		highlightModel.listeners().add( () -> viewer.getDisplay().repaint() );
		focusModel.listeners().add( () -> viewer.getDisplay().repaint() );
		modelGraph.addGraphChangeListener( () -> viewer.getDisplay().repaint() );
		modelGraph.addVertexPositionListener( v -> viewer.getDisplay().repaint() );
		modelGraph.addVertexLabelListener( v -> viewer.getDisplay().repaint() );
		selectionModel.listeners().add( () -> viewer.getDisplay().repaint() );

		final OverlayNavigation< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > overlayNavigation = new OverlayNavigation<>( viewer, viewGraph );
		navigationHandler.listeners().add( overlayNavigation );

		final BdvHighlightHandler< ?, ? > highlightHandler = new BdvHighlightHandler<>( viewGraph, tracksOverlay, highlightModel );
		viewer.getDisplay().addHandler( highlightHandler );
		viewer.addRenderTransformListener( highlightHandler );

		contextProvider = new BdvContextProvider<>( windowTitle, viewGraph, tracksOverlay );
		viewer.addRenderTransformListener( contextProvider );

		final AutoNavigateFocusModel< OverlayVertexWrapper< Spot, Link >, OverlayEdgeWrapper< Spot, Link > > navigateFocusModel = new AutoNavigateFocusModel<>( focusModel, navigationHandler );

		BdvSelectionBehaviours.install( viewBehaviours, viewGraph, tracksOverlay, selectionModel, focusModel, navigationHandler );
		EditBehaviours.install( viewBehaviours, viewGraph, tracksOverlay, selectionModel, focusModel, model );
		EditSpecialBehaviours.install( viewBehaviours, frame.getViewerPanel(), viewGraph, tracksOverlay, selectionModel, focusModel, model );
		HighlightBehaviours.install( viewBehaviours, viewGraph, viewGraph.getLock(), viewGraph, highlightModel, model );
		FocusActions.install( viewActions, viewGraph, viewGraph.getLock(), navigateFocusModel, selectionModel );

		/*
		 * We must make a search action using the underlying model graph,
		 * because we cannot iterate over the OverlayGraphWrapper properly
		 * (vertices are object vertices that wrap a pool vertex...)
		 */
		final NavigationHandler< Spot, Link > navigationHandlerAdapter = groupHandle.getModel( appModel.NAVIGATION );
		final JPanel searchField = SearchVertexLabel.install(
				viewActions,
				appModel.getModel().getGraph(),
				navigationHandlerAdapter,
				appModel.getSelectionModel(),
				appModel.getFocusModel(),
				viewer );
		frame.getSettingsPanel().add( searchField );

		NavigationActionsMamut.install( viewActions, viewer, sharedBdvData.is2D() );
		viewer.getTransformEventHandler().install( viewBehaviours );

		viewer.addTimePointListener( timePointIndex -> timepointModel.setTimepoint( timePointIndex ) );
		timepointModel.listeners().add( () -> viewer.setTimepoint( timepointModel.getTimepoint() ) );

		final RenderSettings renderSettings = appModel.getRenderSettingsManager().getForwardDefaultStyle();
		tracksOverlay.setRenderSettings( renderSettings );
		final UpdateListener updateListener = () -> {
			viewer.repaint();
			contextProvider.notifyContextChanged();
		};
		renderSettings.updateListeners().add( updateListener );
		onClose( () -> renderSettings.updateListeners().remove( updateListener ) );

		// Give focus to display so that it can receive key-presses immediately.
		viewer.getDisplay().requestFocusInWindow();

//		if ( !bdv.tryLoadSettings( bdvFile ) ) // TODO
//			InitializeViewerState.initBrightness( 0.001, 0.999, bdv.getViewer(), bdv.getSetupAssignments() );
	}

	public ContextProvider< Spot > getContextProvider()
	{
		return contextProvider;
	}

	public ViewerPanelMamut getViewerPanelMamut()
	{
		return viewer;
	}

	public void requestRepaint()
	{
		viewer.requestRepaint();
	}
}
