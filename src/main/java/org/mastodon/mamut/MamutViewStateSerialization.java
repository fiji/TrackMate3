package org.mastodon.mamut;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom2.Element;
import org.mastodon.app.ui.MastodonFrameView;
import org.mastodon.mamut.model.Spot;
import org.mastodon.ui.coloring.ColoringModel;
import org.mastodon.views.bdv.ViewerPanelMamut;
import org.mastodon.views.context.ContextChooser;
import org.mastodon.views.context.ContextProvider;
import org.mastodon.views.trackscheme.ScreenTransform;
import org.mastodon.views.trackscheme.display.TrackSchemePanel;

import bdv.viewer.ViewerState;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Collection of constants and utilities related to de/serializing a GUI state.
 */
class MamutViewStateSerialization
{

	static final String WINDOW_TAG = "Window";

	/**
	 * Key to the view type name. Value is a string.
	 */
	static final String VIEW_TYPE_KEY = "Type";

	/**
	 * Key to the parameter that stores the frame position for
	 * {@link MastodonFrameView}s. Value is and <code>int[]</code> array of 4
	 * elements: x, y, width and height.
	 */
	static final String FRAME_POSITION_KEY = "FramePosition";

	/**
	 * Key that specifies whether the settings panel is visible or not.
	 */
	static final String SETTINGS_PANEL_VISIBLE_KEY = "SettingsPanelVisible";

	/**
	 * Key to the lock group id. Value is an int.
	 */
	static final String GROUP_HANDLE_ID_KEY = "LockGroupId";

	/**
	 * Key for the {@link ViewerState} in a BDV view. Value is a XML
	 * {@link Element} serialized from the state.
	 *
	 * @see ViewerPanelMamut#stateToXml()
	 * @see ViewerPanelMamut#stateFromXml(Element)
	 */
	static final String BDV_STATE_KEY = "BdvState";

	/**
	 * Key for the transform in a BDV view. Value is an
	 * {@link AffineTransform3D} instance.
	 */
	static final String BDV_TRANSFORM_KEY = "BdvTransform";

	/**
	 * Key for the transform in a TrackScheme view. Value is a
	 * {@link ScreenTransform} instance.
	 */
	static final String TRACKSCHEME_TRANSFORM_KEY = "TrackSchemeTransform";

	/**
	 * Key that specifies whether we do not use a special coloring scheme on the
	 * view. If <code>true</code>, then we do not use a special coloring scheme.
	 *
	 * @see #TAG_SET_KEY
	 * @see #FEATURE_COLOR_MODE_KEY
	 */
	static final String NO_COLORING_KEY = "NoColoring";

	/**
	 * Key that specifies the name of the tag-set to use for coloring scheme
	 * based on tag-sets. A non-<code>null</code> value means the coloring
	 * scheme is based on tag-sets.
	 *
	 * @see #NO_COLORING_KEY
	 * @see #FEATURE_COLOR_MODE_KEY
	 */
	static final String TAG_SET_KEY = "TagSet";

	/**
	 * Key that specifies the name of the feature color mode to use for coloring
	 * scheme based on feature color modes. A non-<code>null</code> value means
	 * the coloring scheme is based on feature values.
	 *
	 * @see #NO_COLORING_KEY
	 * @see #TAG_SET_KEY
	 */
	static final String FEATURE_COLOR_MODE_KEY = "FeatureColorMode";

	/**
	 * Key that specifies the name of the chosen context provider. Values are
	 * strings.
	 */
	static final String CHOSEN_CONTEXT_PROVIDER_KEY = "ContextProvider";

