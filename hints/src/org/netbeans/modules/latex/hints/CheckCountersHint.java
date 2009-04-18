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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.Command;
import org.netbeans.modules.latex.model.command.CommandPackage;
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
public class CheckCountersHint implements HintProvider {

    public boolean accept(LaTeXParserResult lpr, Node n) {
        if (n instanceof ArgumentNode) {
            ArgumentNode an = (ArgumentNode) n;
            
            if (!an.isPresent()) {
                return false;
            }
            
            return    an.hasAttribute("new-counter")
                   || an.hasAttribute("existing-counter")
                   || an.hasAttribute("#documentclass")
                   || an.hasAttribute("#package");
        }
        
        return false;
    }

    public List computeHints(LaTeXParserResult lpr, Node n, Data providerPrivateData) throws Exception {
        ArgumentNode an = (ArgumentNode) n;
        
        if (an.hasAttribute("new-counter")) {
            getExistingCounters(lpr).add(getArgumentValue(an).toString());
            return null;
        }
        
        if (an.hasAttribute("existing-counter")) {
            if (!getExistingCounters(lpr).contains(getArgumentValue(an).toString())) {
                ErrorDescription e = ErrorDescriptionFactory.createErrorDescription(Severity.WARNING,
                        "Undefined counter", (FileObject) n.getStartingPosition().getFile(),
                        n.getStartingPosition().getOffsetValue(), n.getEndingPosition().getOffsetValue());
                
                return Collections.singletonList(e);
            }
        }
        
        if (an.hasAttribute("#documentclass")) {
            String name = getArgumentValue(an).toString();
            
            CommandPackage cp = CommandPackage.getCommandPackageForName(name);
            
            if (cp == null) {
                cp = CommandPackage.getDefaultDocumentClass();
            }
            
            getExistingCounters(lpr).addAll(cp.getCounters());
            
            return null;
        }
        
        if (an.hasAttribute("#package")) {
            String name = getArgumentValue(an).toString();
            
            CommandPackage cp = CommandPackage.getCommandPackageForName(name);
            
            if (cp != null) {
                getExistingCounters(lpr).addAll(cp.getCounters());
            }
            
            return null;
        }
        
        return null;
    }
    
    private Set<String> getExistingCounters(LaTeXParserResult lpr) {
        Set<String> existing = lpr2ExistingCounters.get(lpr);
        
        if (existing == null) {
            lpr2ExistingCounters.put(lpr, existing = new HashSet<String>());
        }
        
        return existing;
    }
    
    private Map<LaTeXParserResult, Set<String>> lpr2ExistingCounters = new WeakHashMap<LaTeXParserResult, Set<String>>();

    //XXX:
    private static CharSequence getArgumentValue(ArgumentNode an) {
        CharSequence argumentText = an.getFullText();
        
        switch (an.getArgument().getType()) {
            case Command.Param.MANDATORY:
                return removeBrackets(argumentText, '{', '}');
            case Command.Param.NONMANDATORY:
                return removeBrackets(argumentText, '[', ']');
            case Command.Param.FREE:
                return argumentText;
            case Command.Param.SPECIAL:
                //do not know what to do with this:
                return argumentText;
            default:
                throw new IllegalArgumentException("Unknown argument type:" + an.getArgument().getType());
        }
    }
    
    private static CharSequence removeBrackets(CharSequence text, char open, char close) {
        CharSequence result = text;
        
        if (result.length() > 0 && result.charAt(0) == open) {
            result = result.subSequence(1, result.length());
        }
        
        if (result.length() > 0 && result.charAt(result.length() - 1) == close) {
            result = result.subSequence(0, result.length() - 1);
        }
        
        return result;
    }

    public List scanningFinished(LaTeXParserResult lpr, DocumentNode dn, Data providerPrivateData) throws Exception {
        return null;
    }
    
}
