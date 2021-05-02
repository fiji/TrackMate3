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

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import org.mastodon.app.ViewGraph;
import org.mastodon.app.ui.MastodonFrameView;
import org.mastodon.app.ui.ViewMenuBuilder.JMenuHandle;
import org.mastodon.feature.FeatureModel;
import org.mastodon.graph.Edge;
import org.mastodon.graph.Vertex;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.ui.TagSetMenu;
import org.mastodon.ui.coloring.ColoringMenu;
import org.mastodon.ui.coloring.ColoringModel;
import org.mastodon.ui.coloring.GraphColorGeneratorAdapter;
import org.mastodon.ui.coloring.TagSetGraphColorGenerator;
import org.mastodon.ui.coloring.feature.FeatureColorModeManager;
import org.mastodon.views.trackscheme.display.ColorBarOverlay;
import org.mastodon.views.trackscheme.display.ColorBarOverlay.Position;

public class MamutView< VG extends ViewGraph< Spot, Link, V, E >, V extends Vertex< E >, E extends Edge< V > >
		extends MastodonFrameView< MamutAppModel, VG, Spot, Link, V, E >
{
	public MamutView( final MamutAppModel appModel, final VG viewGraph, final String[] keyConfigContexts )
	{
		super( appModel, viewGraph, keyConfigContexts );
	}

	/**
	 * Sets up and registers the coloring menu item and related actions and
	 * listeners. A new instance of the {@code ColoringModel} is created here and
	 * a reference on it is returned. This instance is bound to all relevant actions
	 * and is therefore knowledgeable of the currently used coloring style.
	 *
	 * @param colorGeneratorAdapter
	 *                                  adapts a (modifiable) model coloring to view
	 *                                  vertices/edges.
	 * @param menuHandle
	 *                                  handle to the JMenu corresponding to the
	 *                                  coloring submenu. Coloring options will be
	 *                                  installed here.
	 * @param refresh
<<<<<<< HEAD:src/main/java/org/mastodon/mamut/MamutView.java
	 *            triggers repaint of the graph (called when coloring changes)
     *
	 * @return reference on the underlying {@code ColoringModel}
=======
	 *                                  triggers repaint of the graph (called when
	 *                                  coloring changes)
	 * @return the coloring model of the coloring submenu.
>>>>>>> 5803e35e... Use the colorbar overlay in TrackScheme views.:src/main/java/org/mastodon/revised/mamut/MamutView.java
	 */
	protected ColoringModel registerColoring(
			final GraphColorGeneratorAdapter< Spot, Link, V, E > colorGeneratorAdapter,
			final JMenuHandle menuHandle,
			final Runnable refresh )
	{
		final TagSetModel< Spot, Link > tagSetModel = appModel.getModel().getTagSetModel();
		final FeatureModel featureModel = appModel.getModel().getFeatureModel();
		final FeatureColorModeManager featureColorModeManager = appModel.getFeatureColorModeManager();
		final ColoringModel coloringModel = new ColoringModel( tagSetModel, featureColorModeManager, featureModel );
		final ColoringMenu coloringMenu = new ColoringMenu( menuHandle.getMenu(), coloringModel );

		tagSetModel.listeners().add( coloringModel );
		onClose( () -> tagSetModel.listeners().remove( coloringModel ) );
		tagSetModel.listeners().add( coloringMenu );
		onClose( () -> tagSetModel.listeners().remove( coloringMenu ) );

		featureColorModeManager.listeners().add( coloringModel );
		onClose( () -> featureColorModeManager.listeners().remove( coloringModel ) );
		featureColorModeManager.listeners().add( coloringMenu );
		onClose( () -> featureColorModeManager.listeners().remove( coloringMenu ) );

		featureModel.listeners().add( coloringMenu );
		onClose( () -> featureModel.listeners().remove( coloringMenu ) );

		final ColoringModel.ColoringChangedListener coloringChangedListener = () -> {
			if ( coloringModel.noColoring() )
				colorGeneratorAdapter.setColorGenerator( null );
			else if ( coloringModel.getTagSet() != null )
				colorGeneratorAdapter.setColorGenerator( new TagSetGraphColorGenerator<>( tagSetModel, coloringModel.getTagSet() ) );
			else if ( coloringModel.getFeatureColorMode() != null )
				colorGeneratorAdapter.setColorGenerator( coloringModel.getFeatureGraphColorGenerator() );
			refresh.run();
		};
		coloringModel.listeners().add( coloringChangedListener );

		return coloringModel;
	}

	protected void registerColorbarOverlay(
			final ColorBarOverlay colorBarOverlay,
			final JMenuHandle menuHandle,
			final Runnable refresh )
	{
		menuHandle.getMenu().add( new JSeparator() );
		final JCheckBoxMenuItem toggleOverlay = new JCheckBoxMenuItem( "Show colorbar", ColorBarOverlay.DEFAULT_VISIBLE );
		toggleOverlay.addActionListener( ( l ) -> {
			colorBarOverlay.setVisible( toggleOverlay.isSelected() );
			refresh.run();
		} );
		menuHandle.getMenu().add( toggleOverlay );

		menuHandle.getMenu().add( new JSeparator() );
		menuHandle.getMenu().add( "Position:" ).setEnabled( false );

		final ButtonGroup buttonGroup = new ButtonGroup();
		for ( final Position position : Position.values() )
		{
			final JRadioButtonMenuItem positionItem = new JRadioButtonMenuItem( position.toString() );
			positionItem.addActionListener( ( l ) -> {
				if ( positionItem.isSelected() )
				{
					colorBarOverlay.setPosition( position );
					refresh.run();
				}
			} );
			buttonGroup.add( positionItem );
			menuHandle.getMenu().add( positionItem );

			if ( position.equals( ColorBarOverlay.DEFAULT_POSITION ) )
				positionItem.setSelected( true );
		}
	}

	protected void registerTagSetMenu(
			final JMenuHandle menuHandle,
			final Runnable refresh )
	{
		final SelectionModel< Spot, Link > selectionModel = appModel.getSelectionModel();
		final Model model = appModel.getModel();
		final TagSetModel< Spot, Link > tagSetModel = model.getTagSetModel();
		final TagSetMenu< Spot, Link > tagSetMenu = new TagSetMenu< >( menuHandle.getMenu(), tagSetModel, selectionModel, model.getGraph().getLock(), model );
		tagSetModel.listeners().add( tagSetMenu );
		onClose( () -> tagSetModel.listeners().remove( tagSetMenu ) );
	}
}
