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

import org.netbeans.modules.latex.lexer.TexTokenId;
import org.netbeans.modules.latex.lexer.impl.TexLanguage;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import junit.framework.TestCase;
import org.netbeans.api.lexer.Language;

/**
 *
 * @author Jan Lahoda
 */
public class BracketCompleterTest extends TestCase {
    
    public BracketCompleterTest(String testName) {
        super(testName);
    }            

    public void testAddedBracket1() throws BadLocationException {
        doTestBrackets("{|", "{|}");
    }

    public void testAddedBracket2() throws BadLocationException {
        doTestBrackets("{|}", "{|}");
    }

    public void testAddedBracket3() throws BadLocationException {
        doTestBrackets("\\{|", "\\{|\\}");
    }

    public void testAddedBracket4() throws BadLocationException {
        doTestBrackets("\\{|\\}", "\\{|\\}");
    }

    public void testAddedBracket5() throws BadLocationException {
        doTestBrackets("$|", "$|$");
    }

    public void testAddedBracket6() throws BadLocationException {
        doTestBrackets("{{|}", "{{|}}");
    }

    public void testRemoveOverBalanced1() throws BadLocationException {
        doTestBrackets("{}|}", "{}|");
    }

    public void testRemoveOverBalanced2() throws BadLocationException {
        doTestBrackets("\\{\\}|\\}", "\\{\\}|");
    }

    public void testDoNotRemoveUnBalanced1() throws BadLocationException {
        doTestBrackets("{{}}|", "{{}}|");
    }

    public void testDoNotRemoveUnBalanced2() throws BadLocationException {
        doTestBrackets("a\n\n{{}}|", "a\n\n{{}}|");
    }

    public void testDoNotRemoveUnBalanced3() throws BadLocationException {
        doTestBrackets("a\n\n{{}}}|", "a\n\n{{}}}|");
    }

    private void doTestBrackets(String text, String golden) throws BadLocationException {
        int offset = text.indexOf('|');
        
        text = text.replace("|", "");

        int targetCaret = golden.indexOf('|');

        golden = golden.replace("|", "");
        
        Document doc = new PlainDocument();

        doc.putProperty(Language.class, TexTokenId.language());
        doc.insertString(0, text, null);

        assertEquals(targetCaret, BracketCompleter.typed(doc, offset, "" + text.charAt(offset - 1)));

        assertEquals(golden, doc.getText(0, doc.getLength()));
    }

}
