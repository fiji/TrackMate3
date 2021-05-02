/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.mamut;

import bdv.BigDataViewerActions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ActionMap;

import org.mastodon.app.ui.MastodonFrameViewActions;
import org.mastodon.app.ui.ViewMenu;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.ui.SelectionActions;
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
		menuTexts.put( WindowManager.PREFERENCES_DIALOG, "Preferences..." );

		menuTexts.put( MastodonFrameViewActions.TOGGLE_SETTINGS_PANEL, "Settings Toolbar" );

		menuTexts.put( UndoActions.UNDO, "Undo" );
		menuTexts.put( UndoActions.REDO, "Redo" );

		menuTexts.put( SelectionActions.DELETE_SELECTION, "Delete Selection" );
		menuTexts.put( SelectionActions.SELECT_WHOLE_TRACK, "Select Whole Track" );
		menuTexts.put( SelectionActions.SELECT_TRACK_DOWNWARD, "Select Track Downward" );
		menuTexts.put( SelectionActions.SELECT_TRACK_UPWARD, "Select Track Upward" );

		menuTexts.put( BigDataViewerActions.LOAD_SETTINGS, "Load Bdv Settings" );
		menuTexts.put( BigDataViewerActions.SAVE_SETTINGS, "Save Bdv Settings" );
		menuTexts.put( BigDataViewerActions.BRIGHTNESS_SETTINGS, "Brightness & Color" );
		menuTexts.put( BigDataViewerActions.VISIBILITY_AND_GROUPING, "Visibility & Grouping" );

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

	public static MenuItem colorbarMenu( final JMenuHandle handle )
	{
		return ViewMenuBuilder.menu( "Colorbar", handle );
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