	/**
	 * Serializes a GUI state map into a XML element.
	 *
	 * @param guiState
	 *            the GUI state to serialize.
	 * @return a new XML element.
	 */
	static Element toXml( final MamutView< ?, ?, ? > view )
	{
		final Map< String, Object > guiState = getGuiState( view );

		final Element element = new Element( WINDOW_TAG );
		for ( final Entry< String, Object > entry : guiState.entrySet() )
		{
			final Object value = entry.getValue();
			final Element el;
			if ( value instanceof Integer )
				el = XmlHelpers.intElement( entry.getKey(), ( Integer ) entry.getValue() );
			else if ( value instanceof int[] )
				el = XmlHelpers.intArrayElement( entry.getKey(), ( int[] ) entry.getValue() );
			else if ( value instanceof Double )
				el = XmlHelpers.doubleElement( entry.getKey(), ( Double ) entry.getValue() );
			else if ( value instanceof double[] )
				el = XmlHelpers.doubleArrayElement( entry.getKey(), ( double[] ) entry.getValue() );
			else if ( value instanceof AffineGet )
				el = XmlHelpers.affineTransform3DElement( entry.getKey(), ( AffineGet ) entry.getValue() );
			else if ( value instanceof Boolean )
				el = XmlHelpers.booleanElement( entry.getKey(), ( Boolean ) value );
			else if ( value instanceof String )
			{
				el = new Element( entry.getKey() );
				el.setText( value.toString() );
			}
			else if ( value instanceof ScreenTransform )
			{
				final ScreenTransform t = ( ScreenTransform ) value;
				el = XmlHelpers.doubleArrayElement( entry.getKey(), new double[] {
						t.getMinX(),
						t.getMaxX(),
						t.getMinY(),
						t.getMaxY(),
						t.getScreenWidth(),
						t.getScreenHeight()
				} );
			}
			else if ( value instanceof Element )
			{
				el = new Element( entry.getKey() );
				el.setContent( ( Element ) value );
			}
			else
			{
				System.err.println( "Do not know how to serialize object " + value + " for key " + entry.getKey() + "." );
				continue;
			}
			element.addContent( el );
		}
		return element;
	}

	/**
	 * Wraps GUI state of a {@link MamutView} into a map.
	 * 
	 * @param view
	 *            the view.
	 * @return a new {@link Map}.
	 */
	private static Map< String, Object > getGuiState( final MamutView< ?, ?, ? > view )
	{
		final Map< String, Object > guiState = new LinkedHashMap<>();

		// View type.
		guiState.put( VIEW_TYPE_KEY, view.getClass().getSimpleName() );

		// Frame position and size.
		final Rectangle bounds = view.getFrame().getBounds();
		guiState.put( FRAME_POSITION_KEY, new int[] {
				( int ) bounds.getMinX(),
				( int ) bounds.getMinY(),
				( int ) bounds.getWidth(),
				( int ) bounds.getHeight() } );

		// Lock groups.
		guiState.put( GROUP_HANDLE_ID_KEY, view.getGroupHandle().getGroupId() );

		// Settings panel visibility.
		guiState.put( SETTINGS_PANEL_VISIBLE_KEY, view.getFrame().isSettingsPanelVisible() );

		// View-specifics.
		if ( view instanceof MamutViewBdv )
			getGuiStateBdv( ( MamutViewBdv ) view, guiState );
		else if ( view instanceof MamutViewTrackScheme )
			getGuiStateTrackScheme( ( MamutViewTrackScheme ) view, guiState );

		return guiState;
	}

	/**
	 * Stores the {@link MamutViewTrackScheme} GUI state in the specified map.
	 * 
	 * @param view
	 *            the {@link MamutViewTrackScheme}.
	 * @param guiState
	 *            the map to store info into.
	 */
	private static void getGuiStateTrackScheme( final MamutViewTrackScheme view, final Map< String, Object > guiState )
	{
		final TrackSchemePanel trackschemePanel = view.getTrackschemePanel();

		// Edit position to reflect the fact that we store the TrackScheme panel
		// width and height.
		final Point point = view.getFrame().getLocation();
		guiState.put( FRAME_POSITION_KEY, new int[] {
				point.x,
				point.y,
				trackschemePanel.getDisplay().getWidth(),
				trackschemePanel.getDisplay().getHeight() } );

		// Transform.
		final ScreenTransform t = trackschemePanel.getTransformEventHandler().getTransform().copy();
		guiState.put( TRACKSCHEME_TRANSFORM_KEY, t );

		// Coloring.
		final ColoringModel coloringModel = view.getColoringModel();
		getColoringState( coloringModel, guiState );

		// Context provider.
		guiState.put( CHOSEN_CONTEXT_PROVIDER_KEY, view.getContextChooser().getChosenProvider().getName() );
	}

