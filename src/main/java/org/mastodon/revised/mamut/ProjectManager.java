package org.mastodon.revised.mamut;

import java.awt.Component;
import java.awt.Dialog;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.swing.JFrame;

import org.mastodon.graph.GraphChangeListener;
import org.mastodon.plugin.MastodonPlugins;
import org.mastodon.revised.bdv.SharedBigDataViewerData;
import org.mastodon.revised.bdv.overlay.ui.RenderSettingsManager;
import org.mastodon.revised.mamut.feature.MamutFeatureComputerService;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.trackmate.MamutExporter;
import org.mastodon.revised.model.mamut.trackmate.TrackMateImporter;
import org.mastodon.revised.model.tag.TagSetStructure;
import org.mastodon.revised.trackscheme.display.style.TrackSchemeStyleManager;
import org.mastodon.revised.ui.keymap.CommandDescriptionProvider;
import org.mastodon.revised.ui.keymap.CommandDescriptions;
import org.mastodon.revised.ui.keymap.KeymapManager;
import org.mastodon.revised.ui.util.FileChooser;
import org.mastodon.revised.ui.util.FileChooser.SelectionMode;
import org.mastodon.revised.ui.util.XmlFileFilter;
import org.mastodon.revised.util.DummySpimData;
import org.mastodon.revised.util.ToggleDialogAction;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.SpimDataException;

public class ProjectManager implements GraphChangeListener
{
	public static final String CREATE_PROJECT = "create new project";
	public static final String LOAD_PROJECT = "load project";
	public static final String SAVE_PROJECT = "save project";
	public static final String IMPORT_TGMM = "import tgmm";
	public static final String IMPORT_SIMI = "import simi";
	public static final String IMPORT_MAMUT = "import mamut";
	public static final String EXPORT_MAMUT = "export mamut";

