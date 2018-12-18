package org.mastodon.revised.bdv.overlay;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.model.FocusModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.revised.bdv.overlay.util.JamaEigenvalueDecomposition;
import org.mastodon.revised.mamut.KeyConfigContexts;
import org.mastodon.revised.ui.keymap.CommandDescriptionProvider;
import org.mastodon.revised.ui.keymap.CommandDescriptions;
import org.mastodon.undo.UndoPointMarker;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import net.imglib2.util.LinAlgHelpers;

/**
 * Behaviours for editing spots in BDV views.
 *
 * @param <V>
 *            vertex type.
 * @param <E>
 *            edge type.
 *
 * @author Jean-Yves Tinevez
 * @author Tobias Pietzsch
 */
public class EditBehaviours< V extends OverlayVertex< V, E >, E extends OverlayEdge< E, V > >
{
	public static final String MOVE_SPOT = "move spot";
	public static final String ADD_SPOT = "add spot";
	public static final String INCREASE_SPOT_RADIUS = "increase spot radius";
	public static final String INCREASE_SPOT_RADIUS_ALOT = "increase spot radius a lot";
	public static final String INCREASE_SPOT_RADIUS_ABIT = "increase spot radius a bit";
	public static final String DECREASE_SPOT_RADIUS = "decrease spot radius";
	public static final String DECREASE_SPOT_RADIUS_ALOT = "decrease spot radius a lot";
	public static final String DECREASE_SPOT_RADIUS_ABIT = "decrease spot radius a bit";

