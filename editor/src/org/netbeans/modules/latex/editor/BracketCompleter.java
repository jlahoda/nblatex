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

package org.netbeans.modules.latex.editor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.latex.lexer.TexTokenId;

/**
 *
 * @author Jan Lahoda
 */
public class BracketCompleter {

    public static int typed(Document doc, int offset, String cmd) throws BadLocationException {
        if (!bracketCompletionSettingEnabled()) {
            return -1;
        }
        
        if (ADD_BRACKET_SUPPORTED_COMMANDS.contains(cmd)) {
            return possiblyAddBrackets(doc, offset, cmd);
        }

        if (REMOVE_BRACKET_SUPPORTED_COMMANDS.contains(cmd)) {
            return possiblyRemoveBrackets(doc, offset, cmd);
        }

        return -1;
    }

    public static int possiblyAddBrackets(Document doc, int offset, String cmd) throws BadLocationException {
        TokenHierarchy th = TokenHierarchy.get(doc);

        TokenSequence<TexTokenId> ts = th.tokenSequence(TexTokenId.language());
        
        ts.move(offset);
        
        ts.movePrevious();
        
        Token<TexTokenId> t = ts.token();
        String left = null;
        String right = null;
        
        switch (t.id()) {
            case COMP_BRACKET_LEFT:
                left = "{";
                right = "}";
                break;
            case MATH:
                left = "$";
                right = "$";
                break;
            case COMMAND:
                if ("\\{".contentEquals(t.text())) {
                    left = "\\{";
                    right = "\\}";
                }
                break;
        }
        
        if (left != null && balance(ts, left, right) != 0) {
            doc.insertString(offset, right, null);
        }

        return offset;
    }

    private static int possiblyRemoveBrackets(Document doc, int offset, String cmd) throws BadLocationException {
        TokenHierarchy th = TokenHierarchy.get(doc);

        TokenSequence<TexTokenId> ts = th.tokenSequence(TexTokenId.language());

        ts.move(offset);

        ts.movePrevious();

        Token<TexTokenId> t = ts.token();
        String left = null;
        String right = null;

        switch (t.id()) {
            case COMP_BRACKET_RIGHT:
                left = "{";
                right = "}";
                break;
//            case MATH:
//                left = "$";
//                right = "$";
//                break;
            case COMMAND:
                if ("\\}".contentEquals(t.text())) {
                    left = "\\{";
                    right = "\\}";
                }
                break;
        }

        if (!ts.moveNext()) {
            return offset;
        }

        if (!right.contentEquals(ts.token().text())) {
            return offset;
        }

        if (left != null && balance(ts, left, right) < 0) {
            doc.remove(offset - left.length(), left.length());
        }

        return offset;
    }
    
    private static int balance(TokenSequence<TexTokenId> ts, String left, String right) {
        while (ts.token().id() != TexTokenId.PARAGRAPH_END && ts.movePrevious())
            ;

        if (ts.token().id() == TexTokenId.PARAGRAPH_END) {
            if (!ts.moveNext()) {
                return 0;
            }
        }
        
        boolean sameBrackets = left.equals(right);
        if (sameBrackets) right = "";
        int bal = 0;
        do {
            if (left.contentEquals(ts.token().text())) {
                bal++;
            }
            if (right.contentEquals(ts.token().text())) {
                bal--;
            }
            if (ts.token().id() == TexTokenId.PARAGRAPH_END) {
                break;
            }
        } while (ts.moveNext());
        
        return sameBrackets ? bal % 2 == 0 ? 0 : 1 : (int) Math.signum(bal);
    }
    
    private final static Set<String> ADD_BRACKET_SUPPORTED_COMMANDS = new HashSet<String>(Arrays.asList("$", "{"));
    private final static Set<String> REMOVE_BRACKET_SUPPORTED_COMMANDS = new HashSet<String>(Arrays.asList("}", "\\}"));
    
    private static boolean bracketCompletionSettingEnabled() {
        Preferences prefs = MimeLookup.getLookup(TexKit.TEX_MIME_TYPE).lookup(Preferences.class);
        return prefs.getBoolean(SimpleValueNames.COMPLETION_PAIR_CHARACTERS, false);
    }
    
}
