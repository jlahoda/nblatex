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
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.latex.editor.LaTeXSettings;
import org.netbeans.modules.latex.hints.HintProvider.Data;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.modules.latex.model.bibtex.PublicationEntry;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DocumentNode;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.lexer.TexTokenId;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jan Lahoda
 */
public class CiteFormatHint implements HintProvider<Void> {

    public boolean accept(LaTeXParserResult lpr, Node n) {
        return n instanceof ArgumentNode && n.hasAttribute("#cite");
    }

    public List<ErrorDescription> computeHints(LaTeXParserResult lpr, Node n, Data<Void> providerPrivateData) throws Exception {
        if (!LaTeXSettings.isCiteFormatHintEnabled()) {
            return Collections.emptyList();
        }
        
        ArgumentNode anode = (ArgumentNode) n;
        String       nodeValue = lpr.getCommandUtilities().getArgumentValue(anode).toString();
        List<ErrorDescription> res = new  LinkedList<ErrorDescription>();
        FileObject file = (FileObject) anode.getStartingPosition().getFile();

        if (!lpr.getSnapshot().getSource().getFileObject().equals(file)) {
            return res;
        }

        CommandNode cnode = (CommandNode) anode.getCommand();
        int startOffset = cnode.getStartingPosition().getOffsetValue();
        int endOffset = cnode.getEndingPosition().getOffsetValue();

        if (isSuppressed(lpr, endOffset, "NO-CITE-FORMAT")) {
            return res;
        }

        List<String> authors = new LinkedList<String>();

        for (String c : nodeValue.split(",")) {
            c = c.trim();
            
            PublicationEntry entry = null;
            
            for (PublicationEntry ee : Utilities.getDefault().getAllBibReferences(lpr)) {
                if (c.equals(ee.getTag())) {
                    entry = ee;
                    break;
                }
            }

            if (entry == null) {
                return res;
            }

            String a = entry.getAuthor();

            if (a == null) {
                return res;
            }
            
            authors.add(a);
        }

        String correctText = null;

        for (String currentAuthors : authors) {
            List<String> authorsNames = new LinkedList<String>();

            for (String ap : currentAuthors.split("and")) {
                String author;

                if (ap.contains(",")) {
                    author = ap.split(",")[0].trim();
                } else {
                    String[] sp = ap.trim().split(" ");

                    author = sp[sp.length - 1];
                }

                authorsNames.add(author);
            }

            StringBuilder authorsText = new StringBuilder();

            if (authorsNames.size() > 2) {
                authorsText.append(authorsNames.get(0));
                authorsText.append(" et al.");
            } else {
                authorsText.append(authorsNames.get(0));

                if (authorsNames.size() == 2) {
                    authorsText.append(" and ");
                    authorsText.append(authorsNames.get(1));
                }
            }

            authorsText.append("~");

            String authorsString = authorsText.toString();

            if (correctText == null) {
                correctText = authorsString;
            } else {
                if (!correctText.equals(authorsString)) {
                    res.add(ErrorDescriptionFactory.createErrorDescription(Severity.WARNING, "Different authors", file, startOffset, endOffset));
                    
                    return res;
                }
            }
        }

        String realText = lpr.getSnapshot().getText().subSequence(startOffset - correctText.length(), startOffset).toString(); //XXX: whitespaces!

        if (!realText.equals(correctText.toString())) {
            res.add(ErrorDescriptionFactory.createErrorDescription(Severity.WARNING, "Incorrect format of a citation", file, startOffset, endOffset));
        }

        return res;
    }

    private static boolean isSuppressed(LaTeXParserResult lpr, int offset, String key) {
        TokenSequence<TexTokenId> ts = (TokenSequence<TexTokenId>) lpr.getSnapshot().getTokenHierarchy().tokenSequence();

        ts.move(offset);

        while (ts.moveNext()) {
            String text = ts.token().text().toString();

            if (ts.token().id() == TexTokenId.COMMENT) {
                if (text.contains(key)) {
                    return true;
                }
            }

            if (text.contains("\n")) {
                return false;
            }
        }

        return false;
    }

    public List<ErrorDescription> scanningFinished(LaTeXParserResult lpr, DocumentNode dn, Data<Void> providerPrivateData) throws Exception {
        return null;
    }
    
}
