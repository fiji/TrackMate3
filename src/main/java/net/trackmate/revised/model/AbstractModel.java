package net.trackmate.revised.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.trackmate.graph.AbstractEdge;
import net.trackmate.graph.AbstractVertex;
import net.trackmate.graph.GraphIdBimap;
import net.trackmate.graph.ReadOnlyGraph;
import net.trackmate.graph.listenable.ListenableGraph;
import net.trackmate.io.RawGraphIO;

/**
 * Manages the model graph.
 * <p>
 * The model graph is only exposed as a {@link ReadOnlyGraph}. All updates to
 * the model graph are done through {@link AbstractModel}. This includes vertex
 * and edge attribute changes (although this currently cannot be enforced
 * through {@link ReadOnlyGraph}).
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class AbstractModel<
		MG extends AbstractModelGraph< ?, ?, V, E, ? >,
		V extends AbstractVertex< V, E, ? >,
		E extends AbstractEdge< E, V, ? > >
{

	/**
	 * Exposes the graph managed by this model.
	 * <p>
	 * The graph is only exposed as a {@link ListenableGraph} which is a
	 * {@link ReadOnlyGraph}. All updates to the model graph must be done
	 * through this model instance directly.
	 *
	 * @return the graph.
	 */
	public ListenableGraph< V, E > getGraph()
	{
		return modelGraph;
	}

	/**
	 * Exposes the bidirectional map between vertices and their id, and between
	 * edges and their id.
	 *
	 * @return the bidirectional id map.
	 */
	public GraphIdBimap< V, E > getGraphIdBimap()
	{
		return modelGraph.idmap;
	}

	protected final MG modelGraph;

	protected AbstractModel( final MG modelGraph )
	{
		this.modelGraph = modelGraph;
	}

	/**
	 * Clears this model and loads the model from the specified raw file using
	 * the specified serializer.
	 *
	 * @param file
	 *            the raw file to load.
	 * @param serializer
	 *            the serializer used for reading individual vertices.
	 * @throws IOException
	 *             if an I/O error occurs while reading the file.
	 */
	protected void loadRaw( final File file, final RawGraphIO.Serializer< V, E > serializer ) throws IOException
	{
		final FileInputStream fis = new FileInputStream( file );
		final ObjectInputStream ois = new ObjectInputStream( fis );
		modelGraph.pauseListeners();
		modelGraph.clear();
		RawGraphIO.read( modelGraph, modelGraph.idmap, serializer, ois );
		ois.close();
		modelGraph.resumeListeners();
	}

	/**
	 * Saves this model to the specified raw file using the specified
	 * serializer.
	 *
	 * @param file
	 *            the raw file to save.
	 * @param serializer
	 *            the serializer used for writing individual vertices.
	 * @throws IOException
	 *             if an I/O error occurs while writing the file.
	 */
	protected void saveRaw( final File file, final RawGraphIO.Serializer< V, E > serializer ) throws IOException
	{
		final FileOutputStream fos = new FileOutputStream( file );
		final ObjectOutputStream oos = new ObjectOutputStream( fos );
		RawGraphIO.write( modelGraph, modelGraph.idmap, serializer, oos );
		oos.close();
	}
}
