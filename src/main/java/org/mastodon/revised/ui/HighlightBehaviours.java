package org.mastodon.revised.ui;

import org.mastodon.graph.Edge;
import org.mastodon.graph.GraphChangeNotifier;
import org.mastodon.graph.ListenableGraph;
import org.mastodon.graph.Vertex;
import org.mastodon.revised.ui.selection.HighlightModel;
import org.mastodon.undo.UndoPointMarker;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

/**
 * User-interface actions that are related to a model highlight.
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt;
 *
 */
public class HighlightBehaviours< V extends Vertex< E >, E extends Edge< V > >
		extends Behaviours
{

	private static final String REMOVE_HIGHLIGHTED_VERTEX = "remove highlighted vertex";
	private static final String REMOVE_HIGHLIGHTED_EDGE = "remove highlighted edge";

	private static final String[] REMOVE_HIGHLIGHTED_VERTEX_KEYS = new String[] { "D" };
	private static final String[] REMOVE_HIGHLIGHTED_EDGE_KEYS = new String[] { "D" };

	public static < V extends Vertex< E >, E extends Edge< V > > void installActionBindings(
			final TriggerBehaviourBindings triggerBehaviourBindings,
			final InputTriggerConfig config,
			final String[] keyConfigContexts,
			final ListenableGraph< V, E > graph,
			final GraphChangeNotifier notify,
			final HighlightModel< V, E > highlight,
			final UndoPointMarker undo )
	{
		new HighlightBehaviours<>( config, keyConfigContexts, graph, notify, highlight, undo )
				.install( triggerBehaviourBindings, "highlight" );
	}

	private final ListenableGraph< V, E > graph;

	private final GraphChangeNotifier notify;

	private final HighlightModel< V, E > highlight;

	private final UndoPointMarker undo;

	private HighlightBehaviours(
			final InputTriggerConfig config,
			final String[] keyConfigContexts,
			final ListenableGraph< V, E > graph,
			final GraphChangeNotifier notify,
			final HighlightModel< V, E > highlight,
			final UndoPointMarker undo )
	{
		super( config, keyConfigContexts );
		this.graph = graph;
		this.notify = notify;
		this.highlight = highlight;
		this.undo = undo;

		behaviour( new RemoveHighlightedVertex(), REMOVE_HIGHLIGHTED_VERTEX, REMOVE_HIGHLIGHTED_VERTEX_KEYS );
		behaviour( new RemoveHighlightedEdge(), REMOVE_HIGHLIGHTED_EDGE, REMOVE_HIGHLIGHTED_EDGE_KEYS );
	}

	private class RemoveHighlightedVertex implements ClickBehaviour
	{
		@Override
		public void click( final int x, final int y )
		{
			final V ref = graph.vertexRef();
			final V v = highlight.getHighlightedVertex( ref );
			if ( v != null )
			{
				graph.remove( v );
				undo.setUndoPoint();
				notify.notifyGraphChanged();
			}
			graph.releaseRef( ref );
		}
	}

	private class RemoveHighlightedEdge implements ClickBehaviour
	{
		@Override
		public void click( final int x, final int y )
		{
			final E ref = graph.edgeRef();
			final E e = highlight.getHighlightedEdge( ref );
			if ( e != null )
			{
				graph.remove( e );
				undo.setUndoPoint();
				notify.notifyGraphChanged();
			}
			graph.releaseRef( ref );
		}
	}

}
