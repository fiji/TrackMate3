package org.mastodon.mamut;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.collection.IntRefMap;
import org.mastodon.collection.RefMaps;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.SelectionModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

/**
 * User-interface actions that are specific to the Mamut {@link Model} but independent of a view.
 *
 * @author Jean-Yves Tinevez
 */
public class MamutActions
{

	public static final String LINK_SELECTED_SPOTS = "link selected spots";

	public static final String[] LINK_SELECTED_SPOTS_KEYS = new String[] { "shift K" };

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
			descriptions.add( LINK_SELECTED_SPOTS, LINK_SELECTED_SPOTS_KEYS, "Link the spots currently selected. "
					+ "If there are more than one spot in a frame, only one of them is linked." );
		}
	}

	private final MamutAppModel appModel;

	private final LinkSpotsInSelection linkSpotsInSelection;


	public MamutActions( final MamutAppModel appModel )
	{
		this.appModel = appModel;
		this.linkSpotsInSelection = new LinkSpotsInSelection( LINK_SELECTED_SPOTS );
	}


	public static void install(
			final Actions actions,
			final MamutAppModel appModel)
	{
		final MamutActions mamutActions = new MamutActions(appModel);
		actions.namedAction( mamutActions.linkSpotsInSelection, LINK_SELECTED_SPOTS_KEYS );
	}


	private class LinkSpotsInSelection extends AbstractNamedAction
	{

		private static final long serialVersionUID = 1L;

		private LinkSpotsInSelection( final String name )
		{
			super( name );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			final Model model = appModel.getModel();
			final ModelGraph graph = model.getGraph();
			final ReentrantReadWriteLock lock = graph.getLock();
			final SelectionModel< Spot, Link > selection = appModel.getSelectionModel();

			/*
			 * Put all the selected vertices in a map frame -> vertex, so that
			 * we have at most one vertex per time-point. We ignore the rest.
			 */
			final IntRefMap< Spot > SpotMap = RefMaps.createIntRefMap( graph.vertices(), -1 );
			lock.readLock().lock();
			try
			{
				for ( final Spot v : selection.getSelectedVertices() )
					SpotMap.put( v.getTimepoint(), v );
			}
			finally
			{
				lock.readLock().unlock();
			}

			// Link spots.
			lock.writeLock().lock();
			final Spot ref1 = graph.vertexRef();
			final Spot ref2 = graph.vertexRef();
			final Link eref = graph.edgeRef();
			try
			{
				selection.pauseListeners();
				selection.clearSelection();

				// Get them out of the map in order.
				final int[] timepoints = SpotMap.keys();
				Arrays.sort( timepoints );
				Spot source = null;
				for ( final int timepoint : timepoints )
				{
					final Spot target = SpotMap.get( timepoint, ref2 );
					selection.setSelected( target, true );
					if ( null != source && ( null == graph.getEdge( source, target, eref ) ) )
					{
						final Link link = graph.addEdge( source, target, eref ).init();
						selection.setSelected( link, true );
					}
					else
					{
						source = ref1;
					}

					source.refTo( target );
				}
				model.setUndoPoint();
				graph.notifyGraphChanged();
			}
			finally
			{
				selection.resumeListeners();
				lock.writeLock().unlock();
				graph.releaseRef( ref1 );
				graph.releaseRef( ref2 );
				graph.releaseRef( eref );
			}
		}
	}
}
