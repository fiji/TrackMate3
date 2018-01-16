package org.mastodon.app.ui.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.mastodon.util.Listeners;

/**
 * A {@link SettingsPage} for the following common scenario: There are multiple
 * "profiles" (e.g., keymaps, trackscheme styles, ...) to select from, and its
 * possible to edit each individual profile.
 * <p>
 * To instantiate such a {@code SettingsPage}, a {@code ProfileManager} and a
 * {@code ProfileEditPanel} have to be provided. The {@link ProfileManager} is
 * responsible for selecting/renaming/etc profile. The {@link ProfileEditPanel}
 * is responsible for editing an individual profile.
 * </p>
 * <p>
 * The {@code SelectAndEditProfileSettingsPage} provides UI for selecting,
 * duplicating, renaming, and deleting profiles. If a {@code Profile} is
 * {@link Profile#isBuiltin() built-in} it is unmodifiable. When the user edits
 * it, a duplicate is made automatically.
 * </p>
 *
 * @param <T>
 *            type of thing being edited, e.g., {@code TrackSchemeStyle}.
 *
 * @author Tobias Pietzsch
 */
public class SelectAndEditProfileSettingsPage< T extends SelectAndEditProfileSettingsPage.Profile > implements SettingsPage
{
	private final String treePath;

	private final ProfileManager< T > profileManager;

	private final ProfileEditPanel< T > profileEditPanel;

	private final Listeners.List< ModificationListener > modificationListeners;

	private final JPanel contentPanel;

	/**
	 *
	 * @param treePath
	 *            path of this page in the settings tree.
	 * @param profileManager
	 * @param profileEditPanel
	 */
	public SelectAndEditProfileSettingsPage(
			final String treePath,
			final ProfileManager< T > profileManager,
			final ProfileEditPanel< T > profileEditPanel )
	{
		this.treePath = treePath;
		this.profileManager = profileManager;
		this.profileEditPanel = profileEditPanel;

		final ProfileSelectionPanel< T > profileSelectionPanel = new ProfileSelectionPanel<>( profileManager, profileEditPanel );
		profileSelectionPanel.setBorder( new EmptyBorder( 0, 0, 10, 0 ) );

		profileEditPanel.modificationListeners().add( profileSelectionPanel );

		modificationListeners = new Listeners.SynchronizedList<>();
		profileEditPanel.modificationListeners().add( () -> modificationListeners.list.forEach( ModificationListener::modified ) );
		profileSelectionPanel.selectionListeners().add( p -> modificationListeners.list.forEach( ModificationListener::modified ) );

		contentPanel = new JPanel( new BorderLayout() );
		contentPanel.add( profileSelectionPanel, BorderLayout.NORTH );
		contentPanel.add( profileEditPanel.getJPanel(), BorderLayout.CENTER );
	}

	@Override
	public String getTreePath()
	{
		return treePath;
	}

	@Override
	public JPanel getJPanel()
	{
		return contentPanel;
	}

	@Override
	public Listeners< ModificationListener > modificationListeners()
	{
		return modificationListeners;
	}

	@Override
	public void apply()
	{
		profileEditPanel.storeProfile( profileManager.getSelectedProfile() );
		profileManager.apply();
	}

	@Override
	public void cancel()
	{
		profileManager.cancel();
	}

	/**
	 *
	 *
	 *
	 */
	public interface Profile
	{
		boolean isBuiltin();

		String getName();
	}

	/**
	 *
	 * @param <T>
	 */
	public interface ProfileManager< T extends Profile >
	{
		List< T > getProfiles();

		T getSelectedProfile();

		/**
		 * Select the active profile.
		 *
		 * @param profile
		 */
		void select( T profile );

		/**
		 * Dublicate {@code profile} with a new derived name and add it to user profiles list.
		 *
		 * @param profile
		 *
		 * @return duplicated profile
		 */
		T duplicate( T profile );

		/**
		 * Try to set the name of {@code profile} to {@code newName}.
		 *
		 * @param profile
		 * @param newName
		 *
		 * @throws IllegalArgumentException
		 * 		if renaming was not possible (e.g., newName already exists)
		 */
		void rename( T profile, String newName ) throws IllegalArgumentException;

		void delete( T profile );

		void apply();

		void cancel();
	}

	/**
	 *
	 * @param <T>
	 */
	public interface ProfileEditPanel< T >
	{
		Listeners< ModificationListener > modificationListeners();

		void loadProfile( T profile );

		void storeProfile( T profile );

		JPanel getJPanel();
	}

	interface SelectionListener< T >
	{
		public void selected( T profile );

		public default void deselected( final T profile ) {};
	}

	static class ProfileSelectionPanel< T extends Profile > extends JPanel implements ModificationListener
	{
		private static final long serialVersionUID = 1L;

		private final ProfileManager< T > manager;

		private final ProfileEditPanel< T > editor;

		private boolean blockComboBoxItemListener = false;

