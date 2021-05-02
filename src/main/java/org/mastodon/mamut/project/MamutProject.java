package org.mastodon.mamut.project;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MamutProject
{
	/**
	 * The project folder or {@code .mastodon} file.
	 */
	private File projectRoot;

	/**
	 * The BDV xml file.
	 */
	private File datasetXmlFile;

	private boolean isDatasetXmlPathRelative;

	/**
	 * The space units (e.g. µm, mm, ...).
	 */
	private String spaceUnits;

	/**
	 * The time units (e.g. frame, s, ...).
	 */
	private String timeUnits;

	static final String PROJECT_FILE_NAME = "project.xml";

	static final String RAW_MODEL_FILE_NAME = "model.raw";

	static final String RAW_TAGS_FILE_NAME = "tags.raw";

	static final String FEATURE_FOLDER_NAME = "features";

	static final String GUI_FILE_NAME = "gui.xml";

	public MamutProject( final String projectRoot )
	{
		this( new File( projectRoot ), null );
	}

	public MamutProject( final File projectRoot )
	{
		this( projectRoot, null );
	}

	public MamutProject(
			final File projectRoot,
			final File datasetXmlFile )
	{
		this.projectRoot = projectRoot;
		this.datasetXmlFile = datasetXmlFile;
		this.isDatasetXmlPathRelative = true;
	}

	/**
	 * Get the project folder or zip file.
	 *
	 * @return the project folder or zip file.
	 */
	public File getProjectRoot()
	{
		return projectRoot;
	}

	public void setProjectRoot( final File projectRoot )
	{
		this.projectRoot = projectRoot;
	}

	public File getDatasetXmlFile()
	{
		return datasetXmlFile;
	}

	public void setDatasetXmlFile( final File datasetXmlFile )
	{
		this.datasetXmlFile = datasetXmlFile;
	}

	public boolean isDatasetXmlPathRelative()
	{
		return isDatasetXmlPathRelative;
	}

	public void setDatasetXmlPathRelative( final boolean relative )
	{
		isDatasetXmlPathRelative = relative;
	}

	public String getSpaceUnits()
	{
		return spaceUnits;
	}

	public void setSpaceUnits( final String spaceUnits )
	{
		this.spaceUnits = spaceUnits;
	}

	public String getTimeUnits()
	{
		return timeUnits;
	}

	public void setTimeUnits( final String timeUnits )
	{
		this.timeUnits = timeUnits;
	}

	@Override
	public String toString()
	{
		return super.toString() + "\n"
				+ " - projectRoot: " + getProjectRoot() + "\n"
				+ " - dataset: " + getDatasetXmlFile();
	}

	public ProjectReader openForReading() throws IOException
	{
		return projectRoot.isDirectory()
				? new ReadFromDirectory()
				: new ReadFromZip();
	}

	public ProjectWriter openForWriting() throws IOException
	{
		return projectRoot.isDirectory()
				? new WriteToDirectory()
				: new WriteToZip();
	}

	public interface ProjectReader extends Closeable
	{
		InputStream getProjectXmlInputStream() throws IOException;

		InputStream getRawModelInputStream() throws IOException;

		InputStream getRawTagsInputStream() throws IOException;

		InputStream getFeatureInputStream( String featureKey ) throws IOException;

		/**
		 * Returns the collection of feature keys that are stored in this
		 * project.
		 *
		 * @return the collection of feature keys.
		 */
		Collection< String > getFeatureKeys();
		
		InputStream getGuiInputStream() throws IOException;
	}

	public interface ProjectWriter extends Closeable
	{
		OutputStream getProjectXmlOutputStream() throws IOException;

		OutputStream getRawModelOutputStream() throws IOException;

		OutputStream getRawTagsOutputStream() throws IOException;

		OutputStream getFeatureOutputStream( String featureKey ) throws IOException;

		OutputStream getGuiOutputStream() throws IOException;
	}

	private class ReadFromDirectory implements ProjectReader
	{
		@Override
		public InputStream getProjectXmlInputStream() throws FileNotFoundException
		{
			return new FileInputStream( new File( projectRoot, PROJECT_FILE_NAME ) );
		}

		@Override
		public InputStream getRawModelInputStream() throws FileNotFoundException
		{
			return new FileInputStream( new File( projectRoot, RAW_MODEL_FILE_NAME ) );
		}

		@Override
		public InputStream getRawTagsInputStream() throws FileNotFoundException
		{
			return new FileInputStream( new File( projectRoot, RAW_TAGS_FILE_NAME ) );
		}

		@Override
		public InputStream getFeatureInputStream( final String featureKey ) throws IOException
		{
			final File featureFolder = new File( projectRoot, FEATURE_FOLDER_NAME );
			return new FileInputStream( new File( featureFolder, featureKey + ".raw" ) );
		}

		@Override
		public Collection< String > getFeatureKeys()
		{
			final File featureFolder = new File( projectRoot, FEATURE_FOLDER_NAME );
			if ( !featureFolder.exists() || !featureFolder.canRead() )
				return Collections.emptyList();

			final List< String > featureKeys = Arrays.stream( featureFolder.listFiles( ( dir, name ) -> name.toLowerCase().endsWith( ".raw" ) ) )
				.map( f -> f.getName() )
				.map( s -> s.replace( ".raw", "" ) )
				.collect( Collectors.toList() );
			return featureKeys;
		}

		@Override
		public InputStream getGuiInputStream() throws IOException
		{
			return new FileInputStream( new File( projectRoot, GUI_FILE_NAME ) );
		}

		@Override
		public void close()
		{}
	}

	private class ReadFromZip implements ProjectReader
	{
		private final ReadZip zip;

		ReadFromZip() throws IOException
		{
			zip = new ReadZip( projectRoot );
		}

		@Override
		public InputStream getProjectXmlInputStream() throws IOException
		{
			return zip.getInputStream( PROJECT_FILE_NAME );
		}

		@Override
		public InputStream getRawModelInputStream() throws IOException
		{
			return zip.getInputStream( RAW_MODEL_FILE_NAME );
		}

		@Override
		public InputStream getRawTagsInputStream() throws IOException
		{
			return zip.getInputStream( RAW_TAGS_FILE_NAME );
		}

		@Override
		public InputStream getFeatureInputStream( final String featureKey ) throws IOException
		{
			return zip.getInputStream( FEATURE_FOLDER_NAME + "/" + featureKey + ".raw" );
		}

		@Override
		public Collection< String > getFeatureKeys()
		{
			return zip.listFile( FEATURE_FOLDER_NAME ).stream()
				.map( s -> s.replace( ".raw", "" ) )
				.collect( Collectors.toList() );
		}

		@Override
		public InputStream getGuiInputStream() throws IOException
		{
			return zip.getInputStream( GUI_FILE_NAME );
		}

		@Override
		public void close() throws IOException
		{
			zip.close();
		}
	}

	private class WriteToDirectory implements ProjectWriter
	{

		@Override
		public OutputStream getProjectXmlOutputStream() throws FileNotFoundException
		{
			return new FileOutputStream( new File( projectRoot, PROJECT_FILE_NAME ) );
		}

		@Override
		public OutputStream getRawModelOutputStream() throws FileNotFoundException
		{
			return new FileOutputStream( new File( projectRoot, RAW_MODEL_FILE_NAME ) );
		}

		@Override
		public OutputStream getRawTagsOutputStream() throws FileNotFoundException
		{
			return new FileOutputStream( new File( projectRoot, RAW_TAGS_FILE_NAME ) );
		}

		@Override
		public OutputStream getFeatureOutputStream( final String featureKey ) throws IOException
		{
			final File featureFolder = new File( projectRoot, FEATURE_FOLDER_NAME );
			if ( !featureFolder.exists() )
				featureFolder.mkdir();
			return new FileOutputStream( new File( featureFolder, featureKey + ".raw" ) );
		}

		@Override
		public OutputStream getGuiOutputStream() throws IOException
		{
			return new FileOutputStream( new File( projectRoot, GUI_FILE_NAME ) );
		}

		@Override
		public void close() throws IOException
		{}
	}

	private class WriteToZip implements ProjectWriter
	{
		private final WriteZip zip;

		WriteToZip() throws IOException
		{
			zip = new WriteZip( projectRoot );
		}

		@Override
		public OutputStream getProjectXmlOutputStream() throws IOException
		{
			return zip.getOutputStream( PROJECT_FILE_NAME );
		}

		@Override
		public OutputStream getRawModelOutputStream() throws IOException
		{
			return zip.getOutputStream( RAW_MODEL_FILE_NAME );
		}

		@Override
		public OutputStream getRawTagsOutputStream() throws IOException
		{
			return zip.getOutputStream( RAW_TAGS_FILE_NAME );
		}

		@Override
		public OutputStream getFeatureOutputStream( final String featureKey ) throws IOException
		{
			return zip.getOutputStream( FEATURE_FOLDER_NAME + "/" + featureKey + ".raw" );
		}

		@Override
		public OutputStream getGuiOutputStream() throws IOException
		{
			return zip.getOutputStream( GUI_FILE_NAME );
		}

		@Override
		public void close() throws IOException
		{
			zip.close();
		}
	}
}
