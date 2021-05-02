package org.mastodon.model.feature.ui;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mastodon.feature.Feature;
import org.mastodon.feature.FeatureModel;
import org.mastodon.feature.FeatureSpec;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.MamutViewTable;
import org.mastodon.mamut.WindowManager;
import org.mastodon.mamut.feature.MamutFeatureComputerService;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.project.MamutProject;
import org.mastodon.mamut.project.MamutProjectIO;
import org.mastodon.model.tag.TagSetStructure;
import org.mastodon.ui.ProgressListener;
import org.scijava.Context;

import mpicbg.spim.data.SpimDataException;

public class FeatureTagTableExample
{

	public static void main( final String[] args ) throws IOException, SpimDataException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		Locale.setDefault( Locale.ROOT );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		final Context context = new Context();

		final WindowManager windowManager = new WindowManager( context );
		final String projectFile = "samples/drosophila_crop.mastodon";
		final MamutProject project = new MamutProjectIO().load( projectFile );
		windowManager.getProjectManager().open( project );

		final MamutAppModel appModel = windowManager.getAppModel();
		final Model model = windowManager.getAppModel().getModel();
		final MamutFeatureComputerService computerService = context.getService( MamutFeatureComputerService.class );
		computerService.setSharedBdvData( windowManager.getAppModel().getSharedBdvData() );
		final Set< FeatureSpec< ?, ? > > featureSpecs = computerService.getFeatureSpecs();
		final Map< FeatureSpec< ?, ? >, Feature< ? > > map = computerService.compute( featureSpecs );

		final FeatureModel featureModel = model.getFeatureModel();
		featureModel.pauseListeners();
		for ( final Feature< ? > feature : map.values() )
			featureModel.declareFeature(  feature );
		featureModel.resumeListeners();

		final TagSetStructure tss = new TagSetStructure();
		final Random ran = new Random( 0l );
		final TagSetStructure.TagSet reviewedByTag = tss.createTagSet( "Reviewed by" );
		reviewedByTag.createTag( "Pavel", ran.nextInt() | 0xFF000000 );
		reviewedByTag.createTag( "Mette", ran.nextInt() | 0xFF000000 );
		reviewedByTag.createTag( "Tobias", ran.nextInt() | 0xFF000000 );
		reviewedByTag.createTag( "JY", ran.nextInt() | 0xFF000000 );
		final TagSetStructure.TagSet locationTag = tss.createTagSet( "Location" );
		locationTag.createTag( "Anterior", ran.nextInt() | 0xFF000000 );
		locationTag.createTag( "Posterior", ran.nextInt() | 0xFF000000 );
		model.getTagSetModel().setTagSetStructure( tss );

		final MamutViewTable view = new MamutViewTable( appModel );
		view.getFrame().setMirrorSelection( true );
		final ModelGraph graph = appModel.getModel().getGraph();
		view.getFrame().getVertexTable().setRows( graph.vertices() );
		view.getFrame().getEdgeTable().setRows( graph.edges() );
		view.getFrame().setSize( 400, 400 );
		view.getFrame().setVisible( true );
	}

	public static ProgressListener voidLogger()
	{
		return new ProgressListener()
		{

			@Override
			public void showStatus( final String string )
			{}

			@Override
			public void showProgress( final int current, final int total )
			{}

			@Override
			public void clearStatus()
			{}
		};
	}
}