		private final JComboBox< Item > comboBox;

		private final Listeners.SynchronizedList< SelectionListener< T > > selectionListeners;

		private final JButton buttonDelete;

		private final JButton buttonDuplicate;

		private final JButton buttonRename;

		public ProfileSelectionPanel(
				final ProfileManager< T > manager,
				final ProfileEditPanel< T > editor )
		{
			this.manager = manager;
			this.editor = editor;
			selectionListeners = new Listeners.SynchronizedList<>();

			comboBox = new JComboBox<>();
			comboBox.setEditable( false );
			final Dimension dim = new Dimension( 300, comboBox.getPreferredSize().height );
			comboBox.setPreferredSize( dim );
			comboBox.setMaximumSize( dim );

			buttonDuplicate = new JButton( "Duplicate" );
			buttonRename = new JButton( "Rename" );
			buttonDelete = new JButton( "Delete" );

			comboBox.addItemListener( e -> {
				if ( blockComboBoxItemListener )
					return;
				final T profile = ( ( Item ) e.getItem() ).profile;
				if ( e.getStateChange() == ItemEvent.SELECTED )
				{
					manager.select( profile );
					buttonDelete.setEnabled( !profile.isBuiltin() );
					buttonRename.setEnabled( !profile.isBuiltin() );
					selectionListeners.list.forEach( l -> l.selected( profile ) );
					editor.loadProfile( profile );
				}
				else
				{
					selectionListeners.list.forEach( l -> l.deselected( profile ) );
					editor.storeProfile( profile );
				}
			} );
			buttonDuplicate.addActionListener( e -> duplicateSelected() );
			buttonRename.addActionListener( e -> renameSelected() );
			buttonDelete.addActionListener( e -> deleteSelected() );

			setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			add( Box.createHorizontalGlue() );
			add( comboBox );
			add( buttonDuplicate );
			add( buttonRename );
			add( buttonDelete );
			add( Box.createHorizontalGlue() );

			makeModel();
		}

		private void duplicateSelected()
		{
			final Item selected = ( Item ) comboBox.getSelectedItem();
			if ( selected != null )
			{
				editor.storeProfile( selected.profile );
				final T duplicate = manager.duplicate( selected.profile );
				manager.select( duplicate );
				editor.loadProfile( duplicate );
				makeModel();
			}
		}

		private void deleteSelected()
		{
			final Item selected = ( Item ) comboBox.getSelectedItem();
			if ( selected != null )
			{
				manager.delete( selected.profile );
				makeModel();
			}
		}

		private void renameSelected()
		{
			final Item selected = ( Item ) comboBox.getSelectedItem();
			if ( selected != null )
			{
				final String oldName = selected.getName();
				final String newName = ( String ) JOptionPane.showInputDialog(
						this,
						"Enter the new name:",
						"Rename",
						JOptionPane.PLAIN_MESSAGE, null, null, oldName );
				if ( newName != null && !newName.equals( oldName ) )
				{
					try
					{
						manager.rename( selected.profile, newName );
						makeModel();
					}
					catch ( final IllegalArgumentException e )
					{
						JOptionPane.showMessageDialog( this, e.getMessage(), "Rename failed", JOptionPane.ERROR_MESSAGE );
					}
				}
			}
		}

		@Override
		public void modified()
		{
			if ( ( ( Item ) comboBox.getSelectedItem() ).isBuiltin() )
			{
				duplicateSelected();
			}
		}

		private void makeModel()
		{
			final Vector< Item > items = new Vector<>();
			manager.getProfiles().forEach( profile -> items.add( new Item( profile ) ) );
			comboBox.setModel( new DefaultComboBoxModel<>( items ) );
			final Item selectedItem = new Item( manager.getSelectedProfile() );
			buttonDelete.setEnabled( !selectedItem.isBuiltin() );
			buttonRename.setEnabled( !selectedItem.isBuiltin() );
			blockComboBoxItemListener = true;
			comboBox.setSelectedIndex( items.indexOf( selectedItem ) );
			blockComboBoxItemListener = false;
		}

		public Listeners< SelectionListener< T > > selectionListeners()
		{
			return selectionListeners;
		}

		class Item
		{
			private final T profile;

			public Item( final T profile )
			{
				this.profile = profile;
			}

			@Override
			public String toString()
			{
				return profile.isBuiltin()
						? "<html><b>" + profile.getName() + "</b> <i>(built-in)</i></html>"
						: profile.getName();
			}

			public String getName()
			{
				return profile.getName();
			}

			public boolean isBuiltin()
			{
				return profile.isBuiltin();
			}

			@Override
			public boolean equals( final Object o )
			{
				if ( this == o )
					return true;
				if ( o == null || getClass() != o.getClass() )
					return false;

				final Item item = ( Item ) o;

				return profile.equals( item.profile );
			}

			@Override
			public int hashCode()
			{
				return profile.hashCode();
			}
		}
	}
}