	static final String[] CREATE_PROJECT_KEYS = new String[] { "not mapped" };
	static final String[] LOAD_PROJECT_KEYS = new String[] { "not mapped" };
	static final String[] SAVE_PROJECT_KEYS = new String[] { "not mapped" };
	static final String[] IMPORT_TGMM_KEYS = new String[] { "not mapped" };
	static final String[] IMPORT_SIMI_KEYS = new String[] { "not mapped" };
	static final String[] IMPORT_MAMUT_KEYS = new String[] { "not mapped" };
	static final String[] EXPORT_MAMUT_KEYS = new String[] { "not mapped" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( CREATE_PROJECT, CREATE_PROJECT_KEYS, "Create a new project." );
			descriptions.add( LOAD_PROJECT, LOAD_PROJECT_KEYS, "Load a project." );
			descriptions.add( SAVE_PROJECT, SAVE_PROJECT_KEYS, "Save the current project." );
			descriptions.add( IMPORT_TGMM, IMPORT_TGMM_KEYS, "Import tracks from TGMM xml files into the current project." );
			descriptions.add( IMPORT_SIMI, IMPORT_SIMI_KEYS, "Import tracks from a Simi Biocell .sbd into the current project." );
			descriptions.add( IMPORT_MAMUT, IMPORT_MAMUT_KEYS, "Import a MaMuT project." );
			descriptions.add( EXPORT_MAMUT, EXPORT_MAMUT_KEYS, "Export current project as a MaMuT project." );
		}
	}

	private final WindowManager windowManager;

	private final TgmmImportDialog tgmmImportDialog;

	private final SimiImportDialog simiImportDialog;

	private MamutProject project;

	private File proposedProjectFolder;

	private final AbstractNamedAction createProjectAction;

	private final AbstractNamedAction loadProjectAction;

	private final AbstractNamedAction saveProjectAction;

	private final AbstractNamedAction importTgmmAction;

	private final AbstractNamedAction importSimiAction;

	private final AbstractNamedAction importMamutAction;

	private final AbstractNamedAction exportMamutAction;

	/** When was the project last saved. */
	private long saveTimestamp = -1l;

	/** Whem was the model last changed. */
	private long lastModelChange = -1l;

	public ProjectManager( final WindowManager windowManager )
	{
		this.windowManager = windowManager;

		tgmmImportDialog = new TgmmImportDialog( null );
		simiImportDialog = new SimiImportDialog( null );

		createProjectAction = new RunnableAction( CREATE_PROJECT, this::createProject );
		loadProjectAction = new RunnableAction( LOAD_PROJECT, this::loadProject );
		saveProjectAction = new RunnableAction( SAVE_PROJECT, this::saveProject );
		importTgmmAction = new RunnableAction( IMPORT_TGMM, this::importTgmm );
		importSimiAction = new RunnableAction( IMPORT_SIMI, this::importSimi );
		importMamutAction = new RunnableAction( IMPORT_MAMUT, this::importMamut );
		exportMamutAction = new RunnableAction( EXPORT_MAMUT, this::exportMamut );

		updateEnabledActions();
	}

	private void updateEnabledActions()
	{
		final boolean projectOpen = ( project != null );
		saveProjectAction.setEnabled( projectOpen );
		importTgmmAction.setEnabled( projectOpen );
		importSimiAction.setEnabled( projectOpen );
		exportMamutAction.setEnabled( projectOpen );
	}

	/**
	 * Add Project New/Load/Save actions and install them in the specified
	 * {@link Actions}.
	 *
	 * @param actions
	 *            Actions are added here.
	 */
	public void install( final Actions actions )
	{
		actions.namedAction( createProjectAction, CREATE_PROJECT_KEYS );
		actions.namedAction( loadProjectAction, LOAD_PROJECT_KEYS );
		actions.namedAction( saveProjectAction, SAVE_PROJECT_KEYS );
		actions.namedAction( importTgmmAction, IMPORT_TGMM_KEYS );
		actions.namedAction( importSimiAction, IMPORT_SIMI_KEYS );
		actions.namedAction( importMamutAction, IMPORT_MAMUT_KEYS );
		actions.namedAction( exportMamutAction, EXPORT_MAMUT_KEYS );
	}

	public synchronized void createProject()
	{
		final Component parent = null; // TODO
		final File file = FileChooser.chooseFile(
				parent,
				null,
				new XmlFileFilter(),
				"Open BigDataViewer File",
				FileChooser.DialogType.LOAD );
		if ( file == null )
			return;

		try
		{
			open( new MamutProject( null, file ) );
		}
		catch ( final IOException | SpimDataException e )
		{
			e.printStackTrace();
		}
	}

	public synchronized void loadProject()
	{
		String fn = null;
		if ( proposedProjectFolder != null )
			fn = proposedProjectFolder.getAbsolutePath();
		else if ( project != null && project.getProjectFolder() != null )
			fn = project.getProjectFolder().getAbsolutePath();
		final Component parent = null; // TODO
		final File file = FileChooser.chooseFile(
				parent,
				fn,
				null,
				"Open Mastodon Project",
				FileChooser.DialogType.LOAD,
				SelectionMode.DIRECTORIES_ONLY );
		if ( file == null )
			return;

		try
		{
			proposedProjectFolder = file;
			final MamutProject project = new MamutProjectIO().load( file.getAbsolutePath() );
			open( project );
		}
		catch ( final IOException | SpimDataException e )
		{
			e.printStackTrace();
		}
	}

	public synchronized void saveProject()
	{
		if ( project == null )
			return;

		final String folderPath = getProposedProjectFolder( project );

		final Component parent = null; // TODO
		final File folder = FileChooser.chooseFile(
				parent,
				folderPath,
				null,
				"Save Mastodon Project",
				FileChooser.DialogType.SAVE,
				SelectionMode.DIRECTORIES_ONLY );
		if ( folder == null )
			return;

		try
		{
			proposedProjectFolder = folder;
			saveProject( proposedProjectFolder );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	public synchronized void saveProject( final File projectFolder ) throws IOException
	{
		if ( project == null )
			return;

		project.setProjectFolder( projectFolder );
		new MamutProjectIO().save( project );

		final Model model = windowManager.getAppModel().getModel();
		model.saveRaw( project );

		saveTimestamp = System.currentTimeMillis();
		updateEnabledActions();
	}

	/**
	 * Open a project. If {@code project.getProjectFolder() == null} this is a new project and data structures are initialized as empty.
	 * The image data {@code project.getDatasetXmlFile()} must always be set.
	 *
	 * @param project
	 *
	 * @throws IOException
	 * @throws SpimDataException
	 */
	public synchronized void open( final MamutProject project ) throws IOException, SpimDataException
	{
		/*
		 * Load Model
		 */
		final Model model = new Model();
		final boolean isNewProject = project.getProjectFolder() == null;

		if ( !isNewProject )
		{
			model.loadRaw( project );
			this.saveTimestamp = System.currentTimeMillis();
		}
		else
		{
			this.saveTimestamp = -2l;
		}

		/*
		 * Load SpimData
		 */
		final String spimDataXmlFilename = project.getDatasetXmlFile().getAbsolutePath();
		SpimDataMinimal spimData = DummySpimData.tryCreate( project.getDatasetXmlFile().getName() );
		if ( spimData == null )
			spimData = new XmlIoSpimDataMinimal().load( spimDataXmlFilename );

		final KeyPressedManager keyPressedManager = windowManager.getKeyPressedManager();
		final TrackSchemeStyleManager trackSchemeStyleManager = windowManager.getTrackSchemeStyleManager();
		final RenderSettingsManager renderSettingsManager = windowManager.getRenderSettingsManager();
		final KeymapManager keymapManager = windowManager.getKeymapManager();
		final MastodonPlugins plugins = windowManager.getPlugins();
		final Actions globalAppActions = windowManager.getGlobalAppActions();
		final ViewerOptions options = ViewerOptions.options().shareKeyPressedEvents( keyPressedManager );
		final SharedBigDataViewerData sharedBdvData = new SharedBigDataViewerData(
				spimDataXmlFilename,
				spimData,
				options,
				() -> windowManager.forEachBdvView( bdv -> bdv.requestRepaint() ) );

		final MamutAppModel appModel = new MamutAppModel( model, sharedBdvData, keyPressedManager, trackSchemeStyleManager, renderSettingsManager, keymapManager, plugins, globalAppActions );

		/*
		 * Feature calculation.
		 */

		/*
		 * TODO REMOVE
		 * Set TagSetStructure for debugging.
		 */
		if ( isNewProject )
		{
			final TagSetStructure tss = new TagSetStructure();
			final Random ran = new Random( 0l );
			final TagSetStructure.TagSet reviewedByTag = tss.createTagSet( "Reviewed by" );
			reviewedByTag.createTag( "Pavel", ran.nextInt() | 0xFF000000 );
			reviewedByTag.createTag( "Mette", ran.nextInt() | 0xFF000000 );
			reviewedByTag.createTag( "Tobias", ran.nextInt() | 0xFF000000 );
			reviewedByTag.createTag( "JY", ran.nextInt() | 0xFF000000 );
			final TagSetStructure.TagSet locationTag = tss.createTagSet( "Location" );
			locationTag.createTag( "Anterior", ran.nextInt() | 0xFF000000 );
			locationTag.createTag( "Posterior", ran.nextInt() | 0xFF000000 );
			System.out.println( "Initial TagSetStructure:\n" + tss );
			model.getTagSetModel().setTagSetStructure( tss );
		}

		if ( windowManager.getAppModel() != null && windowManager.getAppModel().getModel() != null )
			windowManager.getAppModel().getModel().getGraph().removeGraphChangeListener( this );

		windowManager.setAppModel( appModel );

		final MamutFeatureComputerService featureComputerService = windowManager.getContext().getService( MamutFeatureComputerService.class );
		final JFrame owner = null; // TODO
		final Dialog featureComputationDialog = new FeatureAndTagDialog( owner, model, featureComputerService );
		featureComputationDialog.setSize( 400, 400 );

		final ToggleDialogAction toggleFeatureComputationDialogAction = new ToggleDialogAction( "feature computation", featureComputationDialog );

		this.project = project;
		appModel.getModel().getGraph().addGraphChangeListener( this );
		updateEnabledActions();
	}

	public boolean projectNeedsSave()
	{
		return lastModelChange > saveTimestamp;
	}

	public synchronized void importTgmm()
	{
		if ( project == null )
			return;

		final MamutAppModel appModel = windowManager.getAppModel();
		tgmmImportDialog.showImportDialog( appModel.getSharedBdvData().getSpimData(), appModel.getModel() );

		updateEnabledActions();
	}

	public synchronized void importSimi()
	{
		if ( project == null )
			return;

		final MamutAppModel appModel = windowManager.getAppModel();
		simiImportDialog.showImportDialog( appModel.getSharedBdvData().getSpimData(), appModel.getModel() );

		updateEnabledActions();
	}

	public synchronized void importMamut()
	{
		final Component parent = null; // TODO
		final File file = FileChooser.chooseFile(
				parent,
				null,
				new XmlFileFilter(),
				"Import MaMuT Project",
				FileChooser.DialogType.LOAD );
		if ( file == null )
			return;

		try
		{
			final TrackMateImporter importer = new TrackMateImporter( file );
			open( importer.createProject() );
			importer.readModel( windowManager.getAppModel().getModel() );
		}
		catch ( final IOException | SpimDataException e )
		{
			e.printStackTrace();
		}

		updateEnabledActions();
	}

	public synchronized void exportMamut()
	{
		if ( project == null )
			return;

		final String filename = getProprosedMamutExportFileName( project );

		final Component parent = null; // TODO
		final File file = FileChooser.chooseFile(
				parent,
				filename,
				new XmlFileFilter(),
				"Export As MaMuT Project",
				FileChooser.DialogType.SAVE );
		if ( file == null )
			return;

		try
		{
			MamutExporter.export( file, windowManager.getAppModel().getModel(), project );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	private static String getProprosedMamutExportFileName( final MamutProject project )
	{
		final File pf = project.getProjectFolder();
		if ( pf != null )
		{
			return new File( pf.getParentFile(), pf.getName() + "_mamut.xml" ).getAbsolutePath();
		}
		else
		{
			final File f = project.getDatasetXmlFile();
			final String fn = f.getName();
			return new File( f.getParentFile(), fn.substring( 0, fn.length() - ".xml".length() ) + "_mamut.xml" ).getAbsolutePath();
		}
	}

	private static String getProposedProjectFolder( final MamutProject project )
	{
		if ( project.getProjectFolder() != null )
			return project.getProjectFolder().getAbsolutePath();
		else
			return project.getDatasetXmlFile().getParentFile().getAbsolutePath();
	}

	@Override
	public void graphChanged()
	{
		lastModelChange = System.currentTimeMillis();
	}
}
