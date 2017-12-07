package org.mastodon.revised.trackscheme.display.style;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.yaml.snakeyaml.Yaml;

/**
 * Manages a collection of {@link TrackSchemeStyle}.
 * <p>
 * Has serialization / deserialization facilities and can return models based on
 * the collection it manages.
 *
 * @author Jean-Yves Tinevez
 * @author Tobias Pietzsch
 */
public class TrackSchemeStyleManager
{
	private static final String STYLE_FILE = System.getProperty( "user.home" ) + "/.mastodon/trackschemestyles.yaml";

	private final List< TrackSchemeStyle > builtinStyles;

	private final List< TrackSchemeStyle > userStyles;

	private TrackSchemeStyle defaultStyle;

	/**
	 * A {@code TrackSchemeStyle} that has the same properties as the default
	 * style. In contrast to defaultStyle this will always refer to the same
	 * object, so a trackscheme can just use this one style to listen for
	 * changes and for painting.
	 */
	private final TrackSchemeStyle forwardDefaultStyle;

	private final TrackSchemeStyle.UpdateListener updateForwardDefaultListeners;

	public TrackSchemeStyleManager()
	{
		this( true );
	}

	public TrackSchemeStyleManager( final boolean loadStyles )
	{
		builtinStyles = Collections.unmodifiableList( new ArrayList<>( TrackSchemeStyle.defaults ) );
		userStyles = new ArrayList<>();
		defaultStyle = builtinStyles.get( 0 );
		forwardDefaultStyle = TrackSchemeStyle.defaultStyle().copy();
		updateForwardDefaultListeners = () -> forwardDefaultStyle.set( defaultStyle );
		defaultStyle.addUpdateListener( updateForwardDefaultListeners );
		if ( loadStyles )
			loadStyles();
	}

	public synchronized void setDefaultStyle( final TrackSchemeStyle style )
	{
		defaultStyle.removeUpdateListener( updateForwardDefaultListeners );
		defaultStyle = style;
		forwardDefaultStyle.set( defaultStyle );
		defaultStyle.addUpdateListener( updateForwardDefaultListeners );
	}

	public synchronized void remove( final TrackSchemeStyle style )
	{
		if ( defaultStyle.equals( style ) )
			setDefaultStyle( builtinStyles.get( 0 ) );
		userStyles.remove( style );
	}

	public synchronized void rename( final TrackSchemeStyle style, final String newName )
	{
		if ( style.getName().equals( newName ) )
			return;

		if ( nameExists( newName ) )
			throw new IllegalArgumentException( "TrackSchemeStyle \"" + newName + "\" already exists.");

		style.name( newName );
	}

	/**
	 * Returns a copy of the specified {@link TrackSchemeStyle}, making sure that
	 * the copy receives a name not already present in this manager's list of
	 * {@link TrackSchemeStyle}.
	 *
	 * @param style
	 *            the {@link TrackSchemeStyle} to copy.
	 * @return a new {@link TrackSchemeStyle}
	 */
	public synchronized TrackSchemeStyle duplicate( final TrackSchemeStyle style )
	{
		final String name = style.getName();
		final Pattern pattern = Pattern.compile( "(.+) \\((\\d+)\\)$" );
		final Matcher matcher = pattern.matcher( name );
		int n;
		String prefix;
		if ( matcher.matches() )
		{
			final String nstr = matcher.group( 2 );
			n = Integer.parseInt( nstr );
			prefix = matcher.group( 1 );
		}
		else
		{
			n = 1;
			prefix = name;
		}

		String newName;
		do
			newName = prefix + " (" + ( ++n ) + ")";
		while ( nameExists( newName ) );

		final TrackSchemeStyle newStyle = style.copy( newName );
		userStyles.add( newStyle );
		return newStyle;
	}

	public List< TrackSchemeStyle > getBuiltinStyles()
	{
		return builtinStyles;
	}

