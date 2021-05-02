package org.mastodon.views.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;

import org.mastodon.RefPool;
import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureProjection;
import org.mastodon.feature.FeatureProjectionKey;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.feature.IntFeatureProjection;
import org.mastodon.model.tag.ObjTags;
import org.mastodon.model.tag.TagSetStructure.Tag;
import org.mastodon.model.tag.TagSetStructure.TagSet;
import org.mastodon.ui.coloring.ColorGenerator;
import org.mastodon.undo.UndoPointMarker;

import gnu.trove.map.hash.TIntIntHashMap;

public class FeatureTagTablePanel< O > extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final Color HIGHLIGHT_BORDER_COLOR = Color.BLACK;

	private static final Color HEADER_BG_COLOR = new Color( 245, 245, 245 );

	private static final Font FOCUS_FONT = new JLabel().getFont().deriveFont( Font.ITALIC );

	private static final int ROW_HEIGHT = 26;

	private final RefPool< O > idBimap;

	private final ObjTags< O > tags;

	private final JTable table;

	private final MyTableModel tableModel;

	private final MyTableCellRenderer cellRenderer;

	private final List< Class< ? > > columnClasses;

	private final List< FeatureProjection< O > > mapToProjections;

	private final List< String > mapToTooltip;

	private final List< int[] > mapToTagIndices;

	private final List< TagSet > tagSets;

	private final Map< FeatureSpec< ?, ? >, Feature< ? > > featureMap;

	private final Function< O, String > labelGenerator;

	private final BiConsumer< O, String > labelSetter;

	private final UndoPointMarker undoPointMarker;

	private int focusRow = -1;

	private int highlightRow = -1;

	/**
	 * Map of model row in the table to the id of the object the row display.
	 */
	private TIntIntHashMap rowMap = new TIntIntHashMap( 1, 0.5f, -1, -1 );

	/**
	 * Map of object ids to their model row in the table.
	 */
	private TIntIntHashMap idMap = new TIntIntHashMap( 1, 0.5f, -1, -1 );

	/**
	 * Map of filtered model row in the table to the id of the object the row
	 * display.
	 */
	private TIntIntHashMap filterRowMap;

	/**
	 * Map of filtered object ids to their model row in the table.
	 */
	private TIntIntHashMap filterIdMap = new TIntIntHashMap( 1, 0.5f, -1, -1 );

	private boolean doFilter = false;

	private final ColorGenerator< O > coloring;

	public FeatureTagTablePanel(
			final ObjTags< O > tags,
			final RefPool< O > idBimap,
			final Function< O, String > labelGenerator,
			final BiConsumer< O, String > labelSetter,
			final UndoPointMarker undoPointMarker,
			final ColorGenerator< O > coloring )
	{
		this.tags = tags;
		this.idBimap = idBimap;
		this.labelGenerator = labelGenerator;
		this.labelSetter = labelSetter;
		this.undoPointMarker = undoPointMarker;
		this.coloring = coloring;
		this.columnClasses = new ArrayList<>();
		this.mapToProjections = new ArrayList<>();
		this.mapToTooltip = new ArrayList<>();
		this.mapToTagIndices = new ArrayList<>();
		this.featureMap = new LinkedHashMap<>();
		this.tagSets = new ArrayList<>();

		this.tableModel = new MyTableModel();
		final DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
		this.table = new JTable( tableModel, tableColumnModel )
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected JTableHeader createDefaultTableHeader()
			{
				return new GroupableTableHeader( columnModel );
			}

			@Override
			public boolean isCellEditable( final int row, final int column )
			{
				// Only label and tags are editable.
				return ( labelSetter != null && column == 0 ) || column >= 2 + mapToProjections.size();
			}

			@Override
			public boolean editCellAt( final int row, final int column, final EventObject e )
			{
				// Intercept to block keys.
				final boolean result = super.editCellAt( row, column, e );
				final Component editor = getEditorComponent();
				if ( editor == null || !( editor instanceof JTextComponent ) )
				{ return result; }

				// Is about text.
				blockKeys( ( JComponent ) editor, table );
				if ( e instanceof MouseEvent )
					EventQueue.invokeLater( () -> ( ( JTextComponent ) editor ).selectAll() );
				else
					SwingUtilities.invokeLater( new Runnable()
					{
						@Override
						public void run()
						{
							( ( JTextComponent ) editor ).selectAll();
							( ( JTextComponent ) editor ).requestFocusInWindow();
						}
					} );
				return result;
			}
		};
		table.putClientProperty( "JTable.autoStartsEdit", Boolean.FALSE );

		table.setRowHeight( ROW_HEIGHT );
		this.cellRenderer = new MyTableCellRenderer();

		table.getSelectionModel().setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		refreshColumns();

		final TableRowSorter< MyTableModel > sorter = new TableRowSorter<>( tableModel );
		table.setRowSorter( sorter );

		final JScrollPane scroll = new JScrollPane( table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		setLayout( new BorderLayout() );
		add( scroll, BorderLayout.CENTER );
	}

	/**
	 * Exposes the {@link JTable} in which the data is displayed.
	 *
	 * @return the table.
	 */
	public JTable getTable()
	{
		return table;
	}

	/**
	 * Returns the object listed at the specified <b>view</b> row index.
	 *
	 * @param viewRowIndex
	 *            the row to query.
	 * @param ref
	 *            a ref object for allocation free retrieval.
	 * @return the object or <code>null</code> if the view row does not
	 *         correspond to an object currently displayed.
	 */
	public O getObjectForViewRow( final int viewRowIndex, final O ref )
	{
		if ( viewRowIndex < 0 )
			return null;
		final int modelRow = table.convertRowIndexToModel( viewRowIndex );
		final int id = doFilter ? filterRowMap.get( modelRow ) : rowMap.get( modelRow );
		return idBimap.getObjectIfExists( id, ref );
	}

	public int getViewRowForObject( final O o )
	{
		final int modeRow = doFilter ? filterIdMap.get( idBimap.getId( o ) ) : idMap.get( idBimap.getId( o ) );
		if ( modeRow < 0 ) // Object not in table.
			return -1;
		return table.convertRowIndexToView( modeRow );
	}

	/**
	 * Returns the object listed at the specified <b>view</b> row index.
	 *
	 * @param viewRowIndex
	 *            the row to query.
	 * @return the object.
	 */
	public O getObjectForViewRow( final int viewRowIndex )
	{
		return getObjectForViewRow( viewRowIndex, idBimap.createRef() );
	}

	public void scrollToObject( final O o )
	{
		final Rectangle rect = table.getVisibleRect();
		final int row = getViewRowForObject( o );
		final Rectangle cellRect = table.getCellRect( row, 0, true );
		cellRect.setLocation( rect.x, cellRect.y );
		table.scrollRectToVisible( cellRect );
	}

	/**
	 * Sets the collection of objects to display in this table.
	 *
	 * @param rows
	 *            the collection of objects to display (unchanged).
	 */
	public void setRows( final Collection< O > rows )
	{
		final TIntIntHashMap idMap = new TIntIntHashMap( rows.size(), 0.5f, -1, -1 );
		final TIntIntHashMap rowMap = new TIntIntHashMap( rows.size(), 0.5f, -1, -1 );
		int row = 0;
		for ( final O o : rows )
		{
			final int id = idBimap.getId( o );
			rowMap.put( row, id );
			idMap.put( id, row );
			row++;
		}

		synchronized ( this )
		{
			this.rowMap = rowMap;
			this.idMap = idMap;
		}
		tableModel.fireTableDataChanged();
	}

	public void filter( final Collection< O > content )
	{
		if ( null == content )
		{
			doFilter = false;
		}
		else
		{
			doFilter = true;
			final TIntIntHashMap filterIdMap = new TIntIntHashMap( content.size(), 0.5f, -1, -1 );
			final TIntIntHashMap filterRowMap = new TIntIntHashMap( content.size(), 0.5f, -1, -1 );

			int filteredRow = 0;
			for ( final O o : content )
			{
				final int id = idBimap.getId( o );
				final int row = idMap.get( id );
				if ( row < 0 )
					continue;

				filterIdMap.put( id, filteredRow );
				filterRowMap.put( filteredRow, id );
				filteredRow++;
			}

			synchronized ( this )
			{
				this.filterIdMap = filterIdMap;
				this.filterRowMap = filterRowMap;
			}
		}
		tableModel.fireTableDataChanged();
	}

	private void refreshColumns()
	{
		// Class of columns.
		columnClasses.clear();
		// Last line of header is for units.
		final List< String > lastHeaderLine = new ArrayList<>();
		// Map from column index to projection keys.
		mapToProjections.clear();
		// Map from column index to tooltip strings.
		mapToTooltip.clear();
		// Map from column index to tag indices.
		mapToTagIndices.clear();
		// Table column model.
		final TableColumnModel tableColumnModel = new DefaultTableColumnModel();
		table.setColumnModel( tableColumnModel );

		final GroupableTableHeader header = ( GroupableTableHeader ) table.getTableHeader();
		final TableCellRenderer defaultRenderer = header.getDefaultRenderer();
		header.clear();
		// Provide tooltips on the fly.
		header.addMouseMotionListener( new MyTableToolTipProvider() );

		// Top header, bold and not the last line.
		final MyTopHeaderRenderer headerRenderer = new MyTopHeaderRenderer( defaultRenderer, false, true );
		// Top header, last line, not bold.
		final MyTopHeaderRenderer headerRendererLast = new MyTopHeaderRenderer( defaultRenderer, true, false );
		// Top header, mid line, not bold.
		final MyTopHeaderRenderer headerRendererMid = new MyTopHeaderRenderer( defaultRenderer, false, false );

		int colIndex = 0;
		// First 2 columns.
		lastHeaderLine.add( "Label" );
		columnClasses.add( String.class );
		tableColumnModel.addColumn( new TableColumn( colIndex++ ) );
		lastHeaderLine.add( "ID" );
		columnClasses.add( Integer.class );
		tableColumnModel.addColumn( new TableColumn( colIndex++ ) );
		// Units for feature columns.
		for ( final FeatureSpec< ?, ? > fs : featureMap.keySet() )
		{
			final Feature< ? > feature = featureMap.get( fs );
			if ( null == feature.projections() )
				continue;
			final List< FeatureProjection< ? > > projections = new ArrayList<>( feature.projections() );
			projections.sort( Comparator.comparing( FeatureProjection::getKey, Comparator.comparing( FeatureProjectionKey::toString ) ) );
			for ( final FeatureProjection< ? > projection : projections )
			{
				@SuppressWarnings( "unchecked" )
				final FeatureProjection< O > fp = ( FeatureProjection< O > ) projection;
				mapToProjections.add( fp );
				mapToTooltip.add( "<html><p width=\"300\">" + fs.getInfo() + "</p></html>" );
				final String units = fp.units();
				lastHeaderLine.add( ( units == null || units.isEmpty() ) ? "" : "(" + units + ")" );
				tableColumnModel.addColumn( new TableColumn( colIndex++ ) );

				final Class< ? > pclass;
				if ( fp instanceof IntFeatureProjection )
					pclass = Integer.class;
				else
					pclass = Double.class;
				columnClasses.add( pclass );
			}
		}

		// Last line for tag columns is empty.
		for ( int tagSetID = 0; tagSetID < tagSets.size(); tagSetID++ )
		{
			final TagSet tagSet = tagSets.get( tagSetID );
			final List< Tag > tags = tagSet.getTags();
			for ( int tagID = 0; tagID < tags.size(); tagID++ )
			{
				columnClasses.add( Boolean.class );
				mapToTagIndices.add( new int[] { tagSetID, tagID } );
				lastHeaderLine.add( "" );
				tableColumnModel.addColumn( new TableColumn( colIndex++ ) );
			}
		}

		// Add feature names and feature projection names.
		int index = 2;
		for ( final FeatureSpec< ?, ? > fs : featureMap.keySet() )
		{
			final ColumnGroup featureGroup = new ColumnGroup( fs.getKey() );
			featureGroup.setHeaderRenderer( headerRenderer );
			final Feature< ? > feature = featureMap.get( fs );
			if ( null == feature.projections() )
				continue;
			final List< FeatureProjection< ? > > projections = new ArrayList<>( feature.projections() );
			if ( projections.size() == 1 )
			{
				final ColumnGroup projectionGroup = new ColumnGroup( " " );
				projectionGroup.setHeaderRenderer( headerRendererMid );
				projectionGroup.add( tableColumnModel.getColumn( index++ ) );
				featureGroup.add( projectionGroup );
			}
			else
			{
				projections.sort( Comparator.comparing( FeatureProjection::getKey, Comparator.comparing( FeatureProjectionKey::toString ) ) );

				for ( final FeatureProjection< ? > projection : projections )
				{
					final ColumnGroup projectionGroup = new ColumnGroup( projection.getKey().toString() );
					projectionGroup.setHeaderRenderer( headerRendererMid );
					projectionGroup.add( tableColumnModel.getColumn( index++ ) );
					featureGroup.add( projectionGroup );
				}
			}
			header.addColumnGroup( featureGroup );
		}

		// Add tag set names and tag names.
		for ( final TagSet tagSet : tagSets )
		{
			final ColumnGroup tagSetGroup = new ColumnGroup( tagSet.getName() );
			tagSetGroup.setHeaderRenderer( headerRenderer );
			final List< Tag > tags = tagSet.getTags();
			for ( final Tag tag : tags )
			{
				final ColumnGroup tagGroup = new ColumnGroup( tag.label() );
				tagGroup.add( tableColumnModel.getColumn( index++ ) );
				tagGroup.setHeaderRenderer( new MyTagHeaderRenderer( tag ) );
				tagSetGroup.add( tagGroup );
			}
			header.addColumnGroup( tagSetGroup );
		}

		// Pass last line to column headers and set cell renderer.
		for ( int c = 0; c < tableColumnModel.getColumnCount(); c++ )
		{
			final TableColumn column = tableColumnModel.getColumn( c );
			column.setHeaderValue( lastHeaderLine.get( c ) );
			column.setCellRenderer( cellRenderer );
		}

		table.getTableHeader().setDefaultRenderer( headerRendererLast );
		tableModel.fireTableStructureChanged();
	}

	public void setFeatures( final Map< FeatureSpec< ?, O >, Feature< O > > features )
	{
		this.featureMap.clear();
		if ( features != null )
		{
			final List< FeatureSpec< ?, O > > fss = new ArrayList<>( features.keySet() );
			fss.sort( Comparator.comparing( FeatureSpec::getKey ) );
			for ( final FeatureSpec< ?, O > fs : fss )
				this.featureMap.put( fs, features.get( fs ) );
		}
		refreshColumns();
	}

	public void setTagSets( final List< TagSet > tagSets )
	{
		this.tagSets.clear();
		if ( tagSets != null )
		{
			this.tagSets.addAll( tagSets );
			this.tagSets.sort( ( o1, o2 ) -> o1.getName().compareTo( o2.getName() ) );
		}
		refreshColumns();
	}

	public void focusObject( final O o )
	{
		this.focusRow = ( null == o ) ? -1 : getViewRowForObject( o );
		table.repaint();
	}

	public void highlightObject( final O o )
	{
		this.highlightRow = ( null == o ) ? -1 : getViewRowForObject( o );
		table.repaint();
	}

	/*
	 * INNER CLASSES
	 */

	private class MyTableToolTipProvider extends MouseMotionAdapter
	{
		private int previousCol = -1;

		@Override
		public void mouseMoved( final MouseEvent evt )
		{
			final TableColumnModel tableColumnModel = table.getColumnModel();
			final int vColIndex = tableColumnModel.getColumnIndexAtX( evt.getX() ) - 2;
			if ( vColIndex != previousCol )
			{
				if ( vColIndex >= 0 && vColIndex < mapToTooltip.size() )
				{
					table.getTableHeader().setToolTipText( mapToTooltip.get( vColIndex ) );
					previousCol = vColIndex;
				}
				else
				{
					table.getTableHeader().setToolTipText( "" );
				}
			}
		}
	}

	private class MyTableModel extends AbstractTableModel
	{

		private static final long serialVersionUID = 1L;

		private final O ref = idBimap.createRef();

		@Override
		public Class< ? > getColumnClass( final int columnIndex )
		{
			return columnClasses.get( columnIndex );
		}

		@Override
		public int getRowCount()
		{
			return doFilter ? filterIdMap.size() : idMap.size();
		}

		@Override
		public int getColumnCount()
		{
			return columnClasses.size();
		}

		@Override
		public Object getValueAt( final int rowIndex, final int columnIndex )
		{
			final int id = doFilter ? filterRowMap.get( rowIndex ) : rowMap.get( rowIndex );

			final O o = idBimap.getObjectIfExists( id, ref );
			if ( null == o )
				return null;

			if ( columnIndex == 0 )
				return labelGenerator.apply( o );
			else if ( columnIndex == 1 )
				return Integer.valueOf( idBimap.getId( o ) );
			else if ( columnIndex >= 2 && columnIndex < 2 + mapToProjections.size() )
			{
				final FeatureProjection< O > featureProjection = mapToProjections.get( columnIndex - 2 );
				if ( featureProjection.isSet( o ) )
				{
					if ( columnClasses.get( columnIndex ).equals( Integer.class ) )
						return Integer.valueOf( ( int ) featureProjection.value( o ) );
					else
						return Double.valueOf( featureProjection.value( o ) );
				}
				else
					return null;
			}
			else if ( columnIndex >= 2 + mapToProjections.size() )
			{
				final int[] ids = mapToTagIndices.get( columnIndex - ( 2 + mapToProjections.size() ) );
				final TagSet tagSet = tagSets.get( ids[ 0 ] );
				final Tag columnTag = tagSet.getTags().get( ids[ 1 ] );
				final Tag tag = tags.tags( tagSet ).get( o );
				return Boolean.valueOf( columnTag.equals( tag ) );
			}
			else
				return null;
		}

		@Override
		public void setValueAt( final Object aValue, final int rowIndex, final int columnIndex )
		{
			if ( columnIndex == 0 )
			{
				final int id = doFilter ? filterRowMap.get( rowIndex ) : rowMap.get( rowIndex );

				final O o = idBimap.getObjectIfExists( id, ref );
				if ( null == o )
					return;
				labelSetter.accept( o, ( String ) aValue );
				if ( null != undoPointMarker )
					undoPointMarker.setUndoPoint();
			}
			else if ( columnIndex >= 2 + mapToProjections.size() )
			{
				final boolean isSet = ( boolean ) aValue;
				final int id = doFilter ? filterRowMap.get( rowIndex ) : rowMap.get( rowIndex );

				final O o = idBimap.getObjectIfExists( id, ref );
				if ( null == o )
					return;
				final int[] ids = mapToTagIndices.get( columnIndex - ( 2 + mapToProjections.size() ) );
				final TagSet tagSet = tagSets.get( ids[ 0 ] );
				final Tag columnTag = tagSet.getTags().get( ids[ 1 ] );
				if ( isSet )
					tags.tags( tagSet ).set( o, columnTag );
				else
					tags.tags( tagSet ).remove( o );
				if ( null != undoPointMarker )
					undoPointMarker.setUndoPoint();

				fireTableRowsUpdated( rowIndex, rowIndex );
			}
		}
	}

	private class MyTagHeaderRenderer extends DefaultTableCellRenderer
	{

		private static final long serialVersionUID = 1L;

		public MyTagHeaderRenderer( final Tag tag )
		{
			final Color color = new Color( tag.color(), true );
			final JLabel renderer = ( JLabel ) super.getTableCellRendererComponent( table, "", false, false, 0, 0 );
			renderer.setBackground( color );
			renderer.setHorizontalTextPosition( SwingConstants.CENTER );
			renderer.setHorizontalAlignment( SwingConstants.CENTER );
		}
	}

	private class MyTopHeaderRenderer implements TableCellRenderer
	{

		private final TableCellRenderer defaultRenderer;

		private final MatteBorder border;

		private final Font normalFont = new JLabel().getFont();

		private final Font boldFont = normalFont.deriveFont( Font.BOLD );

		private final boolean bold;

		public MyTopHeaderRenderer( final TableCellRenderer defaultRenderer, final boolean bottomLine, final boolean bold )
		{
			this.defaultRenderer = defaultRenderer;
			this.bold = bold;
			this.border = bottomLine
					? BorderFactory.createMatteBorder( 0, 0, 1, 1, Color.GRAY )
					: BorderFactory.createMatteBorder( 0, 0, 0, 1, Color.GRAY );
		}

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			final JLabel renderer = ( JLabel ) defaultRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			renderer.setFont( bold ? boldFont : normalFont );
			renderer.setBorder( border );
			renderer.setBackground( HEADER_BG_COLOR );
			return renderer;
		}
	}

	private class MyTableCellRenderer extends DefaultTableCellRenderer
	{

		private final Border normalBorder;

		private final MatteBorder highlightBorderCenter;

		private final MatteBorder highlightBorderRight;

		private final MatteBorder highlightBorderLeft;

		private final Font normalFont;

		private final DecimalFormat nf;

		private final JCheckBox checkBox;

		private final O ref = idBimap.createRef();

		private static final long serialVersionUID = 1L;

		public MyTableCellRenderer()
		{
			this.normalBorder = ( ( JLabel ) super.getTableCellRendererComponent( table, "", false, false, 0, 0 ) ).getBorder();
			this.highlightBorderCenter = BorderFactory.createMatteBorder( 2, 0, 2, 0, HIGHLIGHT_BORDER_COLOR );
			this.highlightBorderLeft = BorderFactory.createMatteBorder( 2, 2, 2, 0, HIGHLIGHT_BORDER_COLOR );
			this.highlightBorderRight = BorderFactory.createMatteBorder( 2, 0, 2, 2, HIGHLIGHT_BORDER_COLOR );
			this.normalFont = table.getFont();
			this.nf = new DecimalFormat();
			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
			formatSymbols.setNaN( "NaN" );
			nf.setDecimalFormatSymbols( formatSymbols );
			this.checkBox = new JCheckBox();
			checkBox.setHorizontalAlignment( SwingConstants.CENTER );
			checkBox.setBorderPainted( true );
		}

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			final JComponent c;
			if ( value instanceof Boolean )
			{
				// Special case: boolean. We prepare the checkbox.
				final Boolean boolValue = ( Boolean ) value;
				checkBox.setSelected( boolValue.booleanValue() );
				c = checkBox;
			}
			else
			{
				c = ( JComponent ) super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			}

			c.setBorder( normalBorder );

			if ( isSelected )
			{
				c.setBackground( table.getSelectionBackground() );
				c.setForeground( table.getSelectionForeground() );
			}
			else
			{
				final O o = getObjectForViewRow( row, ref );
				final int color = coloring.color( o );
				final Color bgColor = color == 0 ? table.getBackground() : new Color( color, true );
				c.setBackground( bgColor );
				c.setForeground( textColorForBackground( bgColor ) );
			}

			if ( hasFocus )
			{
				c.setBackground( table.getSelectionBackground().darker().darker() );
				c.setForeground( table.getSelectionForeground() );
			}

			if ( row == highlightRow )
			{
				if ( column == 0 )
					c.setBorder( highlightBorderLeft );
				else if ( column == table.getColumnCount() - 1 )
					c.setBorder( highlightBorderRight );
				else
					c.setBorder( highlightBorderCenter );
			}
			c.setFont( focusRow == row ? FOCUS_FONT : normalFont );

			if ( value instanceof Double )
			{
				setHorizontalAlignment( JLabel.RIGHT );
				final Double doubleValue = ( Double ) value;
				setText( nf.format( doubleValue.doubleValue() ) );
			}
			else if ( value instanceof Number )
			{
				setHorizontalAlignment( JLabel.RIGHT );
			}
			else
			{
				setHorizontalAlignment( JLabel.CENTER );
			}

			return c;
		}
	}

	/**
	 * Starts editing the label of the object currently selected in the table.
	 * Has not effect if the table focus is not on the first column (the label
	 * column).
	 */
	public void editCurrentLabel()
	{
		final int col = table.getSelectedColumn();
		final int row = table.getSelectedRow();
		if ( col != 0 || row < 0 )
			return;
		table.editCellAt( row, col );
	}

	/**
	 * Toggles the tag that currently has the focus in the table. Does nothing
	 * if the focused cell is not a tag.
	 */
	public void toggleCurrentTag()
	{
		final int col = table.getSelectedColumn();
		final int row = table.getSelectedRow();
		if ( col < ( 2 + mapToProjections.size() ) || row < 0 )
			return;

		final O o = getObjectForViewRow( row );
		final int[] ids = mapToTagIndices.get( col - ( 2 + mapToProjections.size() ) );
		final TagSet tagSet = tagSets.get( ids[ 0 ] );
		final Tag columnTag = tagSet.getTags().get( ids[ 1 ] );
		final boolean isSet = columnTag.equals( tags.tags( tagSet ).get( o ) );
		if ( !isSet )
			tags.tags( tagSet ).set( o, columnTag );
		else
			tags.tags( tagSet ).remove( o );
		if ( null != undoPointMarker )
			undoPointMarker.setUndoPoint();

		final int modelRow = table.convertRowIndexToModel( row );
		tableModel.fireTableRowsUpdated( modelRow, modelRow );
	}

	/**
	 * Adapted from Jan Funke's code in
	 * https://github.com/saalfeldlab/bigcat/blob/janh5/src/main/java/bdv/bigcat/ui/BigCatTable.java#L112-L143
	 *
	 * @param editorComponent
	 */
	private static void blockKeys( final JComponent editorComponent, final JComponent from )
	{
		final ArrayList< KeyStroke > allTableKeys = new ArrayList<>();
		for ( Container c = from; c != null; c = c.getParent() )
		{
			if ( c instanceof JComponent )
			{
				final InputMap inputMap = ( ( JComponent ) c ).getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
				final KeyStroke[] tableKeys = inputMap.allKeys();
				if ( tableKeys != null )
					allTableKeys.addAll( Arrays.asList( tableKeys ) );
			}
		}

		final Action nada = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{}
		};
		editorComponent.getActionMap().put( "nothing", nada );

		final InputMap inputMap = editorComponent.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		for ( final KeyStroke key : allTableKeys )
			inputMap.put( key, "nothing" );
	}

	/**
	 * Returns the black color or white color depending on the specified
	 * background color, to ensure proper readability of the text on said
	 * background.
	 *
	 * @param backgroundColor
	 *            the background color.
	 * @return the black or white color.
	 */
	private static Color textColorForBackground( final Color backgroundColor )
	{
		if ( ( backgroundColor.getRed() * 0.299
				+ backgroundColor.getGreen() * 0.587
				+ backgroundColor.getBlue() * 0.114 ) > 150 )
			return Color.BLACK;
		else
			return Color.WHITE;
	}
}
