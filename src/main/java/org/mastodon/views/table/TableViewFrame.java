package org.mastodon.views.table;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.mastodon.RefPool;
import org.mastodon.app.MastodonAppModel;
import org.mastodon.app.ViewGraph;
import org.mastodon.app.ui.GroupLocksPanel;
import org.mastodon.app.ui.ViewFrame;
import org.mastodon.collection.RefSet;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.collection.ref.RefSetImp;
import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureModel.FeatureModelListener;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.ref.AbstractListenableEdge;
import org.mastodon.grouping.GroupHandle;
import org.mastodon.model.AbstractSpot;
import org.mastodon.model.FocusListener;
import org.mastodon.model.FocusModel;
import org.mastodon.model.HighlightListener;
import org.mastodon.model.HighlightModel;
import org.mastodon.model.NavigationHandler;
import org.mastodon.model.NavigationListener;
import org.mastodon.model.SelectionListener;
import org.mastodon.model.SelectionModel;
import org.mastodon.model.tag.ObjTags;
import org.mastodon.model.tag.TagSetModel;
import org.mastodon.model.tag.TagSetStructure.TagSet;
import org.mastodon.ui.coloring.ColorGenerator;
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.ui.context.ContextChooserPanel;
import org.mastodon.undo.UndoPointMarker;
import org.mastodon.views.context.Context;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.context.ContextListener;
import org.scijava.ui.behaviour.util.InputActionBindings;