	public List< TrackSchemeStyle > getUserStyles()
	{
		return Collections.unmodifiableList( userStyles );
	}

	public TrackSchemeStyle getDefaultStyle()
	{
		return defaultStyle;
	}

	/**
	 * Returns a final {@link TrackSchemeStyle} instance that always has the
	 * same properties as the default style.
	 */
	public TrackSchemeStyle getForwardDefaultStyle()
	{
		return forwardDefaultStyle;
	}

	private boolean nameExists( final String name )
	{
		return styleForName( name ).isPresent();
	}

	private Optional< TrackSchemeStyle > styleForName( final String name )
	{
		return Stream.concat( builtinStyles.stream(), userStyles.stream() ).filter( style -> style.getName().equals( name ) ).findFirst();
	}

	public void loadStyles()
	{
		loadStyles( STYLE_FILE );
	}

	public void loadStyles( final String filename )
	{
		userStyles.clear();
		final Set< String > names = builtinStyles.stream().map( TrackSchemeStyle::getName ).collect( Collectors.toSet() );
		try
		{
			final FileReader input = new FileReader( filename );
			final Yaml yaml = TrackSchemeStyleIO.createYaml();
			final Iterable< Object > objs = yaml.loadAll( input );
			String defaultStyleName = null;
			for ( final Object obj : objs )
			{
				if ( obj instanceof String )
				{
					defaultStyleName = ( String ) obj;
					System.out.println( "TrackSchemeStyleManager.loadStyles" );
					System.out.println( defaultStyleName );
				}
				else if ( obj instanceof TrackSchemeStyle )
				{
					final TrackSchemeStyle ts = ( TrackSchemeStyle ) obj;
					if ( null != ts )
					{
						// sanity check: style names must be unique
						if ( names.add( ts.getName() ) )
							userStyles.add( ts );
						else
							System.out.println( "Discarded style with duplicate name \"" + ts.getName() + "\"." );
					}
				}
			}
			setDefaultStyle( styleForName( defaultStyleName ).orElseGet( () -> builtinStyles.get( 0 ) ) );
		}
		catch ( final FileNotFoundException e )
		{
			System.out.println( "TrackScheme style file " + filename + " not found. Using builtin styles." );
		}
	}

	public void saveStyles()
	{
		saveStyles( STYLE_FILE );
	}

	public void saveStyles( final String filename )
	{
		try
		{
			mkdirs( filename );
			final FileWriter output = new FileWriter( filename );
			final Yaml yaml = TrackSchemeStyleIO.createYaml();
			final ArrayList< Object > objects = new ArrayList<>();
			objects.add( defaultStyle.getName() );
			objects.addAll( userStyles );
			yaml.dumpAll( objects.iterator(), output );
			output.close();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	public void set( final TrackSchemeStyleManager other )
	{
		setSnapshot( other.getSnapshot() );
	}

	static class Snapshot
	{
		private final List< TrackSchemeStyle > userStyles;

		private final String defaultStyleName;

		public Snapshot( final TrackSchemeStyleManager manager )
		{
			this.userStyles = manager.getUserStyles().stream().map( s -> s.copy() ).collect( Collectors.toList() );
			this.defaultStyleName = manager.getDefaultStyle().getName();
		}
	}

	Snapshot getSnapshot()
	{
		return new Snapshot( this );
	}

	void setSnapshot( final Snapshot snapshot )
	{
		userStyles.clear();
		snapshot.userStyles.forEach( s -> userStyles.add( s.copy() ) );
		setDefaultStyle( styleForName( snapshot.defaultStyleName ).orElseGet( () -> builtinStyles.get( 0 ) ) );
	}

	/*
	 * STATIC UTILITIES
	 */

	private static boolean mkdirs( final String fileName )
	{
		final File dir = new File( fileName ).getParentFile();
		return dir == null ? false : dir.mkdirs();
	}
}
