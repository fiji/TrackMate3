package org.mastodon.views.table;

import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.table.TableModel;

import org.mastodon.app.MastodonAppModel;
import org.mastodon.app.ViewGraph;
import org.mastodon.graph.ref.AbstractListenableEdge;
import org.mastodon.model.AbstractSpot;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.mastodon.ui.util.ExtensionFileFilter;
import org.mastodon.ui.util.FileChooser;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import com.opencsv.CSVWriter;

public class TableViewActions<
			M extends MastodonAppModel< ?, V, E >,
			VG extends ViewGraph< V, E, V, E >,
			V extends AbstractSpot< V, E, ?, ?, ? >,
			E extends AbstractListenableEdge< E, V, ?, ? > >
{

	public static final String EDIT_LABEL = "edit vertex label";
	public static final String TOGGLE_TAG = "toggle tag";
	public static final String EXPORT_TO_CSV = "export to csv";

	private static final String[] EDIT_LABEL_KEYS = new String[] { "F2" };
	private static final String[] TOGGLE_TAG_KEYS = new String[] { "SPACE" };
	private static final String[] EXPORT_TO_CSV_KEYS = new String[] { "not mapped" };

	private final RunnableAction editLabel;

	private final RunnableAction toggleTag;

	private final RunnableAction exportToCSV;

	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.TABLE );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( EDIT_LABEL, EDIT_LABEL_KEYS, "Edit the label of the current vertex." );
			descriptions.add( TOGGLE_TAG, TOGGLE_TAG_KEYS, "Toggle the tag at the current cell in the table." );
			descriptions.add( EXPORT_TO_CSV, EXPORT_TO_CSV_KEYS, "Export the current content of the table to two CSV files (one for vertices, one for edges)." );
		}
	}

	private TableViewActions( final TableViewFrame< M, VG, V, E > tableView )
	{
		this.editLabel = new RunnableAction( EDIT_LABEL, tableView::editCurrentLabel );
		this.toggleTag = new RunnableAction( TOGGLE_TAG, tableView::toggleTag );
		this.exportToCSV = new RunnableAction( EXPORT_TO_CSV, () -> exportToCSV( tableView ) );
	}

	/**
	 * Create table-view actions and install them in the specified
	 * {@link Actions}.
	 *
	 * @param <M>
	 *            the type of the {@link MastodonAppModel}.
	 * @param <VG>
	 *            the type of the view graph used.
	 * @param <V>
	 *            the type of vertices in the graph.
	 * @param <E>
	 *            the type of edges in the graph.
	 * @param actions
	 *            Actions are added here.
	 * @param frame
	 *            Actions are targeted at this table view.
	 */
	public static <
		M extends MastodonAppModel< ?, V, E >,
		VG extends ViewGraph< V, E, V, E >,
		V extends AbstractSpot< V, E, ?, ?, ? >,
		E extends AbstractListenableEdge< E, V, ?, ? > > void install(
			final Actions actions,
			final TableViewFrame< M, VG, V, E > frame )
	{
		final TableViewActions< M, VG, V, E > tva = new TableViewActions<>( frame );
		actions.namedAction( tva.editLabel, EDIT_LABEL_KEYS );
		actions.namedAction( tva.toggleTag, TOGGLE_TAG_KEYS );
		actions.namedAction( tva.exportToCSV, EXPORT_TO_CSV_KEYS );
	}

	private static File csvFile;

	private static final < M extends MastodonAppModel< ?, V, E >, VG extends ViewGraph< V, E, V, E >, V extends AbstractSpot< V, E, ?, ?, ? >, E extends AbstractListenableEdge< E, V, ?, ? > >
			void exportToCSV( final TableViewFrame< M, VG, V, E > frame )
	{
		final Component parent = frame;
		final String filename = ( csvFile == null )
				? new File( System.getProperty( "user.home" ), "FeatureAndTagTable.csv" ).getAbsolutePath()
				: csvFile.getAbsolutePath();
		final File file = FileChooser.chooseFile(
				parent,
				filename,
				new ExtensionFileFilter( "csv" ),
				"Export Table content as CSV files",
				FileChooser.DialogType.SAVE );
		if ( file == null )
			return;
		csvFile = file;

		final int p = csvFile.getAbsolutePath().lastIndexOf( '.' );
		final String vertexPath = csvFile.getAbsolutePath().substring( 0, p ) + "-vertices.csv";
		try
		{
			export( vertexPath, frame.getVertexTable(), CSVWriter.DEFAULT_SEPARATOR );
		}
		catch ( final IOException e )
		{
			JOptionPane.showMessageDialog( frame.getVertexTable(),
					"Could not save to file " + vertexPath + ":\n" + e.getMessage(),
					"Error exporting vertices to CSV",
					JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
			return;
		}

		final String edgePath = csvFile.getAbsolutePath().substring( 0, p ) + "-edges.csv";
		try
		{
			export( edgePath, frame.getEdgeTable(), CSVWriter.DEFAULT_SEPARATOR );
		}
		catch ( final IOException e )
		{
			JOptionPane.showMessageDialog( frame.getEdgeTable(),
					"Could not save to file " + edgePath + ":\n" + e.getMessage(),
					"Error exporting edges to CSV",
					JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}

	}

	public static < O > void export( final String path, final FeatureTagTablePanel< O > table, final char separator ) throws IOException
	{
		try (CSVWriter writer = new CSVWriter( new FileWriter( new File( path ) ), separator ))
		{
			/*
			 * Headers are complicated: The first 2 columns are label and id,
			 * and they are not in column group. All the rest is, and have an
			 * extra line for units.
			 */
			final GroupableTableHeader header = ( GroupableTableHeader ) table.getTable().getTableHeader();
			final int nCols = table.getTable().getColumnCount();
			final String[][] headerEntries = new String[ 3 ][ nCols ];
			headerEntries[ 0 ][ 0 ] = header.getColumnModel().getColumn( 0 ).getHeaderValue().toString();
			headerEntries[ 1 ][ 0 ] = "";
			headerEntries[ 2 ][ 0 ] = "";
			headerEntries[ 0 ][ 1 ] = header.getColumnModel().getColumn( 1 ).getHeaderValue().toString();
			headerEntries[ 1 ][ 1 ] = "";
			headerEntries[ 2 ][ 1 ] = "";

			if ( null != header.columnGroups)
			{
				int lcol = 2;
				for ( final ColumnGroup cg : header.columnGroups )
				{
					for ( final Object obj : cg.v )
					{
						final ColumnGroup cg2 = ( ColumnGroup ) obj;
						headerEntries[ 0 ][ lcol ] = cg.text;
						headerEntries[ 1 ][ lcol ] = cg2.text;
						headerEntries[ 2 ][ lcol ] = header.getColumnModel().getColumn( lcol ).getHeaderValue().toString();
						lcol++;
					}
				}
			}
			for ( int hr = 0; hr < 3; hr++ )
				writer.writeNext( headerEntries[ hr ] );

			/*
			 * Content.
			 */
			final String[] content = new String[ nCols ];
			final int nRows = table.getTable().getRowCount();
			final TableModel model = table.getTable().getModel();
			for ( int r = 0; r < nRows; r++ )
			{
				final int row = table.getTable().convertRowIndexToModel( r );
				for ( int col = 0; col < nCols; col++ )
				{
					final Object obj = model.getValueAt( row, col );
					if ( null == obj )
						content[ col ] = "";
					else if ( obj instanceof Integer )
						content[ col ] = Integer.toString( ( Integer ) obj );
					else if ( obj instanceof Double )
						content[ col ] = Double.toString( ( Double ) obj );
					else if ( obj instanceof Boolean )
						content[ col ] = ( ( Boolean ) obj ) ? "1" : "0";
					else
						content[ col ] = obj.toString();
				}
				writer.writeNext( content );
			}
		}
	}
}
