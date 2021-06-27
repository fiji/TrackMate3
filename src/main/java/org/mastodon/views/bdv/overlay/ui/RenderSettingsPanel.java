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
package org.mastodon.views.bdv.overlay.ui;

import static org.mastodon.app.ui.settings.StyleElements.booleanElement;
import static org.mastodon.app.ui.settings.StyleElements.colorElement;
import static org.mastodon.app.ui.settings.StyleElements.doubleElement;
import static org.mastodon.app.ui.settings.StyleElements.intElement;
import static org.mastodon.app.ui.settings.StyleElements.linkedCheckBox;
import static org.mastodon.app.ui.settings.StyleElements.linkedColorButton;
import static org.mastodon.app.ui.settings.StyleElements.linkedSliderPanel;
import static org.mastodon.app.ui.settings.StyleElements.separator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.mastodon.app.ui.settings.StyleElements.BooleanElement;
import org.mastodon.app.ui.settings.StyleElements.ColorElement;
import org.mastodon.app.ui.settings.StyleElements.DoubleElement;
import org.mastodon.app.ui.settings.StyleElements.IntElement;
import org.mastodon.app.ui.settings.StyleElements.Separator;
import org.mastodon.app.ui.settings.StyleElements.StyleElement;
import org.mastodon.app.ui.settings.StyleElements.StyleElementVisitor;
import org.mastodon.views.bdv.overlay.RenderSettings;

import bdv.tools.brightness.SliderPanel;
import bdv.tools.brightness.SliderPanelDouble;

public class RenderSettingsPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private static final int tfCols = 4;

	private static final Dimension SLIDER_PREFERRED_DIM = new Dimension( 50, 30 );

	private final JColorChooser colorChooser;

	private final List< StyleElement > styleElements;

	public RenderSettingsPanel( final RenderSettings style )
	{
		super( new GridBagLayout() );
		colorChooser = new JColorChooser();
		styleElements = styleElements( style );

		style.updateListeners().add( () -> {
			styleElements.forEach( StyleElement::update );
			repaint();
		} );

		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 0, 5, 0, 5 );
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

		styleElements.forEach( element -> element.accept(
				new StyleElementVisitor()
				{
					@Override
					public void visit( final Separator element )
					{
						add( Box.createVerticalStrut( 10 ), c );
						++c.gridy;
					}

					@Override
					public void visit( final BooleanElement element )
					{
						final JCheckBox checkbox = linkedCheckBox( element, "" );
						checkbox.setHorizontalAlignment( SwingConstants.TRAILING );
						addToLayout(
								checkbox,
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final DoubleElement element )
					{
						final SliderPanelDouble slider = linkedSliderPanel( element, tfCols );
						slider.setPreferredSize( SLIDER_PREFERRED_DIM );
						addToLayout(
								slider,
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final IntElement element )
					{
						final SliderPanel slider = linkedSliderPanel( element, tfCols );
						slider.setPreferredSize( SLIDER_PREFERRED_DIM );
						addToLayout(
								slider,
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final ColorElement element )
					{
						final JButton button = linkedColorButton( element, null, colorChooser );
						button.setHorizontalAlignment( SwingConstants.RIGHT );
						addToLayout(
								button,
								new JLabel( element.getLabel() ) );
					}

					private void addToLayout( final JComponent comp1, final JComponent comp2 )
					{
						c.anchor = GridBagConstraints.LINE_END;
						add( comp1, c );
						c.gridx++;
						c.weightx = 0.0;
						c.anchor = GridBagConstraints.LINE_START;
						add( comp2, c );
						c.gridx = 0;
						c.weightx = 1.0;
						c.gridy++;
					}
				} ) );
	}

	private List< StyleElement > styleElements( final RenderSettings style )
	{
		return Arrays.asList(
				booleanElement( "anti-aliasing", style::getUseAntialiasing, style::setUseAntialiasing ),

				separator(),

				colorElement(
						"spot color",
						() -> new Color( style.getColorSpot(), true ),
						( c ) -> style.setColorSpot( c.getRGB() ) ),
				colorElement(
						"links backward in time",
						() -> new Color( style.getColorPast(), true ),
						( c ) -> style.setColorPast( c.getRGB() ) ),
				colorElement(
						"links ahead in time",
						() -> new Color( style.getColorFuture(), true ),
						( c ) -> style.setColorFuture( c.getRGB() ) ),

				separator(),

				booleanElement( "draw links", style::getDrawLinks, style::setDrawLinks ),
				intElement( "time range for links", 0, 100, style::getTimeLimit, style::setTimeLimit ),
				booleanElement( "gradients for links", style::getUseGradient, style::setUseGradient ),
				booleanElement( "arrow heads", style::getDrawArrowHeads, style::setDrawArrowHeads ),
				booleanElement( "draw links ahead in time", style::getDrawLinksAheadInTime, style::setDrawLinksAheadInTime ),
				doubleElement( "link stroke width", 1, 100, style::getLinkStrokeWidth, style::setLinkStrokeWidth ),

				separator(),

				booleanElement( "draw spots", style::getDrawSpots, style::setDrawSpots ),
				booleanElement( "ellipsoid intersection", style::getDrawEllipsoidSliceIntersection, style::setDrawEllipsoidSliceIntersection ),
				booleanElement( "ellipsoid projection", style::getDrawEllipsoidSliceProjection, style::setDrawEllipsoidSliceProjection ),
				booleanElement( "draw spot centers", style::getDrawSpotCenters, style::setDrawSpotCenters ),
				booleanElement( "draw spot centers for ellipses", style::getDrawSpotCentersForEllipses, style::setDrawSpotCentersForEllipses ),
				booleanElement( "draw spot labels", style::getDrawSpotLabels, style::setDrawSpotLabels ),
				doubleElement( "spot stroke width", 1, 100, style::getSpotStrokeWidth, style::setSpotStrokeWidth ),

				separator(),

				doubleElement( "focus limit (max dist to view plane)", 0, 2000, style::getFocusLimit, style::setFocusLimit ),
				booleanElement( "view relative focus limit", style::getFocusLimitViewRelative, style::setFocusLimitViewRelative ),

				separator(),
				doubleElement( "ellipsoid fade depth", 0, 1, style::getEllipsoidFadeDepth, style::setEllipsoidFadeDepth ),
				doubleElement( "center point fade depth", 0, 1, style::getPointFadeDepth, style::setPointFadeDepth )
		);
	}
}