	/**
	 * Stores the {@link MamutViewBdv} GUI state in the specified map.
	 * 
	 * @param view
	 *            the {@link MamutViewBdv}.
	 * @param guiState
	 *            the map to store info into.
	 */
	private static void getGuiStateBdv( final MamutViewBdv view, final Map< String, Object > guiState )
	{
		// Viewer state.
		final Element stateEl = view.getViewer().stateToXml();
		guiState.put( BDV_STATE_KEY, stateEl );
		// Transform.
		final AffineTransform3D t = new AffineTransform3D();
		view.getViewer().state().getViewerTransform( t );
		guiState.put( BDV_TRANSFORM_KEY, t );
		// Coloring.
		final ColoringModel coloringModel = view.getColoringModel();
		getColoringState( coloringModel, guiState );
	}

	/**
	 * Reads the coloring state of a view and stores it into the specified map.
	 * 
	 * @param coloringModel
	 *            the coloring model to read from.
	 * @param guiState
	 *            the map to store it to.
	 */
	private static void getColoringState( final ColoringModel coloringModel, final Map< String, Object > guiState )
	{
		final boolean noColoring = coloringModel.noColoring();
		guiState.put( NO_COLORING_KEY, noColoring );
		if ( !noColoring )
			if ( coloringModel.getTagSet() != null )
				guiState.put( TAG_SET_KEY, coloringModel.getTagSet().getName() );
			else if ( coloringModel.getFeatureColorMode() != null )
				guiState.put( FEATURE_COLOR_MODE_KEY, coloringModel.getFeatureColorMode().getName() );
	}

	/**
	 * Deserializes a GUI state from XML and recreate view windows as specified.
	 * 
	 * @param windowsEl
	 *            the XML element that stores the GUI state of a view.
	 * @param windowManager
	 *            the application {@link WindowManager}.
	 */
	static void fromXml( final Element windowsEl, final WindowManager windowManager )
	{
		// To deal later with context providers.
		final Map< String, ContextProvider< Spot > > contextProviders = new HashMap<>();
		final Map< ContextChooser< Spot >, String > contextChosers = new HashMap<>();

		final List< Element > viewEls = windowsEl.getChildren( WINDOW_TAG );
		for ( final Element viewEl : viewEls )
		{
			final Map< String, Object > guiState = xmlToMap( viewEl );
			final String typeStr = ( String ) guiState.get( VIEW_TYPE_KEY );
			switch ( typeStr )
			{
			case "MamutViewBdv":
			{
				final MamutViewBdv bdv = windowManager.createBigDataViewer( guiState );

				// Store context provider.
				contextProviders.put( bdv.getContextProvider().getName(), bdv.getContextProvider() );
				break;
			}

			case "MamutViewTrackScheme":
			{
				final MamutViewTrackScheme ts = windowManager.createTrackScheme( guiState );

				// Deal with context chooser.
				final String desiredProvider = ( String ) guiState.get( CHOSEN_CONTEXT_PROVIDER_KEY );
				if ( null != desiredProvider )
					contextChosers.put( ts.getContextChooser(), desiredProvider );
				break;
			}

			default:
				System.err.println( "Unknown window type: " + typeStr + "." );
				continue;
			}
		}

		/*
		 * Loop again on context choosers and try to give them their desired
		 * context provider.
		 */

		for ( final ContextChooser< Spot > contextChooser : contextChosers.keySet() )
		{
			final String desiredContextProvider = contextChosers.get( contextChooser );
			final ContextProvider< Spot > contextProvider = contextProviders.get( desiredContextProvider );
			if ( null != contextProvider )
				contextChooser.choose( contextProvider );
		}
	}

	private static Map< String, Object > xmlToMap( final Element viewEl )
	{
		final Map< String, Object > guiState = new HashMap<>();
		final List< Element > children = viewEl.getChildren();
		for ( final Element el : children )
		{
			final String key = el.getName();
			final Object value;
			switch ( key )
			{
			case BDV_STATE_KEY:
				value = el;
				break;
			case BDV_TRANSFORM_KEY:
				value = XmlHelpers.getAffineTransform3D( viewEl, key );
				break;
			case FRAME_POSITION_KEY:
				final int[] pos = XmlHelpers.getIntArray( viewEl, key );
				value = sanitize( pos );
				break;
			case TAG_SET_KEY:
			case FEATURE_COLOR_MODE_KEY:
			case VIEW_TYPE_KEY:
			case CHOSEN_CONTEXT_PROVIDER_KEY:
				value = el.getTextTrim();
				break;
			case TRACKSCHEME_TRANSFORM_KEY:
				final double[] arr = XmlHelpers.getDoubleArray( viewEl, key );
				value = new ScreenTransform( arr[ 0 ], arr[ 1 ], arr[ 2 ], arr[ 3 ], ( int ) arr[ 4 ], ( int ) arr[ 5 ] );
				break;
			case NO_COLORING_KEY:
			case SETTINGS_PANEL_VISIBLE_KEY:
				value = XmlHelpers.getBoolean( viewEl, key );
				break;
			case GROUP_HANDLE_ID_KEY:
				value = XmlHelpers.getInt( viewEl, key );
				break;
			default:
				System.err.println( "Unkown GUI config parameter: " + key + " found in GUI file." );
				continue;
			}
			guiState.put( key, value );
		}
		return guiState;
	}

