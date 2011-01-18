/*
 * Copyright (c) 2011 Center for Bioinformatics of the University of Tuebingen.
 * 
 * This file is part of KEGGtranslator, a program to convert KGML files from the
 * KEGG database into various other formats, e.g., SBML, GraphML, and many more.
 * Please visit <http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 * 
 * KEGGtranslator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * KEGGtranslator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with KEGGtranslator. If not, see
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package de.zbit.kegg.gui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;

import javax.swing.JPanel;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SimpleSpeciesReference;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.util.StringTools;

/**
 * 
 * @author Andreas Dr&auml;ger
 * @date 2011-01-18
 */
public class ReactionPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1820252195280709150L;
	/**
	 * 
	 */
	private int preferredWith, preferredHeight;
	/**
	 * 
	 */
	private Reaction reaction;
	
	/**
	 * 
	 * @param reaction
	 */
	public ReactionPanel(Reaction reaction) {
		super();
		this.reaction = reaction;
		int fontSize = getFont().getSize();
		preferredWith = (createString(reaction.getListOfReactants(), false, true)
				.length()
				+ createString(reaction.getListOfModifiers(), true, true).length() + createString(
			reaction.getListOfProducts(), true, false).length())
				* fontSize;
		preferredHeight = reaction.isReversible() ? fontSize * 5 : fontSize * 3;
	}
	
	/**
	 * 
	 * @param listOf
	 * @param separator
	 * @param leadingBlank
	 * @param tailingBlank
	 * @return
	 */
	private String createString(ListOf<? extends SimpleSpeciesReference> listOf,
		boolean leadingBlank, boolean tailingBlank) {
		StringBuilder sb = new StringBuilder();
		if (leadingBlank) {
			sb.append(' ');
		}
		if (listOf.size() > 0) {
			SimpleSpeciesReference specRef;
			for (int i = 0; i < listOf.size(); i++) {
				specRef = listOf.get(i);
				if (specRef instanceof SpeciesReference) {
					SpeciesReference s = (SpeciesReference) specRef;
					if (s.getStoichiometry() != 1d) {
						sb.append(StringTools.toString(s.getStoichiometry()));
						sb.append(' ');
					}
				}
				sb.append(specRef.getSpecies());
				if (i < listOf.size() - 1) {
					sb.append((specRef instanceof SpeciesReference) ? " + " : ", ");
				}
			}
		} else {
			// empty set symbol
			sb.append("\u2205");
		}
		if (tailingBlank) {
			sb.append(' ');
		}
		return sb.toString();
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param length
	 * @param fontSize
	 * @param g
	 */
	private void drawReactionArrow(int x, int y, double length, int fontSize,
		Graphics g) {
		int x2 = (int) (x + length), y2, width = fontSize / 3, height = fontSize / 4;
		if (reaction.isReversible()) {
			y2 = y - width;
			int y3 = y2 + height;
			g.drawLine(x, y2, x2, y2);
			g.drawLine(x2, y2, x2 - width, y2 - height);
			g.drawLine(x, y3, x2, y3);
			g.drawLine(x, y3, x + width, y3 + height);
		} else {
			y2 = y - width;
			g.drawLine(x, y2, x2 - 1, y2);
			Polygon p = new Polygon(new int[] { x2 - width, x2, x2 - width },
				new int[] { y2 - height, y2, y2 + height }, 3);
			g.fillPolygon(p);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#getPreferredSize()
	 */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(preferredWith, preferredHeight);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (g != null) {
			int fontSize = g.getFont().getSize(), x = 0, y = fontSize;
			double length;
			if (reaction.getNumModifiers() > 0) {
				y *= 2;
			}
			FontMetrics metrics = g.getFontMetrics();
			String curr = createString(reaction.getListOfReactants(), false, true);
			g.drawString(curr, x, y);
			x += metrics.getStringBounds(curr, g).getWidth();
			if (reaction.getNumModifiers() > 0) {
				curr = createString(reaction.getListOfModifiers(), true, true);
				g.drawString(curr, x, y - fontSize);
				length = metrics.getStringBounds(curr, g).getWidth();
				drawReactionArrow(x, y, length, fontSize, g);
				x += length;
			} else {
				// simply draw a regular reaction arrow
				curr = reaction.isReversible() ? " \u21CC " : " \u21FE ";
				g.drawString(curr, x, y);
				x += metrics.getStringBounds(curr, g).getWidth();
			}
			curr = createString(reaction.getListOfProducts(), true, false);
			g.drawString(curr, x, y);
			preferredWith = 2 * fontSize
					+ (int) (x + metrics.getStringBounds(curr, g).getWidth());
			preferredHeight = fontSize + y;
		}
	}
	
	
	
}
