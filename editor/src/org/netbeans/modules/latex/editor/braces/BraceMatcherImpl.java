/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 2008 Sun
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

package org.netbeans.modules.latex.editor.braces;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.BadLocationException;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.gsf.api.CancellableTask;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.napi.gsfret.source.CompilationInfo;
import org.netbeans.spi.editor.bracesmatching.BracesMatcher;
import org.netbeans.spi.editor.bracesmatching.BracesMatcherFactory;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jan Lahoda
 */
public class BraceMatcherImpl implements BracesMatcher {

    private final MatcherContext context;

    public BraceMatcherImpl(MatcherContext context) {
        this.context = context;
    }
    
    public int[] findOrigin() throws InterruptedException, BadLocationException {
        List<int[]> braces = computeBraces();
        
        if (braces.isEmpty()) {
            return null;
        }
        
        return braces.get(0);
    }

    public int[] findMatches() throws InterruptedException, BadLocationException {
        List<int[]> braces = computeBraces();
        
        if (braces.size() < 2) {
            return null;
        }

        int[] result = new int[2 * (braces.size() - 1)];
        int   index  = 0;

        for (int[] i : braces.subList(1, braces.size())) {
            result[index++] = i[0];
            result[index++] = i[1];
        }

        return result;
    }

    private List<int[]> braces;

    private synchronized List<int[]> computeBraces() {
        if (braces == null) {
            braces = computeBracesImpl();
        }

        return braces;
    }

    private List<int[]> computeBracesImpl() {
        FileObject f = NbEditorUtilities.getFileObject(context.getDocument());
        
        if (f == null) {
            return Collections.emptyList();
        }
        
        final List<int[]> result = new LinkedList<int[]>();
        final AtomicBoolean cancel = new AtomicBoolean();
        final AtomicBoolean finished = new AtomicBoolean();
        final Object wait = new Object();

        synchronized (wait) {
            RunOnceFactory.add(f, new CancellableTask<CompilationInfo>() {
                private final AtomicBoolean privateCancel = new AtomicBoolean();
                public void cancel() {
                    privateCancel.set(true);
                }
                public  void run(CompilationInfo parameter) throws Exception {
                    privateCancel.set(false);
                    
                    if (cancel.get()) {
                        return;
                    }

                    result.addAll(ComputeBraces.doComputeBraces(LaTeXParserResult.get(parameter), context, cancel, privateCancel));

                    if (privateCancel.get() && !cancel.get()) {
                        RunOnceFactory.add(parameter.getFileObject(), this);
                        return;
                    }
                    
                    synchronized (wait) {
                        finished.set(true);
                        wait.notifyAll();

                    }
                }
            });

            while (!finished.get()) {
                if (MatcherContext.isTaskCanceled()) {
                    cancel.set(true);
                    break;
                }

                try {
                    wait.wait(10);
                } catch (InterruptedException ex) {
                    //
                }
            }
        }

        if (!finished.get() || cancel.get()) {
            return null;
        }
        
        return result;
    }

    public final static class FactoryImpl implements BracesMatcherFactory {

        public BracesMatcher createMatcher(MatcherContext context) {
            return new BraceMatcherImpl(context);
        }
        
    }
}