	private static final int MIN_WIDTH = 200;

	private static final int MIN_HEIGHT = MIN_WIDTH;

	/**
	 * Makes sure the specified position array won't end in creating windows
	 * off-screen. We impose that a window is fully on *one* screen and not
	 * split over severals. We also impose a minimal size for the windows.
	 * <p>
	 * The pos array is { x, y, width, height }.
	 * 
	 * @param pos
	 *            the position array.
	 * @return the same position array.
	 */
	private static int[] sanitize( final int[] pos )
	{
		assert pos.length == 4;
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if ( null == ge )
			return pos;
		final GraphicsDevice sd[] = ge.getScreenDevices();
		if ( sd.length < 1 )
			return pos;

		// Window min size.
		pos[ 2 ] = Math.max( MIN_WIDTH, pos[ 2 ] );
		pos[ 3 ] = Math.max( MIN_HEIGHT, pos[ 3 ] );

		for ( final GraphicsDevice gd : sd )
		{
			final Rectangle bounds = gd.getDefaultConfiguration().getBounds();
			if ( bounds.contains( pos[ 0 ], pos[ 1 ], pos[ 2 ], pos[ 3 ] ) )
				// Fully in a screen, nothing to do.
				return pos;

			if ( bounds.contains( pos[ 0 ], pos[ 1 ] ) )
			{
				/*
				 * This window is on this screen, but exits it. First resize it
				 * so that it is not bigger than the screen.
				 */
				pos[ 2 ] = Math.min( bounds.width, pos[ 2 ] );
				pos[ 3 ] = Math.min( bounds.height, pos[ 3 ] );

				/*
				 * Then move it back so that its bottom right corner is in the
				 * screen.
				 */
				if ( pos[ 0 ] + pos[ 2 ] > bounds.x + bounds.width )
					pos[ 0 ] -= ( pos[ 0 ] - bounds.x + pos[ 2 ] - bounds.width );

				if ( pos[ 1 ] + pos[ 3 ] > bounds.y + bounds.height )
					pos[ 1 ] -= ( pos[ 1 ] - bounds.y + pos[ 3 ] - bounds.height );

				return pos;
			}
		}

		/*
		 * Ok we did not find a screen in which this window is. So we will put
		 * it in the first screen.
		 */
		final Rectangle bounds = sd[ 0 ].getDefaultConfiguration().getBounds();
		pos[ 0 ] = Math.max( bounds.x,
				Math.min( bounds.x + bounds.width - pos[ 2 ], pos[ 0 ] ) );
		pos[ 1 ] = Math.max( bounds.y,
				Math.min( bounds.y + bounds.height - pos[ 3 ], pos[ 1 ] ) );

		if ( bounds.contains( pos[ 0 ], pos[ 1 ], pos[ 2 ], pos[ 3 ] ) )
			// Fully in a screen, nothing to do.
			return pos;

		/*
		 * This window is on this screen, but exits it. First resize it so that
		 * it is not bigger than the screen.
		 */
		pos[ 2 ] = Math.min( bounds.width, pos[ 2 ] );
		pos[ 3 ] = Math.min( bounds.height, pos[ 3 ] );

		/*
		 * Then move it back so that its bottom right corner is in the screen.
		 */
		pos[ 0 ] -= ( pos[ 0 ] - bounds.x + pos[ 2 ] - bounds.width );
		pos[ 1 ] -= ( pos[ 1 ] - bounds.y + pos[ 3 ] - bounds.height );

		return pos;
	}
}
