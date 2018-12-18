package org.mastodon.model;

import org.mastodon.util.Listeners;

/**
 * Class to manage the object that has the "focus", regardless of how this focus
 * is used.
 *
 * @param <T>
 *            type of object that might receive the focus.
 */
public interface FocusModel< T >
{

	/**
	 * Sets the object that has the focus in this model. If <code>null</code>,
	 * the focus is cleared.
	 * 
	 * @param obj
	 *            the object to focus, or <code>null</code>.
	 */
	public void focus( final T obj );

	/**
	 * Returns the object that has the focus in this model. Returns
	 * <code>null</code> if there is not a focused object in this model.
	 * 
	 * @param ref
	 *            a ref object, possibly used to return the focused object.
	 * @return the focused object or <code>null</code>.
	 */
	public T getFocused( final T ref );

	/**
	 * Exposes the listener list of this focus model. Listeners are notified
	 * when the focused object changes.
	 * 
	 * @return the listeners.
	 * @see #focus(Object)
	 */
	public Listeners< FocusListener > listeners();
}
