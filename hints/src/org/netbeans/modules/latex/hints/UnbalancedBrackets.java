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
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import org.netbeans.modules.latex.hints.HintProvider.Data;
import org.netbeans.modules.latex.hints.UnbalancedBrackets.Pair;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DocumentNode;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jan Lahoda
 */
public class UnbalancedBrackets implements HintProvider<Stack<Pair>> {

    private static final String UNBALANCED_BRACKET = "Unbalanced opening bracket";
    private static final String TRAILING_BRACKET = "Trailing closing bracket";
    
    public boolean accept(LaTeXParserResult lpr, Node n) {
        String bracket = n.getAttribute("bracket");
        
        return "open".equals(bracket) || "close".equals(bracket);
    }

    public List<ErrorDescription> computeHints(LaTeXParserResult lpr, Node n, Data<Stack<Pair>> providerPrivateData) throws Exception {
        Stack<Pair> seenBrackets = providerPrivateData.getValue();
        
        if (seenBrackets == null) {
            providerPrivateData.setValue(seenBrackets = new Stack<Pair>());
        }
        
        String bracket = n.getAttribute("bracket");
        
        if ("open".equals(bracket)) {
            seenBrackets.push(new Pair(getBracketType(n), n));
            return null;
        }
        
        if ("close".equals(bracket)) {
            if (seenBrackets.isEmpty()) {
                return Collections.singletonList(computeError(n, TRAILING_BRACKET));
            }
            
            Pair p = seenBrackets.pop();
            
            if (!getBracketType(n).equals(p.type)) {
                return Collections.singletonList(computeError(p.n, UNBALANCED_BRACKET));
            }
            
            return null;
        }
        
        return null;
    }
    
    private String getBracketType(Node n) {
        return n.getAttribute("bracket-type");
    }
    
    private ErrorDescription computeError(Node n, String message) {
        int start;
        int end;
        if (n instanceof CommandNode) {
            start = n.getStartingPosition().getOffsetValue();
            end = start + ((CommandNode) n).getCommand().getCommand().length();
        } else {
            start = n.getStartingPosition().getOffsetValue();
            end = n.getEndingPosition().getOffsetValue();
        }
        
        return ErrorDescriptionFactory.createErrorDescription(Severity.WARNING, message, (FileObject) n.getStartingPosition().getFile(), start, end);
    }

    public List<ErrorDescription> scanningFinished(LaTeXParserResult lpr, DocumentNode dn, Data<Stack<Pair>> providerPrivateData) throws Exception {
        Stack<Pair> seenBrackets = providerPrivateData.getValue();
        
        if (seenBrackets == null) {
            return null;
        }

        List<ErrorDescription> result = new LinkedList<ErrorDescription>();
        
        for (Pair p : seenBrackets) {
            result.add(computeError(p.n, UNBALANCED_BRACKET));
        }
        
        return result;
    }

    public static final class Pair {
        final String type;
        final Node n;

        public Pair(String type, Node n) {
            this.type = type;
            this.n = n;
        }
        
    }
}