public class TableViewFrame<
		M extends MastodonAppModel< ?, V, E >,
		VG extends ViewGraph< V, E, V, E >,
		V extends AbstractSpot< V, E, ?, ?, ? >,
		E extends AbstractListenableEdge< E, V, ?, ? > >
	extends ViewFrame
	implements
		SelectionListener,
		FocusListener,
		HighlightListener,
		TagSetModel.TagSetModelListener,
		FeatureModelListener,
		ContextListener< V >
{

	private static final long serialVersionUID = 1L;

	private final SelectionModel< V, E > selectionModel;

	private final FocusModel< V, E > focusModel;

	private final HighlightModel< V, E > highlightModel;

	private final FeatureModel featureModel;

	private final TagSetModel< V, E > tagSetModel;

	private final FeatureTagTablePanel< V > vertexTable;

	private final FeatureTagTablePanel< E > edgeTable;

	private boolean ignoreTableSelectionChange = false;

	private boolean ignoreSelectionChange = false;

	private final V vref;

	private final E eref;

	/**
	 * If <code>true</code>, the selection in the {@link JTable}s will be used
	 * to update the {@link SelectionModel} of this view.
	 */
	private boolean mirrorSelection = false;

	private final JTabbedPane pane;

	private final RefArrayList< V > filterByVertices;

	private final RefSetImp< E > filterByEdges;

	private final ContextChooser< V > contextChooser;

	public TableViewFrame(
			final M appModel,
			final VG viewGraph,
			final FeatureModel featureModel,
			final TagSetModel< V, E > tagSetModel,
			final Function< V, String > vertexLabelGenerator,
			final Function< E, String > edgeLabelGenerator,
			final BiConsumer< V, String > vertexLabelSetter,
			final BiConsumer< E, String > edgeLabelSetter,
			final GroupHandle groupHandle,
			final NavigationHandler< V, E > navigationHandler,
			final UndoPointMarker undoPointMarker,
			final GraphColorGenerator< V, E > coloring )
	{
		super( "Feature and tag table" );
		this.featureModel = featureModel;
		this.tagSetModel = tagSetModel;
		this.selectionModel = appModel.getSelectionModel();
		this.focusModel = appModel.getFocusModel();
		this.highlightModel = appModel.getHighlightModel();
		final GraphIdBimap< V, E > graphIdBimap = appModel.getModel().getGraphIdBimap();
		this.vref = graphIdBimap.vertexIdBimap().createRef();
		this.eref = graphIdBimap.edgeIdBimap().createRef();
		final List< TagSet > tagSets = tagSetModel.getTagSetStructure().getTagSets();
		this.filterByVertices = new RefArrayList<>( graphIdBimap.vertexIdBimap() );
		this.filterByEdges = new RefSetImp<>( graphIdBimap.edgeIdBimap() );

		/*
		 * Settings panel.
		 */

		final GroupLocksPanel navigationLocksPanel = new GroupLocksPanel( groupHandle );
		settingsPanel.add( navigationLocksPanel );
		settingsPanel.add( Box.createHorizontalGlue() );

		this.contextChooser = new ContextChooser<>( this );
		final ContextChooserPanel< ? > contextChooserPanel = new ContextChooserPanel<>( contextChooser );
		settingsPanel.add( contextChooserPanel );
		add( settingsPanel, BorderLayout.NORTH );

		/*
		 * Vertices
		 */

		final ObjTags< V > vertexTags = tagSetModel.getVertexTags();
		final RefPool< V > vertexIdBimap = graphIdBimap.vertexIdBimap();
		vertexTable = new FeatureTagTablePanel<>(
				vertexTags,
				vertexIdBimap,
				vertexLabelGenerator,
				vertexLabelSetter,
				undoPointMarker,
				( v ) -> coloring.color( v ) );
		final JTable vt = vertexTable.getTable();

		vt.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{

			private final V ref = viewGraph.vertexRef();

			@Override
			public void valueChanged( final ListSelectionEvent event )
			{
				if ( event.getValueIsAdjusting() || ignoreTableSelectionChange )
					return;
				ignoreSelectionChange = true;

				// Selection.
				if ( mirrorSelection )
				{
					selectionModel.pauseListeners();
					final RefSet< E > selectedEdges = selectionModel.getSelectedEdges();
					selectionModel.clearSelection();
					final int[] selectedRows = vt.getSelectedRows();
					for ( final int row : selectedRows )
						selectionModel.setSelected( vertexTable.getObjectForViewRow( row, ref ), true );
					for ( final E e : selectedEdges )
						selectionModel.setSelected( e, true );
					selectionModel.resumeListeners();
				}

				// Navigate and focus.
				final int rowIndex = vt.getSelectionModel().getLeadSelectionIndex();
				final V v = vertexTable.getObjectForViewRow( rowIndex, ref );
				if ( null != v )
				{
					navigationHandler.notifyNavigateToVertex( v );
					focusModel.focusVertex( v );
				}

				ignoreSelectionChange = false;
			}
		} );

		@SuppressWarnings( "unchecked" )
		final Class< V > vertexClass = ( Class< V > ) viewGraph.vertexRef().getClass();
		final Map< FeatureSpec< ?, V >, Feature< V > > vertexFeatures = collectFeatureMap( featureModel, vertexClass );
		vertexTable.setFeatures( vertexFeatures );
		vertexTable.setTagSets( tagSets );

		/*
		 * Edges
		 */

		final ObjTags< E > edgeTags = tagSetModel.getEdgeTags();
		final RefPool< E > edgeIdBimap = graphIdBimap.edgeIdBimap();

		final ColorGenerator< E > edgeColorGenerator = new ColorGenerator< E >()
		{

			private final V vTmpS = graphIdBimap.vertexIdBimap().createRef();

			private final V vTmpT = graphIdBimap.vertexIdBimap().createRef();

			@Override
			public int color( final E edge )
			{
				edge.getTarget( vTmpT );
				edge.getSource( vTmpS );
				return coloring.color( edge, vTmpS, vTmpT );
			}
		};

		edgeTable = new FeatureTagTablePanel<>(
				edgeTags,
				edgeIdBimap,
				edgeLabelGenerator,
				edgeLabelSetter,
				undoPointMarker,
				edgeColorGenerator );
		final JTable et = edgeTable.getTable();
		et.getSelectionModel().addListSelectionListener( new ListSelectionListener()
		{

			private final E eref = viewGraph.edgeRef();

			private final V vref = viewGraph.vertexRef();

			@Override
			public void valueChanged( final ListSelectionEvent event )
			{
				if ( event.getValueIsAdjusting() || ignoreTableSelectionChange )
					return;
				ignoreSelectionChange = true;

				// Selection.
				if ( mirrorSelection )
				{
					selectionModel.pauseListeners();
					final RefSet< V > selectedVertices = selectionModel.getSelectedVertices();
					selectionModel.clearSelection();
					final int[] selectedRows = et.getSelectedRows();
					for ( final int row : selectedRows )
						selectionModel.setSelected( edgeTable.getObjectForViewRow( row, eref ), true );
					for ( final V v : selectedVertices )
						selectionModel.setSelected( v, true );
					selectionModel.resumeListeners();
				}

				// Navigate and focus.
				final int rowIndex = et.getSelectionModel().getLeadSelectionIndex();
				final E e = edgeTable.getObjectForViewRow( rowIndex, eref );
				if ( null != e )
				{
					navigationHandler.notifyNavigateToEdge( e );
					focusModel.focusVertex( e.getTarget( vref ) );
				}

				ignoreSelectionChange = false;
			}
		} );

		@SuppressWarnings( "unchecked" )
		final Class< E > edgeClass = ( Class< E > ) viewGraph.edgeRef().getClass();
		final Map< FeatureSpec< ?, E >, Feature< E > > edgeFeatures = collectFeatureMap( featureModel, edgeClass);
		edgeTable.setFeatures( edgeFeatures );
		edgeTable.setTagSets( tagSets );

		navigationHandler.listeners().add( new NavigationListener< V, E >()
		{

			@Override
			public void navigateToVertex( final V vertex )
			{
				vertexTable.scrollToObject( vertex );
			}

			@Override
			public void navigateToEdge( final E edge )
			{
				edgeTable.scrollToObject( edge );
			}

		} );

		/*
		 * Tab pane.
		 */

		pane = new JTabbedPane( JTabbedPane.LEFT );
		pane.add( "Vertices", vertexTable );
		pane.add( "Edges", edgeTable );
		add( pane, BorderLayout.CENTER );
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}

	/**
	 * Exposes the vertex table of this view.
	 *
	 * @return the vertex table.
	 */
	public FeatureTagTablePanel< V > getVertexTable()
	{
		return vertexTable;
	}

	/**
	 * Exposes the edge table of this view.
	 *
	 * @return the edge table.
	 */
	public FeatureTagTablePanel< E > getEdgeTable()
	{
		return edgeTable;
	}

	/**
	 * Sets the selection mirroring settings. If <code>true</code>, the
	 * selection in the {@link JTable}s will be used to update the
	 * {@link SelectionModel} of this view.
	 *
	 * @param mirrorSelection
	 *            whether the selection in the table should be mirrored on the
	 *            {@link SelectionModel}.
	 */
	public void setMirrorSelection( final boolean mirrorSelection )
	{
		this.mirrorSelection = mirrorSelection;
	}

	@Override
	public void selectionChanged()
	{
		if ( ignoreSelectionChange )
			return;
		ignoreTableSelectionChange = true;

		// Vertices table.
		final RefSet< V > selectedVertices = selectionModel.getSelectedVertices();
		final JTable vt = vertexTable.getTable();
		vt.getSelectionModel().clearSelection();
		for ( final V v : selectedVertices )
		{
			final int row = vertexTable.getViewRowForObject( v );
			vt.getSelectionModel().addSelectionInterval( row, row );
		}

		// Edges table.
		final RefSet< E > selectedEdges = selectionModel.getSelectedEdges();
		final JTable et = edgeTable.getTable();
		et.getSelectionModel().clearSelection();
		for ( final E e : selectedEdges )
		{
			final int row = edgeTable.getViewRowForObject( e );
			et.getSelectionModel().addSelectionInterval( row, row );
		}
		ignoreTableSelectionChange = false;
	}

	@Override
	public void focusChanged()
	{
		final V focusedVertex = focusModel.getFocusedVertex( vref );
		vertexTable.focusObject( focusedVertex );
	}

	@Override
	public void highlightChanged()
	{
		final V highlightedVertex = highlightModel.getHighlightedVertex( vref );
		vertexTable.highlightObject( highlightedVertex );
		final E highlightedEdge = highlightModel.getHighlightedEdge( eref );
		edgeTable.highlightObject( highlightedEdge );
	}

	@Override
	public void featureModelChanged()
	{
		ignoreTableSelectionChange = true;
		final Map< ?, ? > map1 = collectFeatureMap( featureModel, vref.getClass() );
		@SuppressWarnings( "unchecked" )
		final Map< FeatureSpec< ?, V >, Feature< V > > vertexFeatures =  ( Map< FeatureSpec< ?, V >, Feature< V > > ) map1;
		vertexTable.setFeatures( vertexFeatures );
		final Map< ?, ? > map2 = collectFeatureMap( featureModel, eref.getClass() );
		@SuppressWarnings( "unchecked" )
		final Map< FeatureSpec< ?, E >, Feature< E > >  edgeFeatures = ( Map< FeatureSpec< ?, E >, Feature< E > > ) map2;
		edgeTable.setFeatures( edgeFeatures );
		if ( mirrorSelection )
			selectionChanged();
		ignoreTableSelectionChange = false;
	}

	@Override
	public void tagSetStructureChanged()
	{
		ignoreTableSelectionChange = true;
		final List< TagSet > tagSets = tagSetModel.getTagSetStructure().getTagSets();
		vertexTable.setTagSets( tagSets );
		edgeTable.setTagSets( tagSets );
		if ( mirrorSelection )
			selectionChanged();
		ignoreTableSelectionChange = false;
	}

	private FeatureTagTablePanel< ? > getCurrentlyDisplayedTable()
	{
		final int selectedIndex = pane.getSelectedIndex();
		return selectedIndex == 0 ? vertexTable : edgeTable;
	}

	public void editCurrentLabel()
	{
		getCurrentlyDisplayedTable().editCurrentLabel();
	}

	public void toggleTag()
	{
		getCurrentlyDisplayedTable().toggleCurrentTag();
	}

	@Override
	public void contextChanged( final Context< V > context )
	{
		ignoreTableSelectionChange = true;
		if ( null == context )
		{
			vertexTable.filter( null  );
			edgeTable.filter( null );
		}
		else
		{
			filterByVertices.clear();
			filterByEdges.clear();
			final Iterable< V > insideVertices = context.getInsideVertices( context.getTimepoint() );
			for ( final V v : insideVertices )
			{
				filterByVertices.add( v );
				for ( final E e : v.edges() )
					filterByEdges.add( e );
			}
			vertexTable.filter( filterByVertices );
			edgeTable.filter( filterByEdges );
		}
		if ( mirrorSelection )
			selectionChanged();
		ignoreTableSelectionChange = false;
	}

	public ContextChooser< V > getContextChooser()
	{
		return contextChooser;
	}

	private static final < O > Map< FeatureSpec< ?, O >, Feature< O > > collectFeatureMap( final FeatureModel featureModel, final Class< O > clazz )
	{
		final Set< FeatureSpec< ?, ? > > featureSpecs = featureModel.getFeatureSpecs().stream()
				.filter( ( fs ) -> fs.getTargetClass().isAssignableFrom( clazz ) )
				.collect( Collectors.toSet() );
		final Map< FeatureSpec< ?, O >, Feature< O > > featureMap = new HashMap<>();
		for ( final FeatureSpec< ?, ? > fs : featureSpecs )
		{
			@SuppressWarnings( "unchecked" )
			final Feature< O > feature = ( Feature< O > ) featureModel.getFeature( fs );
			@SuppressWarnings( "unchecked" )
			final FeatureSpec< ?, O > featureSpec = ( FeatureSpec< ?, O > ) fs;
			featureMap.put( featureSpec, feature );
		}
		return featureMap;
	}
}
