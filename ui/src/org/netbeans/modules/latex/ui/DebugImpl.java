/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2010 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.latex.ui;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.modules.latex.model.command.DebuggingSupport;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.ZOrder;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;

/**
 *
 * @author Jan Lahoda
 */
public class DebugImpl implements PropertyChangeListener {
    
    private static final AttributeSet HIGHLIGHT_COLORING = AttributesUtilities.createImmutable(StyleConstants.Background, Color.GRAY);
    public DebugImpl() {}

    private Reference<Document> last;
        
    public void propertyChange(PropertyChangeEvent evt) {
        if (!DebuggingSupport.getDefault().isDebuggingEnabled())
            return ;
        
        Node n = DebuggingSupport.getDefault().getCurrentSelectedNode();
        Document lastDocument = last != null ? last.get() : null;

        if (lastDocument != null) {
            getBag(lastDocument).setHighlights(new OffsetsBag(lastDocument));
        }

        last = null;
        
        if (n != null) {
            SourcePosition spos = n.getStartingPosition();
            SourcePosition epos = n.getEndingPosition();
            
            Document doc = spos.getDocument();

            if (doc != null) {
                OffsetsBag bag = new OffsetsBag(doc);

                bag.addHighlight(spos.getOffset().getOffset(), epos.getOffset().getOffset(), HIGHLIGHT_COLORING);
                getBag(doc);
                last = new WeakReference<Document>(doc);
            }
        }
    }

    private static OffsetsBag getBag(Document doc) {
        OffsetsBag bag = (OffsetsBag) doc.getProperty(DebugImpl.class);

        if (bag == null) {
            doc.putProperty(DebugImpl.class, bag = new OffsetsBag(doc));
        }

        return bag;
    }

    public static final class HighlightsLayerFactoryImpl implements HighlightsLayerFactory {

        public HighlightsLayer[] createLayers(Context context) {
            return new HighlightsLayer[] {
                HighlightsLayer.create(HighlightsLayerFactoryImpl.class.getName(), ZOrder.CARET_RACK, true, getBag(context.getDocument()))
            };
        }

    }
    
}
