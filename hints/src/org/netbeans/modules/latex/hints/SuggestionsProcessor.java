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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.modules.latex.hints.HintProvider.Data;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.parsing.spi.CursorMovedSchedulerEvent;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.HintsController;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Jan Lahoda
 */
public class SuggestionsProcessor extends ParserResultTask<LaTeXParserResult> {

    public void cancel() {
    }

    public void run(final LaTeXParserResult info, SchedulerEvent evt) {
        try {
            long start = System.currentTimeMillis();

            List<ErrorDescription> suggestions = compute(info, evt);

            if (suggestions == null) {
                suggestions = Collections.<ErrorDescription>emptyList();
            }

            FileObject file = info.getSnapshot().getSource().getFileObject();

            HintsController.setErrors(file, SuggestionsProcessor.class.getName(), suggestions);

            long end = System.currentTimeMillis();

            Logger.getLogger("TIMER").log(Level.FINE, "Suggestions Processor", new Object[] {file, (end - start)});
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private List<ErrorDescription> compute(LaTeXParserResult info, SchedulerEvent evt) throws Exception {
        final Document doc = info.getSnapshot().getSource().getDocument(false);

        if (doc == null) {
            return null;
        }

        LaTeXParserResult lpr = LaTeXParserResult.get(info);
        
        Node n = lpr.getCommandUtilities().findNode(doc, ((CursorMovedSchedulerEvent) evt).getCaretOffset());
        
        if (n == null) {
            return null;
        }
        
        final List<ErrorDescription> hints = new LinkedList<ErrorDescription>();
        final Map<Class, Data<?>> providerPrivateData = new HashMap<Class, Data<?>>();
        
        while (n != null) {
            HintsProcessor.handleNode(info, providers, hints, n, providerPrivateData);
            
            n = n.getParent();
        }
        
        HintsProcessor.handleFinished(info, providers, hints, lpr.getDocument(), providerPrivateData);
        
        return hints;
    }

    private static List<HintProvider> providers;
    
    static {
        providers = new LinkedList<HintProvider>();
        
        providers.add(new AddItemHint());
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.CURSOR_SENSITIVE_TASK_SCHEDULER;
    }
        
}