	static final String[] MOVE_SPOT_KEYS = new String[] { "SPACE" };
	static final String[] ADD_SPOT_KEYS = new String[] { "A" };
	static final String[] INCREASE_SPOT_RADIUS_KEYS = new String[] { "E" };
	static final String[] INCREASE_SPOT_RADIUS_ALOT_KEYS = new String[] { "shift E" };
	static final String[] INCREASE_SPOT_RADIUS_ABIT_KEYS = new String[] { "control E" };
	static final String[] DECREASE_SPOT_RADIUS_KEYS = new String[] { "Q" };
	static final String[] DECREASE_SPOT_RADIUS_ALOT_KEYS = new String[] { "shift Q" };
	static final String[] DECREASE_SPOT_RADIUS_ABIT_KEYS = new String[] { "control Q" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( MOVE_SPOT, MOVE_SPOT_KEYS, "Move spot by mouse-dragging." );
			descriptions.add( ADD_SPOT, ADD_SPOT_KEYS, "Add spot at mouse position." );
			descriptions.add( INCREASE_SPOT_RADIUS, INCREASE_SPOT_RADIUS_KEYS, "Increase radius of spot at mouse position." );
			descriptions.add( INCREASE_SPOT_RADIUS_ALOT, INCREASE_SPOT_RADIUS_ALOT_KEYS, "Increase radius of spot at mouse position (a lot)." );
			descriptions.add( INCREASE_SPOT_RADIUS_ABIT, INCREASE_SPOT_RADIUS_ABIT_KEYS, "Increase radius of spot at mouse position (a little)." );
			descriptions.add( DECREASE_SPOT_RADIUS, DECREASE_SPOT_RADIUS_KEYS, "Decrease radius of spot at mouse position." );
			descriptions.add( DECREASE_SPOT_RADIUS_ALOT, DECREASE_SPOT_RADIUS_ALOT_KEYS, "Decrease radius of spot at mouse position (a lot)." );
			descriptions.add( DECREASE_SPOT_RADIUS_ABIT, DECREASE_SPOT_RADIUS_ABIT_KEYS, "Decrease radius of spot at mouse position (a little)." );
		}
	}

	public static final boolean FOCUS_EDITED_SPOT = true;

	public static final boolean SELECT_ADDED_SPOT = true;

	public static final double POINT_SELECT_DISTANCE_TOLERANCE = 5.0;

	/** Minimal radius below which changes in vertex size are rejected. */
	private static final double MIN_RADIUS = 1.0;

	/**
	 * Ratio by which we change the radius upon change radius action.
	 */
	private static final double NORMAL_RADIUS_CHANGE = 0.1;

	/**
	 * Ratio by which we change the radius upon change radius a bit action.
	 */
	private static final double ABIT_RADIUS_CHANGE = 0.01;

	/**
	 * Ratio by which we change the radius upon change radius a lot action.
	 */
	private static final double ALOT_RADIUS_CHANGE = 1.;

	private final AddSpotBehaviour addSpotBehaviour;

	private final MoveSpotBehaviour moveSpotBehaviour;

	private final ResizeSpotBehaviour increaseSpotRadiusBehaviour;

	private final ResizeSpotBehaviour increaseSpotRadiusBehaviourALot;

	private final ResizeSpotBehaviour increaseSpotRadiusBehaviourABit;

	private final ResizeSpotBehaviour decreaseSpotRadiusBehaviour;

	private final ResizeSpotBehaviour decreaseSpotRadiusBehaviourALot;

	private final ResizeSpotBehaviour decreaseSpotRadiusBehaviourABit;

	private double lastRadius = 10;

	public static < V extends OverlayVertex< V, E >, E extends OverlayEdge< E, V > > void install(
			final Behaviours behaviours,
			final OverlayGraph< V, E > overlayGraph,
			final OverlayGraphRenderer< V, E > renderer,
			final SelectionModel< V, E > selection,
			final FocusModel< V > focus,
			final UndoPointMarker undo )
	{
		final EditBehaviours< V, E > eb = new EditBehaviours<>( overlayGraph, renderer, selection, focus, undo, NORMAL_RADIUS_CHANGE, ABIT_RADIUS_CHANGE, ALOT_RADIUS_CHANGE );

		behaviours.namedBehaviour( eb.moveSpotBehaviour, MOVE_SPOT_KEYS );
		behaviours.namedBehaviour( eb.addSpotBehaviour, ADD_SPOT_KEYS );
		behaviours.namedBehaviour( eb.increaseSpotRadiusBehaviour, INCREASE_SPOT_RADIUS_KEYS );
		behaviours.namedBehaviour( eb.increaseSpotRadiusBehaviourABit, INCREASE_SPOT_RADIUS_ABIT_KEYS );
		behaviours.namedBehaviour( eb.increaseSpotRadiusBehaviourALot, INCREASE_SPOT_RADIUS_ALOT_KEYS );
		behaviours.namedBehaviour( eb.decreaseSpotRadiusBehaviour, DECREASE_SPOT_RADIUS_KEYS );
		behaviours.namedBehaviour( eb.decreaseSpotRadiusBehaviourABit, DECREASE_SPOT_RADIUS_ABIT_KEYS );
		behaviours.namedBehaviour( eb.decreaseSpotRadiusBehaviourALot, DECREASE_SPOT_RADIUS_ALOT_KEYS );
	}

	private final OverlayGraph< V, E > overlayGraph;

	private final ReentrantReadWriteLock lock;

	private final OverlayGraphRenderer< V, E > renderer;

	private final SelectionModel< V, E > selection;

	private final FocusModel< V > focus;

	private final UndoPointMarker undo;

	private EditBehaviours(
			final OverlayGraph< V, E > overlayGraph,
			final OverlayGraphRenderer< V, E > renderer,
			final SelectionModel< V, E > selection,
			final FocusModel< V > focus,
			final UndoPointMarker undo,
			final double normalRadiusChange,
			final double aBitRadiusChange,
			final double aLotRadiusChange )
	{
		this.overlayGraph = overlayGraph;
		this.lock = overlayGraph.getLock();
		this.renderer = renderer;
		this.selection = selection;
		this.focus = focus;
		this.undo = undo;

		moveSpotBehaviour = new MoveSpotBehaviour( MOVE_SPOT );
		addSpotBehaviour = new AddSpotBehaviour( ADD_SPOT );
		increaseSpotRadiusBehaviour = new ResizeSpotBehaviour( INCREASE_SPOT_RADIUS, normalRadiusChange );
		increaseSpotRadiusBehaviourALot = new ResizeSpotBehaviour( INCREASE_SPOT_RADIUS_ALOT, aLotRadiusChange );
		increaseSpotRadiusBehaviourABit = new ResizeSpotBehaviour( INCREASE_SPOT_RADIUS_ABIT, aBitRadiusChange );
		decreaseSpotRadiusBehaviour = new ResizeSpotBehaviour( DECREASE_SPOT_RADIUS, -normalRadiusChange / ( 1 + normalRadiusChange ) );
		decreaseSpotRadiusBehaviourALot = new ResizeSpotBehaviour( DECREASE_SPOT_RADIUS_ALOT, -aLotRadiusChange / ( 1 + aLotRadiusChange ) );
		decreaseSpotRadiusBehaviourABit = new ResizeSpotBehaviour( DECREASE_SPOT_RADIUS_ABIT, -aBitRadiusChange / ( 1 + aBitRadiusChange ) );
	}

	private class AddSpotBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		private final double[] pos;

		public AddSpotBehaviour( final String name )
		{
			super( name );
			pos = new double[ 3 ];
		}

		@Override
		public void click( final int x, final int y )
		{
			boolean isOutsideExistingSpot = false;
			lock.readLock().lock();
			try
			{
				final V ref = overlayGraph.vertexRef();
				isOutsideExistingSpot = renderer.getVertexAt( x, y, POINT_SELECT_DISTANCE_TOLERANCE, ref ) == null;
				overlayGraph.releaseRef( ref );
			}
			finally
			{
				lock.readLock().unlock();
			}

			// Only create a spot if we don't click inside an existing spot.
			if ( isOutsideExistingSpot )
			{
				final int timepoint = renderer.getCurrentTimepoint();
				renderer.getGlobalPosition( x, y, pos );
				final V ref = overlayGraph.vertexRef();
				lock.writeLock().lock();
				try
				{
					final V vertex = overlayGraph.addVertex( ref ).init( timepoint, pos, lastRadius );
					overlayGraph.notifyGraphChanged();

					undo.setUndoPoint();

					if ( FOCUS_EDITED_SPOT )
						focus.focus( vertex );

					if ( SELECT_ADDED_SPOT )
					{
						selection.pauseListeners();
						selection.clearSelection();
						selection.setSelected( vertex, true );
						selection.resumeListeners();
					}
				}
				finally
				{
					overlayGraph.releaseRef( ref );
					lock.writeLock().unlock();
				}
			}
		}
	}

	private class MoveSpotBehaviour extends AbstractNamedBehaviour implements DragBehaviour
	{
		private final double[] start;

		private final double[] pos;

		/**
		 * This is set to true in {@link #init(int, int)} if a vertex can be
		 * found at the start location. If it is false, {@link #drag(int, int)}
		 * and {@link #end(int, int)} don't do anything.
		 */
		private boolean moving;

		private final V ref;

		private V vertex;

		public MoveSpotBehaviour( final String name )
		{
			super( name );
			start = new double[ 3 ];
			pos = new double[ 3 ];
			moving = false;
			ref = overlayGraph.vertexRef();
		}

		@Override
		public void init( final int x, final int y )
		{
			lock.readLock().lock();
			vertex = renderer.getVertexAt( x, y, POINT_SELECT_DISTANCE_TOLERANCE, ref );
			if ( vertex != null )
			{
				renderer.getGlobalPosition( x, y, start );
				vertex.localize( pos );
				LinAlgHelpers.subtract( pos, start, start );
				moving = true;
			}
			else
				lock.readLock().unlock();
		}

		@Override
		public void drag( final int x, final int y )
		{
			if ( moving )
			{
				renderer.getGlobalPosition( x, y, pos );
				LinAlgHelpers.add( pos, start, pos );
				vertex.setPosition( pos );
			}
		}

		@Override
		public void end( final int x, final int y )
		{
			if ( moving )
			{
				undo.setUndoPoint();

				if ( FOCUS_EDITED_SPOT )
					focus.focus( vertex );

				moving = false;
				lock.readLock().unlock();
			}
		}
	};

	private class ResizeSpotBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		private final double[][] mat;

		private final double factor;

		private final JamaEigenvalueDecomposition eig;

		public ResizeSpotBehaviour( final String name, final double factor )
		{
			super( name );
			this.factor = factor;
			mat = new double[ 3 ][ 3 ];
			eig = new JamaEigenvalueDecomposition( 3 );
		}

		@Override
		public void click( final int x, final int y )
		{
			final V ref = overlayGraph.vertexRef();
			lock.readLock().lock();
			try
			{
				final V vertex = renderer.getVertexAt( x, y, POINT_SELECT_DISTANCE_TOLERANCE, ref );
				if ( vertex != null )
				{
					// Scale the covariance matrix.
					vertex.getCovariance( mat );
					LinAlgHelpers.scale( mat, 1 + factor, mat );

					// Check if the min radius is not too small.
					eig.decomposeSymmetric( mat );
					final double[] eigVals = eig.getRealEigenvalues();
					for ( final double eigVal : eigVals )
						if ( eigVal < MIN_RADIUS )
							return;

					vertex.setCovariance( mat );
					lastRadius = Math.max( MIN_RADIUS, Math.sqrt( vertex.getBoundingSphereRadiusSquared() ) );
					overlayGraph.notifyGraphChanged();
					undo.setUndoPoint();

					if ( FOCUS_EDITED_SPOT )
						focus.focus( vertex );
				}
			}
			finally
			{
				lock.readLock().unlock();
				overlayGraph.releaseRef( ref );
			}
		}
	}
}
