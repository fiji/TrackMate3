package org.mastodon.mamut;

import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.mastodon.feature.FeatureSpecsService;
import org.mastodon.feature.ui.FeatureColorModeConfigPage;
import org.mastodon.graph.GraphChangeListener;
import org.mastodon.mamut.feature.MamutFeatureProjectionsManager;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.SpotPool;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.mamut.plugin.MamutPlugins;
import org.mastodon.model.SelectionListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.model.tag.ui.TagSetDialog;
import org.mastodon.ui.SelectionActions;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.CommandDescriptionsBuilder;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.ui.keymap.Keymap;
import org.mastodon.ui.keymap.KeymapManager;
import org.mastodon.ui.keymap.KeymapSettingsPage;
import org.mastodon.util.ToggleDialogAction;
import org.mastodon.views.bdv.overlay.ui.RenderSettingsConfigPage;
import org.mastodon.views.bdv.overlay.ui.RenderSettingsManager;
import org.mastodon.views.context.ContextProvider;
import org.mastodon.views.table.FeatureTagTablePanel;
import org.mastodon.views.trackscheme.display.style.TrackSchemeStyleManager;
import org.mastodon.views.trackscheme.display.style.TrackSchemeStyleSettingsPage;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.util.InvokeOnEDT;

public class WindowManager
{
	public static final String NEW_BDV_VIEW = "new bdv view";
	public static final String NEW_TRACKSCHEME_VIEW = "new trackscheme view";
	public static final String NEW_TABLE_VIEW = "new full table view";
	public static final String NEW_SELECTION_TABLE_VIEW = "new selection table view";
	public static final String PREFERENCES_DIALOG = "Preferences";
	public static final String TAGSETS_DIALOG = "edit tag sets";
	public static final String COMPUTE_FEATURE_DIALOG = "compute features";

