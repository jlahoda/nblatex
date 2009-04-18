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
 * Software is Sun Microsystems, Inc. Portions Copyright 2007-2009 Sun
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.ParseError;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.CommandPackage;
import org.netbeans.modules.latex.model.command.DefaultTraverseHandler;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.text.NbDocument;

/**
 *
 * @author Jan Lahoda
 */
public class AddPackageFixProvider implements FixProvider {

    private final boolean command;

    public AddPackageFixProvider(boolean command) {
        this.command = command;
    }
    
    public List<Fix> resolveFixes(LaTeXParserResult lpr, ParseError error) {
        List<Fix> result = new LinkedList<Fix>();
        Source s = Source.create(lpr.getMainFile());
        
        if (s == null) {
            return result;
        }
        
        for (String packageName : (Collection<String>) CommandPackage.getKnownPackages()) {
            CommandPackage pack = CommandPackage.getCommandPackageForName(packageName);
            if (getSpecificationMap(pack).get(error.getParameters()[0]) != null) {
                result.add(new FixImpl(s, pack));
            }
        }
        
        return result;
    }
    
    private Map<String, ?> getSpecificationMap(CommandPackage p) {
        if (command)
            return p.getCommands();
        else
            return p.getEnvironments();
    }

    private class FixImpl implements Fix {
        private Source s;
        private CommandPackage pack;

        public FixImpl(Source s, CommandPackage pack) {
            this.s = s;
            this.pack = pack;
        }

        public String getText() {
            return "Add \\usepackage{" + pack.getName() + "}";
        }

        public ChangeInfo implement() throws Exception {
            ParserManager.parse(Collections.singleton(s), new UserTask() {
                public void run(final ResultIterator parameter) throws Exception {
                    final StyledDocument doc = (StyledDocument) parameter.getSnapshot().getSource().getDocument(true);
                    final LaTeXParserResult lpr = LaTeXParserResult.get(parameter);

                    NbDocument.runAtomic(doc, new Runnable() {
                        public void run() {
                            try {
                                doAddUsePackage(lpr, doc);
                            } catch (BadLocationException ex) {
                                throw new IllegalStateException(ex);
                            }
                        }
                    });
                }
            });
            return null;
        }

        private void doAddUsePackage(LaTeXParserResult lpr, Document doc) throws BadLocationException {
            final CommandNode[] documentClass = new CommandNode[1];
            final List<CommandNode> usePackage = new LinkedList<CommandNode>();

            lpr.getDocument().traverse(new DefaultTraverseHandler() {
                @Override
                public boolean commandStart(CommandNode node) {
                    if (node.getCommand().hasAttribute("#usepackage-command")) {
                        usePackage.add(node);
                    }
                    return true;
                }
                @Override
                public boolean argumentStart(ArgumentNode node) {
                    if (node.getArgument().hasAttribute("#documentclass")) {
                        documentClass[0] = (CommandNode) node.getCommand();
                    }
                    return false;
                }
                @Override
                public boolean blockStart(BlockNode node) {
                    return false;
                }
            });

            int pos;
            boolean before = false;

            if (usePackage.isEmpty()) {
                pos = documentClass[0].getEndingPosition().getOffsetValue();
            } else {
                List<String> packageName = new LinkedList<String>();

                for (CommandNode n : usePackage) {
                    packageName.add(n.getArgument(1).getText().toString());
                }

                int insertIndex = 0;

                while (insertIndex < packageName.size() && pack.getName().compareTo(packageName.get(insertIndex)) > 0) {
                    insertIndex++;
                }

                if (pack.getName().compareTo(packageName.get(packageName.size() - 1)) < 0) {
                    before = true;
    //                            insertIndex = insertIndex > 0 ? insertIndex - 1 : 0;
                    pos = usePackage.get(insertIndex).getStartingPosition().getOffsetValue();
                } else {
                    pos = usePackage.get(usePackage.size() - 1).getEndingPosition().getOffsetValue();
                }
            }

            doc.insertString(pos, (!before ? "\n" : "") + "\\usepackage{" + pack.getName() + "}" + (before ? "\n" : ""), null);
        }
    }
}
