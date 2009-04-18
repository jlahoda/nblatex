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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.ParseError;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jan Lahoda
 */
public class ErrorHints extends ParserResultTask<LaTeXParserResult> {

    public void cancel() {
    }

    public void run(final LaTeXParserResult info, SchedulerEvent evt) {
        long start = System.currentTimeMillis();
        
        final Document doc = info.getSnapshot().getSource().getDocument(false);
        
        if (doc == null) return ;
        
        LaTeXParserResult lpr = LaTeXParserResult.get(info);
        
        Map<FileObject, List<ParseError>> sortedErrors = sortErrors(lpr.getErrors());
        final FileObject file = info.getSnapshot().getSource().getFileObject();
        List<ParseError> errors = sortedErrors.get(file);
        
        if (errors == null) errors = Collections.<ParseError>emptyList();
        
        final List<ErrorDescription> editorErrors = new LinkedList<ErrorDescription>();
        
        for (final ParseError e : errors) {
            final Severity s = latex2EditorSeverity.get(e.getSeverity());
            final List<Fix> fixes = new LinkedList<Fix>();
            
            FixProvider p = code2StringProvider.get(e.getCode());
            
            if (p != null) {
                List<Fix> providedFixes = p.resolveFixes(info, e);
                
                if (providedFixes != null) {
                    fixes.addAll(providedFixes);
                }
            }

            if (fixes.isEmpty() && code2Ignore.contains(e.getCode())) {
                continue;
            }
            
            doc.render(new Runnable() {
                public void run() {
                    if (e.getEnd() == null) {
                        editorErrors.add(ErrorDescriptionFactory.createErrorDescription(s, e.getDisplayName(), fixes, doc, e.getStart().getLine() + 1));
                    } else {
                        editorErrors.add(ErrorDescriptionFactory.createErrorDescription(s, e.getDisplayName(), fixes, file, e.getStart().getOffsetValue(), e.getEnd().getOffsetValue()));
                    }
                }
            });
        }
        
        HintsController.setErrors(file, ErrorHints.class.getName(), editorErrors);
        
        long end = System.currentTimeMillis();
        
        Logger.getLogger("TIMER").log(Level.FINE, "Error Hints Processor", new Object[] {file, (end - start)});
    }

    private Map<FileObject, List<ParseError>> sortErrors(Collection<ParseError> errors) {
        Map<FileObject, List<ParseError>> result = new HashMap<FileObject, List<ParseError>>();
        
        for (ParseError err : errors) {
            FileObject file = (FileObject) err.getStart().getFile();
            List<ParseError> errs = result.get(file);
            
            if (errs == null) {
                result.put(file, errs = new ArrayList<ParseError>());
            }
            
            errs.add(err);
        }
        
        return result;
    }
    
    private static final Map<ParseError.Severity, Severity> latex2EditorSeverity;
    private static final Map<String, FixProvider> code2StringProvider;
    private static final Set<String> code2Ignore;
    
    static {
        latex2EditorSeverity = new EnumMap<ParseError.Severity, Severity>(ParseError.Severity.class);
        
        latex2EditorSeverity.put(ParseError.Severity.ERROR, Severity.ERROR);
        latex2EditorSeverity.put(ParseError.Severity.WARNING, Severity.WARNING);
        
        code2StringProvider = new HashMap<String, FixProvider>();
        
        code2StringProvider.put("unknown.command", new AddPackageFixProvider(true));
        code2StringProvider.put("unknown.environment", new AddPackageFixProvider(false));
        
        code2Ignore = new HashSet<String>();
        
        code2Ignore.add("unknown.command");
        code2Ignore.add("unknown.environment");
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