	static final String[] NEW_BDV_VIEW_KEYS = new String[] { "not mapped" };
	static final String[] NEW_TRACKSCHEME_VIEW_KEYS = new String[] { "not mapped" };
	static final String[] NEW_TABLE_VIEW_KEYS = new String[] { "not mapped" };
	static final String[] NEW_SELECTION_TABLE_VIEW_KEYS = new String[] { "not mapped" };
	static final String[] PREFERENCES_DIALOG_KEYS = new String[] { "meta COMMA", "ctrl COMMA" };
	static final String[] TAGSETS_DIALOG_KEYS = new String[] { "not mapped" };
	static final String[] COMPUTE_FEATURE_DIALOG_KEYS = new String[] { "not mapped" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( NEW_BDV_VIEW, NEW_BDV_VIEW_KEYS, "Open a new BigDataViewer view." );
			descriptions.add( NEW_TRACKSCHEME_VIEW, NEW_TRACKSCHEME_VIEW_KEYS, "Open a new TrackScheme view." );
			descriptions.add( NEW_TABLE_VIEW, NEW_TABLE_VIEW_KEYS, "Open a new table view. "
					+ "The table displays the full data." );
			descriptions.add( NEW_SELECTION_TABLE_VIEW, NEW_SELECTION_TABLE_VIEW_KEYS,
					"Open a new selection table view. "
							+ "The table only displays the current selection and "
							+ "is updated as the selection changes." );
			descriptions.add( PREFERENCES_DIALOG, PREFERENCES_DIALOG_KEYS, "Edit Mastodon preferences." );
			descriptions.add( TAGSETS_DIALOG, TAGSETS_DIALOG_KEYS, "Edit tag definitions." );
		}
	}

	private final Context context;

	private final MamutPlugins plugins;

	/**
	 * All currently open BigDataViewer windows.
	 */
	private final List< MamutViewBdv > bdvWindows = new ArrayList<>();

	/**
	 * The {@link ContextProvider}s of all currently open BigDataViewer windows.
	 */
	private final List< ContextProvider< Spot > > contextProviders = new ArrayList<>();

	/**
	 * All currently open TrackScheme windows.
	 */
	private final List< MamutViewTrackScheme > tsWindows = new ArrayList<>();

	/**
	 * All currently open Table windows.
	 */
	private final List< MamutViewTable > tableWindows = new ArrayList<>();

	private final KeyPressedManager keyPressedManager;

	private final TrackSchemeStyleManager trackSchemeStyleManager;

	private final RenderSettingsManager renderSettingsManager;

	private final FeatureColorModeManager featureColorModeManager;

	private final MamutFeatureProjectionsManager featureProjectionsManager;

	private final KeymapManager keymapManager;

	private final Actions globalAppActions;

	private final AbstractNamedAction newBdvViewAction;

	private final AbstractNamedAction newTrackSchemeViewAction;

	private final AbstractNamedAction newTableViewAction;

	private final AbstractNamedAction newSelectionTableViewAction;

	private final AbstractNamedAction editTagSetsAction;

	private final AbstractNamedAction featureComputationAction;

	private MamutAppModel appModel;

	private TagSetDialog tagSetDialog;

	private JDialog featureComputationDialog;

	final ProjectManager projectManager;

	public WindowManager( final Context context )
	{
		this.context = context;

		keyPressedManager = new KeyPressedManager();
		trackSchemeStyleManager = new TrackSchemeStyleManager();
		renderSettingsManager = new RenderSettingsManager();
		featureColorModeManager = new FeatureColorModeManager();
		featureProjectionsManager = new MamutFeatureProjectionsManager(
				context.getService( FeatureSpecsService.class ),
				featureColorModeManager );
		keymapManager = new KeymapManager();

		final Keymap keymap = keymapManager.getForwardDefaultKeymap();

		plugins = new MamutPlugins( keymap );
		discoverPlugins();

		final CommandDescriptions descriptions = buildCommandDescriptions();
		final Consumer< Keymap > augmentInputTriggerConfig = k -> descriptions.augmentInputTriggerConfig( k.getConfig() );
		keymapManager.getUserStyles().forEach( augmentInputTriggerConfig );
		keymapManager.getBuiltinStyles().forEach( augmentInputTriggerConfig );

		// TODO: naming, this should be named appActions and the AppModel.appActions should become modelActions?
		// TODO: or rename AppModel --> ProjectModel, then projectActions?
		globalAppActions = new Actions( keymap.getConfig(), KeyConfigContexts.MASTODON );
		keymap.updateListeners().add( () -> {
			globalAppActions.updateKeyConfig( keymap.getConfig() );
			if ( appModel != null )
				appModel.getAppActions().updateKeyConfig( keymap.getConfig() );
		} );

		projectManager = new ProjectManager( this );
		projectManager.install( globalAppActions );

		newBdvViewAction = new RunnableAction( NEW_BDV_VIEW, this::createBigDataViewer );
		newTrackSchemeViewAction = new RunnableAction( NEW_TRACKSCHEME_VIEW, this::createTrackScheme );
		newTableViewAction = new RunnableAction( NEW_TABLE_VIEW, () -> createTable( false ) );
		newSelectionTableViewAction = new RunnableAction( NEW_SELECTION_TABLE_VIEW, () -> createTable( true ) );
		editTagSetsAction = new RunnableAction( TAGSETS_DIALOG, this::editTagSets );
		featureComputationAction = new RunnableAction( COMPUTE_FEATURE_DIALOG, this::computeFeatures );

		globalAppActions.namedAction( newBdvViewAction, NEW_BDV_VIEW_KEYS );
		globalAppActions.namedAction( newTrackSchemeViewAction, NEW_TRACKSCHEME_VIEW_KEYS );
		globalAppActions.namedAction( newTableViewAction, NEW_SELECTION_TABLE_VIEW_KEYS );
		globalAppActions.namedAction( newSelectionTableViewAction, NEW_SELECTION_TABLE_VIEW_KEYS );
		globalAppActions.namedAction( editTagSetsAction, TAGSETS_DIALOG_KEYS );
		globalAppActions.namedAction( featureComputationAction, COMPUTE_FEATURE_DIALOG_KEYS );

		final PreferencesDialog settings = new PreferencesDialog( null, keymap, new String[] { KeyConfigContexts.MASTODON } );
		settings.addPage( new TrackSchemeStyleSettingsPage( "TrackScheme Styles", trackSchemeStyleManager ) );
		settings.addPage( new RenderSettingsConfigPage( "BDV Render Settings", renderSettingsManager ) );
		settings.addPage( new KeymapSettingsPage( "Keymap", keymapManager, descriptions ) );
		settings.addPage( new FeatureColorModeConfigPage( "Feature Color Modes", featureColorModeManager, featureProjectionsManager ) );

		final ToggleDialogAction tooglePreferencesDialogAction = new ToggleDialogAction( PREFERENCES_DIALOG, settings );
		globalAppActions.namedAction( tooglePreferencesDialogAction, PREFERENCES_DIALOG_KEYS );

		updateEnabledActions();
	}

	private void discoverPlugins()
	{
		if ( context == null )
			return;

		final PluginService pluginService = context.getService( PluginService.class );
		final List< PluginInfo< MamutPlugin > > infos = pluginService.getPluginsOfType( MamutPlugin.class );
		for ( final PluginInfo< MamutPlugin > info : infos )
		{
			try
			{
				final MamutPlugin plugin = info.createInstance();
				context.inject( plugin );
				plugins.register( plugin );
			}
			catch ( final InstantiableException e )
			{
				e.printStackTrace();
			}
		}
	}

	private void updateEnabledActions()
	{
		newBdvViewAction.setEnabled( appModel != null );
		newTrackSchemeViewAction.setEnabled( appModel != null );
		newTableViewAction.setEnabled( appModel != null );
		newSelectionTableViewAction.setEnabled( appModel != null );
		editTagSetsAction.setEnabled( appModel != null );
		featureComputationAction.setEnabled( appModel != null );
	}

	void setAppModel( final MamutAppModel appModel )
	{
		closeAllWindows();

		this.appModel = appModel;
		if ( appModel == null )
		{
			tagSetDialog.dispose();
			tagSetDialog = null;
			featureComputationDialog.dispose();
			featureComputationDialog = null;
			featureProjectionsManager.setModel( null, 1 );
			updateEnabledActions();
			return;
		}

		final Model model = appModel.getModel();
		UndoActions.install( appModel.getAppActions(), model );
		SelectionActions.install( appModel.getAppActions(), model.getGraph(), model.getGraph().getLock(), model.getGraph(), appModel.getSelectionModel(), model );

		final Keymap keymap = keymapManager.getForwardDefaultKeymap();
		tagSetDialog = new TagSetDialog( null, model.getTagSetModel(), model, keymap, new String[] { KeyConfigContexts.MASTODON } );
		featureComputationDialog = MamutFeatureComputation.getDialog( appModel, context );
		featureProjectionsManager.setModel( model, appModel.getSharedBdvData().getSources().size() );

		updateEnabledActions();

		plugins.setAppPluginModel( new MamutPluginAppModel( appModel, this ) );
	}

	private synchronized void addBdvWindow( final MamutViewBdv w )
	{
		bdvWindows.add( w );
		contextProviders.add( w.getContextProvider() );
		for ( final MamutViewTrackScheme tsw : tsWindows )
			tsw.getContextChooser().updateContextProviders( contextProviders );
		for ( final MamutViewTable tw : tableWindows )
			tw.getContextChooser().updateContextProviders( contextProviders );
		w.onClose( () -> {
			bdvWindows.remove( w );
			contextProviders.remove( w.getContextProvider() );
			for ( final MamutViewTrackScheme tsw : tsWindows )
				tsw.getContextChooser().updateContextProviders( contextProviders );
			for ( final MamutViewTable tw : tableWindows )
				tw.getContextChooser().updateContextProviders( contextProviders );
		} );
	}

	public void forEachBdvView( final Consumer< ? super MamutViewBdv > action )
	{
		bdvWindows.forEach( action );
	}

	private synchronized void addTsWindow( final MamutViewTrackScheme w )
	{
		tsWindows.add( w );
		w.getContextChooser().updateContextProviders( contextProviders );
		w.onClose( () -> {
			tsWindows.remove( w );
			w.getContextChooser().updateContextProviders( new ArrayList<>() );
		} );
	}

	private synchronized void addTableWindow( final MamutViewTable table )
	{
		tableWindows.add( table );
		table.getContextChooser().updateContextProviders( contextProviders );
		table.onClose( () -> {
			tableWindows.remove( table );
			table.getContextChooser().updateContextProviders( new ArrayList<>() );
		} );
	}

	public void forEachTableView( final Consumer< ? super MamutViewTable > action )
	{
		tableWindows.forEach( action );
	}

	public void forEachTrackSchemeView( final Consumer< ? super MamutViewTrackScheme > action )
	{
		tsWindows.forEach( action );
	}

	public void forEachView( final Consumer< ? super MamutView< ?, ?, ? > > action )
	{
		forEachBdvView( action );
		forEachTrackSchemeView( action );
		forEachTableView( action );
	}

	public MamutViewBdv createBigDataViewer()
	{
		if ( appModel != null )
		{
			final MamutViewBdv view = new MamutViewBdv( appModel );
			addBdvWindow( view );
			return view;
		}
		return null;
	}

	public MamutViewTrackScheme createTrackScheme()
	{
		if ( appModel != null )
		{
			final MamutViewTrackScheme view = new MamutViewTrackScheme( appModel );
			addTsWindow( view );
			return view;
		}
		return null;
	}

	/**
	 * Creates, shows and registers a new table view.
	 *
	 * @param selectionOnly
	 *            if <code>true</code>, the table will only display the current
	 *            content of the selection, and will listen to its changes. If
	 *            <code>false</code>, the table will display the full graph content,
	 *            listen to its changes, and will be able to edit the selection.
	 * @return a new table view.
	 */
	public MamutViewTable createTable( final boolean selectionOnly )
	{
		if ( appModel == null )
			return null;

		final MamutViewTable view = new MamutViewTable( appModel );
		final FeatureTagTablePanel< Spot > vertexTable = view.getFrame().getVertexTable();
		final FeatureTagTablePanel< Link > edgeTable = view.getFrame().getEdgeTable();
		final SelectionModel< Spot, Link > selectionModel = appModel.getSelectionModel();

		if ( selectionOnly )
		{
			// Pass only the selection.
			view.getFrame().setTitle( "Selection table" );
			view.getFrame().setMirrorSelection( false );
			final SelectionListener selectionListener = () -> {
				vertexTable.setRows( selectionModel.getSelectedVertices() );
				edgeTable.setRows( selectionModel.getSelectedEdges() );
			};
			selectionModel.listeners().add( selectionListener );
			selectionListener.selectionChanged();
			view.onClose( () -> selectionModel.listeners().remove( selectionListener ) );
		}
		else
		{
			// Pass and listen to the full graph.
			final ModelGraph graph = appModel.getModel().getGraph();
			final GraphChangeListener graphChangeListener = () -> {
				vertexTable.setRows( graph.vertices() );
				edgeTable.setRows( graph.edges() );
			};
			graph.addGraphChangeListener( graphChangeListener );
			graphChangeListener.graphChanged();
			view.onClose( () -> graph.removeGraphChangeListener( graphChangeListener ) );

			// Listen to selection changes.
			view.getFrame().setMirrorSelection( true );
			selectionModel.listeners().add( view.getFrame() );
			view.getFrame().selectionChanged();
			view.onClose( () -> selectionModel.listeners().remove( view.getFrame() ) );
		}
		/*
		 * Register a listener to vertex label property changes, will update the
		 * table-view when the label change.
		 */
		final SpotPool spotPool = ( SpotPool ) appModel.getModel().getGraph().vertices().getRefPool();
		spotPool.labelProperty().addPropertyChangeListener( v -> view.getFrame().repaint() );

		addTableWindow( view );
		return view;
	}

	public void editTagSets()
	{
		if ( appModel != null )
		{
			tagSetDialog.setVisible( true );
		}
	}

	public void computeFeatures()
	{
		if ( appModel != null )
		{
			featureComputationDialog.setVisible( true );
		}
	}

	public void closeAllWindows()
	{
		final ArrayList< JFrame > frames = new ArrayList<>();
		for ( final MamutViewBdv w : bdvWindows )
			frames.add( w.getFrame() );
		for ( final MamutViewTrackScheme w : tsWindows )
			frames.add( w.getFrame() );
		for ( final MamutViewTable w : tableWindows )
			frames.add( w.getFrame() );
		try
		{
			InvokeOnEDT.invokeAndWait( new Runnable()
			{
				@Override
				public void run()
				{
					for ( final JFrame f : frames )
						f.dispatchEvent( new WindowEvent( f, WindowEvent.WINDOW_CLOSING ) );
				}
			} );
		}
		catch ( final InvocationTargetException e )
		{
			e.printStackTrace();
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}

	KeyPressedManager getKeyPressedManager()
	{
		return keyPressedManager;
	}

	TrackSchemeStyleManager getTrackSchemeStyleManager()
	{
		return trackSchemeStyleManager;
	}

	RenderSettingsManager getRenderSettingsManager()
	{
		return renderSettingsManager;
	}

	FeatureColorModeManager getFeatureColorModeManager()
	{
		return featureColorModeManager;
	}

	KeymapManager getKeymapManager()
	{
		return keymapManager;
	}

	// TODO: make package private
	public MamutAppModel getAppModel()
	{
		return appModel;
	}

	Actions getGlobalAppActions()
	{
		return globalAppActions;
	}

	MamutPlugins getPlugins()
	{
		return plugins;
	}

	public Context getContext()
	{
		return context;
	}

	public FeatureSpecsService getFeatureSpecsService()
	{
		return context.getService( FeatureSpecsService.class );
	}

	/**
	 * Exposes the {@link ProjectManager} of this window manager, that handles
	 * project files.
	 *
	 * @return the project manager.
	 */
	public ProjectManager getProjectManager()
	{
		return projectManager;
	}

	private CommandDescriptions buildCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		context.inject( builder );
		builder.discoverProviders();
		return builder.build();
	}
}
