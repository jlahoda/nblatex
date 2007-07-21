/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.latex.editor;

import org.netbeans.spi.editor.highlighting.HighlightsContainer;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.ZOrder;

/**
 *
 * @author Jan Lahoda
 */
public class HighlightsLayerFactoryImpl implements HighlightsLayerFactory {
    
    /** Creates a new instance of HighlightsLayerFactoryImpl */
    public HighlightsLayerFactoryImpl() {
    }
    
    public HighlightsLayer[] createLayers(Context context) {
        return new HighlightsLayer[] {
            HighlightsLayer.create(HighlightsLayerFactoryImpl.class.getName(), ZOrder.SYNTAX_RACK, true, ColoringEvaluator.getDelegate(context.getDocument())),
        };
    }
    
}