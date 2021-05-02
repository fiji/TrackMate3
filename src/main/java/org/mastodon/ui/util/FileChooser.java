/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.ui.util;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Image;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class FileChooser
{
	public static boolean useJFileChooser = !isMac();

	public static enum DialogType
	{
		LOAD, SAVE
	}

	public static enum SelectionMode
	{
		FILES_ONLY, DIRECTORIES_ONLY, FILES_AND_DIRECTORIES
	}

	public static File chooseFile(
			final Component parent,
			final String selectedFile,
			final DialogType dialogType )
	{
		return chooseFile( parent, selectedFile, null, null, dialogType );
	}

	public static File chooseFile(
			final Component parent,
			final String selectedFile,
			final FileFilter fileFilter,
			final String dialogTitle,
			final DialogType dialogType )
	{
		return chooseFile( parent, selectedFile, fileFilter, dialogTitle, dialogType, SelectionMode.FILES_ONLY );
	}

	public static File chooseFile(
			final Component parent,
			final String selectedFile,
			final XmlFileFilter fileFilter,
			final String dialogTitle,
			final DialogType dialogType,
			final Image image )
	{
		return chooseFile( useJFileChooser, parent, selectedFile, fileFilter, dialogTitle, dialogType, SelectionMode.FILES_ONLY, image );
	}

	public static File chooseFile(
			final Component parent,
			final String selectedFile,
			final FileFilter fileFilter,
			final String dialogTitle,
			final DialogType dialogType,
			final SelectionMode selectionMode )
	{
		return chooseFile( useJFileChooser, parent, selectedFile, fileFilter, dialogTitle, dialogType, selectionMode, null );
	}

	public static File chooseFile(
			final boolean useJFileChooser,
			final Component parent,
			final String selectedFile,
			final FileFilter fileFilter,
			final String dialogTitle,
			final DialogType dialogType,
			final SelectionMode selectionMode )
	{
		return chooseFile( useJFileChooser, parent, selectedFile, fileFilter, dialogTitle, dialogType, selectionMode, null );
	}

	public static File chooseFile(
			boolean useJFileChooser,
			final Component parent,
			final String selectedFile,
			final FileFilter fileFilter,
			final String dialogTitle,
			final DialogType dialogType,
			final SelectionMode selectionMode,
			final Image iconImage )
	{
		final boolean isSaveDialog = ( dialogType == DialogType.SAVE );
		final boolean isDirectoriesOnly = ( selectionMode == SelectionMode.DIRECTORIES_ONLY );

		if ( isSaveDialog && isDirectoriesOnly )
			useJFileChooser = true; // FileDialog cannot handle this

		/*
		 * Determine dialog title:
		 *
		 * If a dialogTitle is given, just use that.
		 *
		 * Otherwise, use "Open" or "Save", depending on DialogType. If a
		 * FileFilter is provided, append the FileFilter description,
		 * leading to "Open xml files" or similar.
		 */
		String title = dialogTitle;
		if ( title == null )
			title = ( isSaveDialog ? "Save" : "Open" )
					+ ( fileFilter == null ? "" : " " + fileFilter.getDescription() );

		File file = null;
		if ( useJFileChooser )
		{
			final JFileChooser fileChooser = new JFileChooser()
			{

				private static final long serialVersionUID = 1L;

				@Override
				protected JDialog createDialog( final Component parent ) throws HeadlessException
				{
					final JDialog dialog = super.createDialog( parent );
					dialog.setIconImage( iconImage );
					return dialog;
				}
			};

			fileChooser.setDialogTitle( title );

			if ( selectedFile != null )
				fileChooser.setSelectedFile( new File( selectedFile ) );

			switch ( selectionMode )
			{
			case FILES_ONLY:
				fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
				break;
			case DIRECTORIES_ONLY:
				fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
				break;
			case FILES_AND_DIRECTORIES:
				fileChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
				break;
			}

			fileChooser.setFileFilter( fileFilter );

			final int returnVal = isSaveDialog
					? fileChooser.showSaveDialog( parent )
					: fileChooser.showOpenDialog( parent );
			if ( returnVal == JFileChooser.APPROVE_OPTION )
				file = fileChooser.getSelectedFile();
		}
		else // use FileDialog
		{
			final int fdMode = isSaveDialog ? FileDialog.SAVE : FileDialog.LOAD;

			/*
			 * If provided parent is a Frame or a Dialog, we can use it.
			 * Otherwise use null as parent.
			 */
			final FileDialog fd;
			if ( parent != null && parent instanceof Frame )
				fd = new FileDialog( ( Frame ) parent, title, fdMode );
			else if ( parent != null && parent instanceof Dialog )
				fd = new FileDialog( ( Dialog ) parent, title, fdMode );
			else
				fd = new FileDialog( ( Frame ) null, title, fdMode );

			fd.setIconImage( iconImage );

			/*
			 * If a selectedFile path was provided, set it.
			 */
			if ( selectedFile != null )
			{
				System.out.println( "selectedFile = " + selectedFile );
				if ( isDirectoriesOnly )
				{
					fd.setDirectory( selectedFile );
					fd.setFile( null );
				}
				else
				{
					fd.setDirectory( new File( selectedFile ).getParent() );
					fd.setFile( new File( selectedFile ).getName() );
				}
			}

			/*
			 * Handle SelectionMode DIRECTORIES_ONLY.
			 */
			System.setProperty( "apple.awt.fileDialogForDirectories", isDirectoriesOnly ? "true" : "false" );

			/*
			 * Try with a FilenameFilter (may silently fail).
			 */
			final AtomicBoolean workedWithFilenameFilter = new AtomicBoolean( false );
			if ( fileFilter != null )
			{
				final FilenameFilter filenameFilter = new FilenameFilter()
				{
					private boolean firstTime = true;

					@Override
					public boolean accept( final File dir, final String name )
					{
						if ( firstTime )
						{
							workedWithFilenameFilter.set( true );
							firstTime = false;
						}

						return fileFilter.accept( new File( dir, name ) );
					}
				};
				fd.setFilenameFilter( filenameFilter );
				fd.setVisible( true );
			}
			if ( fileFilter == null || ( isMac() && !workedWithFilenameFilter.get() ) )
			{
				fd.setFilenameFilter( null );
				fd.setVisible( true );
			}

			final String filename = fd.getFile();
			if ( filename != null )
			{
				file = new File( fd.getDirectory() + filename );
			}
		}

		return file;
	}

	private static boolean isMac()
	{
		final String OS = System.getProperty( "os.name", "generic" ).toLowerCase( Locale.ENGLISH );
		return ( OS.indexOf( "mac" ) >= 0 ) || ( OS.indexOf( "darwin" ) >= 0 );
	}

//	public static void main( final String[] args )
//	{
//		final FileFilter xmlfileFilter = new FileFilter()
//		{
//			@Override
//			public String getDescription()
//			{
//				return "xml files";
//			}
//
//			@Override
//			public boolean accept( final File f )
//			{
//				if ( f.isDirectory() )
//					return true;
//				if ( f.isFile() )
//				{
//			        final String s = f.getName();
//			        final int i = s.lastIndexOf('.');
//			        if (i > 0 &&  i < s.length() - 1) {
//			            final String ext = s.substring(i+1).toLowerCase();
//			            return ext.equals( "xml" );
//			        }
//				}
//				return false;
//			}
//		};
//		final Component parent = null;
//		final String selectedFile = null;
//		final FileFilter fileFilter = xmlfileFilter;
//		final String dialogTitle = null;
//		final DialogType dialogType = DialogType.LOAD;
//		final SelectionMode selectionMode = SelectionMode.FILES_ONLY;
//		final File file = FileChooser.chooseFile( useJFileChooser, parent, selectedFile, fileFilter, dialogTitle, dialogType, selectionMode );
//
//		System.out.println( file );
//	}
//
//	public static void main( final String[] args )
//	{
//		final Context context = new Context( UIService.class );
//		final UIService service = context.getService( UIService.class );
//		final File file = service.chooseFile( null, "open" );
//		System.out.println( file );
//
//		final ImageJ ij = new ImageJ();
//
//		// ask the user for a file to open
//		final File file2 = ij.ui().chooseFile(null, "open");
//		System.out.println( file );
//
//	}
}
