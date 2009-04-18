/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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

package org.netbeans.modules.latex.hints;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.modules.latex.hints.HintProvider.Data;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DefaultTraverseHandler;
import org.netbeans.modules.latex.model.command.DocumentNode;
import org.netbeans.modules.latex.model.command.MathNode;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.command.TextNode;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.HintsController;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Jan Lahoda
 */
public class HintsProcessor extends ParserResultTask<LaTeXParserResult> {

    private AtomicBoolean cancel = new AtomicBoolean();
    
    public void cancel() {
    }

    public void run(final LaTeXParserResult lpr, SchedulerEvent evt) {
        long start = System.currentTimeMillis();

        try {
            List<ErrorDescription> hints = compute(lpr, providers, cancel);

            if (hints == null) {
                hints = Collections.<ErrorDescription>emptyList();
            }

            final FileObject file = lpr.getSnapshot().getSource().getFileObject();

            HintsController.setErrors(file,HintsProcessor.class.getName(), hints);

            long end = System.currentTimeMillis();

            Logger.getLogger("TIMER").log(Level.FINE, "Hints Processor", new Object[] {file, (end - start)});
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }
    
    static List<ErrorDescription> compute(final LaTeXParserResult info, final List<HintProvider> providers, final AtomicBoolean cancel) throws Exception {
        final Document doc = info.getSnapshot().getSource().getDocument(false);

        if (doc == null) {
            return null;
        }

        final List<ErrorDescription> hints = new LinkedList<ErrorDescription>();
        LaTeXParserResult lpr = LaTeXParserResult.get(info);
        final Map<Class, Data<?>> providerPrivateData = new HashMap<Class, Data<?>>();
        
        lpr.getDocument().traverse(new DefaultTraverseHandler() {
            @Override
            public boolean commandStart(CommandNode node) {
                if (cancel.get()) {
                    return false;
                }
                
                handleNode(info, providers, hints, node, providerPrivateData);
                
                return !cancel.get();
            }
            @Override
            public boolean argumentStart(ArgumentNode node) {
                if (cancel.get()) {
                    return false;
                }

                handleNode(info, providers, hints, node, providerPrivateData);
                
                return !cancel.get();
            }
            @Override
            public boolean blockStart(BlockNode node) {
                if (cancel.get()) {
                    return false;
                }

                handleNode(info, providers, hints, node, providerPrivateData);
                
                return !cancel.get();
            }
            @Override
            public boolean textStart(TextNode node) {
                if (cancel.get()) {
                    return false;
                }

                handleNode(info, providers, hints, node, providerPrivateData);
                
                return !cancel.get();
            }
            @Override
            public boolean mathStart(MathNode node) {
                if (cancel.get()) {
                    return false;
                }

                handleNode(info, providers, hints, node, providerPrivateData);
                
                return !cancel.get();
            }
        });
        
        handleFinished(info, providers, hints, lpr.getDocument(), providerPrivateData);
        
        return hints;
    }

    private static List<HintProvider> providers;
    
    static {
        providers = new LinkedList<HintProvider>();
        
        providers.add(new CheckCountersHint());
        providers.add(new UnknownCiteHint());
        providers.add(new CiteFormatHint());
        providers.add(new UnbalancedBrackets());
    }
        
    static void handleNode(LaTeXParserResult lpr, List<HintProvider> providers, List<ErrorDescription> hints, Node n, Map<Class, Data<?>> providerPrivateData) {
        for (HintProvider<?> p : providers) {
            if (p.accept(lpr, n)) {
                Data d = providerPrivateData.get(p.getClass());
                
                if (d == null) {
                    providerPrivateData.put(p.getClass(), d = new Data());
                }
                
                try {
                    @SuppressWarnings("unchecked")
                    List<ErrorDescription> h = p.computeHints(lpr, n, d);

                    if (h != null) {
                        for (ErrorDescription ed : h) {
                            if (lpr.getSnapshot().getSource().getFileObject().equals(ed.getFile())) {
                                hints.add(ed);
                            }
                        }
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
    
    static void handleFinished(LaTeXParserResult info, List<HintProvider> providers, List<ErrorDescription> hints, DocumentNode dn, Map<Class, Data<?>> providerPrivateData) {
        for (HintProvider<?> p : providers) {
            Data d = providerPrivateData.get(p.getClass());

            if (d == null) {
                providerPrivateData.put(p.getClass(), d = new Data());
            }

            try {
                @SuppressWarnings("unchecked")
                List<ErrorDescription> h = p.scanningFinished(info, dn, d);

                if (h != null) {
                    for (ErrorDescription ed : h) {
                        if (info.getSnapshot().getSource().getFileObject().equals(ed.getFile())) {
                            hints.add(ed);
                        }
                    }
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

}
