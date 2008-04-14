/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.BaseKit;
import org.netbeans.editor.Settings;
import org.netbeans.editor.SettingsNames;
import org.netbeans.editor.Syntax;
import org.netbeans.editor.SyntaxSupport;
import org.netbeans.modules.editor.NbEditorKit;
import org.netbeans.modules.latex.model.lexer.TexTokenId;
import org.openide.util.Exceptions;

/**
* Editor kit used to edit the plain text.
*
* @author Miloslav Metelka
* @version 1.00
*/

public class TexKit extends NbEditorKit {
    
    protected Action[] createActions() {
        Action[] texActions = new Action[] {
            new ActionsFactory.GoToDeclarationActionImpl(),
            new ActionsFactory.WordCountAction(),
            new ActionsFactory.CiteAction(ActionsFactory.CiteAction.CITE),
            new ActionsFactory.CiteAction(ActionsFactory.CiteAction.REF),
            new CommentAction("%"),
            new UncommentAction("%"),
            new ToggleCommentAction("%"),
            new WrappingDefaultKeyTypedAction(),
        };
        return TextAction.augmentList(super.createActions(), texActions);
    }

    public static final String TEX_MIME_TYPE = "text/x-tex"; // NOI18N

    public @Override String getContentType() {
//        System.err.println("TexKit getContentType.");
        return TEX_MIME_TYPE;
    }

    public @Override Syntax createSyntax(Document doc) {
        return new TexSyntax();
    }

    public @Override SyntaxSupport createSyntaxSupport(BaseDocument doc) {
        return new TexSyntaxSupport(doc);
    }

    public @Override void install(JEditorPane pane) {
        super.install(pane);
        
        //Set locale, test:
//        Locale csCZ = new Locale("cs", "CZ");
        
//        System.err.println("Setting input method: " + pane.getInputContext().selectInputMethod(csCZ));
//        pane.setLocale(csCZ);
    }
    
    public @Override void deinstall(JEditorPane pane) {
        super.deinstall(pane);
    }
    
    protected @Override void initDocument(final BaseDocument doc) {
        super.initDocument(doc);
        doc.putProperty("mime-type", TEX_MIME_TYPE);
        doc.putProperty(Language.class, TexLanguage.description());
    }

    private static final class WrappingDefaultKeyTypedAction extends ExtDefaultKeyTypedAction {

        @Override
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            String cmd = evt.getActionCommand();
            int mod = evt.getModifiers();

            if (LaTeXSettings.isHardWrapAllowed() && (target != null) && (evt != null)) {
                if (" ".equals(cmd) && (mod & ActionEvent.ALT_MASK) == 0 && (mod & ActionEvent.CTRL_MASK) == 0) {
                    try {
                        int caret = target.getCaretPosition();
                        int start = org.netbeans.editor.Utilities.getRowStart(target, caret);
                        int end = org.netbeans.editor.Utilities.getRowEnd(target, caret);
                        final BaseKit kit = org.netbeans.editor.Utilities.getKit(target);
                        Integer rightMargin = (Integer) Settings.getValue(kit.getClass(), SettingsNames.TEXT_LIMIT_WIDTH);
                        
                        if (rightMargin == null) {
                            rightMargin = 80;
                        }

                        if (end - start > rightMargin && caret == end) {
                            boolean isComment = false;
                            
                            TokenHierarchy<Document> h = TokenHierarchy.get(target.getDocument());
                            TokenSequence<TexTokenId> ts = h.tokenSequence(TexLanguage.description());
                            
                            if (ts != null) {
                                ts.move(caret);
                                
                                isComment = ts.movePrevious() && ts.token().id() == TexTokenId.COMMENT;
                            }
                            
                            ((InsertBreakAction) kit.getActionByName(insertBreakAction)).actionPerformed(evt, target);
                            
                            if (isComment) {
                                target.getDocument().insertString(target.getCaretPosition(), "%", null);
                            }
                            return ;
                        }
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
            
            super.actionPerformed(evt, target);
        }
        
    }
}
