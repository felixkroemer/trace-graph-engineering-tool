package com.felixkroemer.trace_graph_engineering_tool.renderer.intr;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2021 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


/**
 * A utility class which conveniently converts an array of integers into
 * an IntIterator (an iteration of integers).
 */
public final class ArrayIntIterator implements IntIterator {
    private final int[] m_elements;
    private int m_index;
    private final int m_end;

    /**
     * No copy of the elements array is made.  The contents of the array
     * are never modified by this object.
     */
    public ArrayIntIterator(int[] elements, int beginIndex, int length) {
        if (beginIndex < 0) throw new IllegalArgumentException("beginIndex is less than zero");

        if (length < 0) throw new IllegalArgumentException("length is less than zero");

        if ((((long) beginIndex) + (long) length) > (long) elements.length)
            throw new IllegalArgumentException("combination of beginIndex and length exceed length of array");

        m_elements = elements;
        m_index = beginIndex;
        m_end = beginIndex + length;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final boolean hasNext() {
        return m_index < m_end;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final int nextInt() {
        return m_elements[m_index++];
    }
}
