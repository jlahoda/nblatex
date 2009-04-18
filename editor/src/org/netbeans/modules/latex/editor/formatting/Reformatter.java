/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 2008-2009 Sun
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
package org.netbeans.modules.latex.editor.formatting;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.Context.Region;
import org.netbeans.modules.editor.indent.spi.ExtraLock;
import org.netbeans.modules.editor.indent.spi.ReformatTask;
import org.netbeans.modules.latex.editor.formatting.Fragment.Diff;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.openide.util.Exceptions;

/**
 *
 * @author Jan Lahoda
 */
public class Reformatter implements ReformatTask {

    private final Document doc;
    private final List<Region> regions;

    private final Context context;

    Reformatter(Context context) {
        this.doc = null;
        this.regions = null;

        this.context = context;
    }

    Reformatter(Document doc, List<Region> regions) {
        this.doc = doc;
        this.regions = regions;

        this.context = null;
    }

    public void reformat() throws BadLocationException {
        Document     doc     = this.doc     != null ? this.doc     : this.context.document();
        List<Region> regions = this.regions != null ? this.regions : this.context.indentRegions();
        try {
            Source s = Source.create(doc);
            final List<Diff> diffs = new LinkedList<Diff>();

            ParserManager.parse(Collections.singleton(s), new UserTask() {
                public void run(ResultIterator it) throws Exception {
                    LaTeXParserResult lpr = LaTeXParserResult.get(it);

                    if (lpr == null) {
                        return ;
                    }
                    
                    diffs.addAll(FormatWorker.format(lpr, lpr.getDocument().getRootForFile(lpr.getSnapshot().getSource().getFileObject()), 0));
                }
            });

            Collections.sort(diffs, new Comparator<Diff>() {
                public int compare(Diff o1, Diff o2) {
                    return -(o1.getOffset() - o2.getOffset());
                }
            });
            
            for (Diff d : diffs) {
                int start = d.getOffset();
                int len = d.getRemoveLength();
                boolean found = false;

                for (Region r : regions) {
                    if (start + len < r.getStartOffset())
                        continue;

                    if (start > r.getEndOffset())
                        continue;
                    
                    found = true;
                }

                if (!found) {
                    continue;
                }

                doc.remove(d.getOffset(), d.getRemoveLength());
                doc.insertString(d.getOffset(), d.getAdd(), null);
            }
        } catch (ParseException ex) {
            if (ex.getCause() instanceof BadLocationException) {
                throw (BadLocationException) ex.getCause();
            }
            Exceptions.printStackTrace(ex);
        }
    }

    public ExtraLock reformatLock() {
        return new LockImpl();
    }

    private static final class LockImpl implements ExtraLock {

        public void lock() {
//            SourceAccessor.getINSTANCE().lockParser();
        }

        public void unlock() {
//            SourceAccessor.getINSTANCE().unlockParser();
        }
        
    }

    public static final class FactoryImpl implements Factory {

        public ReformatTask createTask(Context context) {
            return new Reformatter(context);
        }
        
    }
}
