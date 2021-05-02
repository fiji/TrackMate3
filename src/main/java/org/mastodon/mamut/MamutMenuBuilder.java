package org.mastodon.mamut;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ActionMap;

import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.ui.SelectionActions;
import org.mastodon.views.bdv.BigDataViewerActionsMamut;
import org.mastodon.views.table.TableViewActions;
import org.mastodon.views.trackscheme.display.EditFocusVertexLabelAction;
import org.mastodon.views.trackscheme.display.TrackSchemeNavigationActions;

public class MamutMenuBuilder extends ViewMenuBuilder
{
	static Map< String, String > menuTexts = new HashMap<>();

	static
	{
		menuTexts.put( ProjectManager.CREATE_PROJECT, "New Project" );
		menuTexts.put( ProjectManager.LOAD_PROJECT, "Load Project" );
		menuTexts.put( ProjectManager.SAVE_PROJECT, "Save Project" );
		menuTexts.put( ProjectManager.IMPORT_TGMM, "Import TGMM tracks" );
		menuTexts.put( ProjectManager.IMPORT_SIMI, "Import Simi BioCell tracks" );
		menuTexts.put( ProjectManager.IMPORT_MAMUT, "Import MaMuT project" );
		menuTexts.put( ProjectManager.EXPORT_MAMUT, "Export MaMuT project" );

		menuTexts.put( WindowManager.NEW_BDV_VIEW, "New Bdv" );
		menuTexts.put( WindowManager.NEW_TRACKSCHEME_VIEW, "New Trackscheme" );
		menuTexts.put( WindowManager.NEW_TABLE_VIEW, "New data table" );
		menuTexts.put( WindowManager.NEW_SELECTION_TABLE_VIEW, "New selection table" );
		menuTexts.put( WindowManager.PREFERENCES_DIALOG, "Preferences..." );

		menuTexts.put( MastodonFrameViewActions.TOGGLE_SETTINGS_PANEL, "Settings Toolbar" );

		menuTexts.put( UndoActions.UNDO, "Undo" );
		menuTexts.put( UndoActions.REDO, "Redo" );

		menuTexts.put( SelectionActions.DELETE_SELECTION, "Delete Selection" );
		menuTexts.put( SelectionActions.SELECT_WHOLE_TRACK, "Select Whole Track" );
		menuTexts.put( SelectionActions.SELECT_TRACK_DOWNWARD, "Select Track Downward" );
		menuTexts.put( SelectionActions.SELECT_TRACK_UPWARD, "Select Track Upward" );

		menuTexts.put( BigDataViewerActionsMamut.LOAD_SETTINGS, "Load Bdv Settings" );
		menuTexts.put( BigDataViewerActionsMamut.SAVE_SETTINGS, "Save Bdv Settings" );
		menuTexts.put( BigDataViewerActionsMamut.BRIGHTNESS_SETTINGS, "Brightness & Color" );
		menuTexts.put( BigDataViewerActionsMamut.VISIBILITY_AND_GROUPING, "Visibility & Grouping" );

		menuTexts.put( TrackSchemeNavigationActions.NAVIGATE_CHILD, "Navigate to Child" );
		menuTexts.put( TrackSchemeNavigationActions.NAVIGATE_PARENT, "Navigate to Parent" );
		menuTexts.put( TrackSchemeNavigationActions.NAVIGATE_LEFT, "Navigate to Left" );
		menuTexts.put( TrackSchemeNavigationActions.NAVIGATE_RIGHT, "Navigate to Right" );
		menuTexts.put( TrackSchemeNavigationActions.SELECT_NAVIGATE_CHILD, "Select to Child" );
		menuTexts.put( TrackSchemeNavigationActions.SELECT_NAVIGATE_PARENT, "Select to Parent" );
		menuTexts.put( TrackSchemeNavigationActions.SELECT_NAVIGATE_LEFT, "Select to Left" );
		menuTexts.put( TrackSchemeNavigationActions.SELECT_NAVIGATE_RIGHT, "Select to Right" );
		menuTexts.put( TrackSchemeNavigationActions.TOGGLE_FOCUS_SELECTION, "Toggle Focused Vertex Selection" );

		menuTexts.put( EditFocusVertexLabelAction.EDIT_FOCUS_LABEL, "Edit Vertex Label" );

		menuTexts.put( TableViewActions.EDIT_LABEL, "Edit Vertex Label" );
		menuTexts.put( TableViewActions.TOGGLE_TAG, "Toggle Current Tag" );
		menuTexts.put( TableViewActions.EXPORT_TO_CSV, "Export to CSV" );
	}

	public static void build( final ViewMenu viewMenu, final ActionMap actionMap, final MenuItem... items )
	{
		final MenuItemVisitor visitor = new MenuItemVisitor( viewMenu, actionMap, menuTexts, null );
		Arrays.asList( items ).forEach( item -> item.accept( visitor ) );
	}

	public static MenuItem fileMenu( final MenuItem... items )
	{
		return ViewMenuBuilder.menu( "File", items );
	}

	public static MenuItem viewMenu( final MenuItem... items )
	{
		return ViewMenuBuilder.menu( "View", items );
	}

	public static MenuItem colorMenu( final JMenuHandle handle )
	{
		return ViewMenuBuilder.menu( "Coloring", handle );
	}

	public static MenuItem tagSetMenu( final JMenuHandle handle )
	{
		return ViewMenuBuilder.menu( "Tags", handle );
	}

	public static MenuItem editMenu( final MenuItem... items )
	{
		return ViewMenuBuilder.menu( "Edit", items );
	}

	public static MenuItem windowMenu( final MenuItem... items )
	{
		return ViewMenuBuilder.menu( "Window", items );
	}
}
