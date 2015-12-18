package net.trackmate.revised.ui.grouping;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

public class GroupLocksPanel extends JPanel implements GroupChangeListener
{
	private static final long serialVersionUID = 1L;

	private static final ImageIcon LOCK_ICON = new ImageIcon( GroupLocksPanel.class.getResource( "lock.png" ) );

	private static final ImageIcon UNLOCK_ICON = new ImageIcon( GroupLocksPanel.class.getResource( "lock_open_grey.png" ) );

	private static final Font FONT = new Font( "Arial", Font.PLAIN, 10 );

	private static final int N_LOCKS = 3;

	private final ArrayList< JToggleButton > buttons;

	private final GroupHandle groupHandle;

	public GroupLocksPanel( final GroupHandle groupHandle )
	{
		this.groupHandle = groupHandle;
		this.buttons = new ArrayList<>();
		setLayout( new FlowLayout( FlowLayout.LEADING ) );
		for ( int i = 0; i < N_LOCKS; i++ )
		{
			final int lockId = i;
			final boolean isActive = groupHandle.isInGroup( lockId );
			final JToggleButton button = new JToggleButton( "" + ( i + 1 ), isActive ? LOCK_ICON : UNLOCK_ICON, isActive );
			button.setFont( FONT );
			button.setPreferredSize( new Dimension( 60, 20 ) );
			button.setHorizontalAlignment( SwingConstants.LEFT );
			button.setOpaque( false );
			button.setContentAreaFilled( false );
			button.setBorderPainted( false );
			button.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					if ( button.isSelected() )
						groupHandle.addToGroup( lockId );
					else
						groupHandle.removeFromGroup( lockId );
				}
			} );
			add( button );
			buttons.add( button );
		}
		groupHandle.addGroupChangeListener( this );
	}

	@Override
	public void groupChanged()
	{
		for ( int i = 0; i < N_LOCKS; ++i )
		{
			final boolean activated = groupHandle.isInGroup( i );
			final JToggleButton button = buttons.get( i );
			button.setSelected( activated );
			button.setIcon( activated ? LOCK_ICON : UNLOCK_ICON );
		}
	}
}
