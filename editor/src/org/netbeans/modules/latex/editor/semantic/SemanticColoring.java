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

package org.netbeans.modules.latex.editor.semantic;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.modules.gsf.api.CancellableTask;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.napi.gsfret.source.CompilationInfo;
import org.netbeans.modules.latex.editor.TexColoringNames;
import org.netbeans.modules.latex.editor.TexLanguage;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.ArgumentContainingNode;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.Command;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DocumentNode;
import org.netbeans.modules.latex.model.command.Environment;
import org.netbeans.modules.latex.model.command.MathNode;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.command.TraverseHandler;
import org.netbeans.modules.latex.model.lexer.TexTokenId;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Jan Lahoda
 */
public class SemanticColoring implements CancellableTask<CompilationInfo> {

    private static final Logger LOG = Logger.getLogger(SemanticColoring.class.getName());
    
    private AtomicBoolean cancelled = new AtomicBoolean();
    private FileObject file;
    
    public SemanticColoring(FileObject file) {
        this.file = file;
    }

    public void cancel() {
        cancelled.set(true);
    }

    public Document getDocument() {
        try {
            DataObject d = DataObject.find(file);
            EditorCookie ec = d.getLookup().lookup(EditorCookie.class);
            
            if (ec == null)
                return null;
            
            return ec.getDocument();
        } catch (IOException e) {
            Logger.getLogger(SemanticColoring.class.getName()).log(Level.FINE, "SemanticColoring: Cannot find DataObject for file: " + FileUtil.getFileDisplayName(file), e);
            return null;
        }
    }
    
    public void run(CompilationInfo parameter) throws Exception {
        cancelled.set(false);
        
        final Document document = getDocument();
        
        if (document == null) {
            return ;
        }
        
        DocumentNode dn = LaTeXParserResult.get(parameter).getDocument();
        Node rootNode = dn.getRootForFile(parameter.getFileObject());
        
        if (rootNode == null) {
            LOG.log(Level.SEVERE, "No root for: {0}", FileUtil.getFileDisplayName(parameter.getFileObject()));
            getDelegate(document).setHighlights(new OffsetsBag(null));
            return;
        }

        final OffsetsBag coloring = new OffsetsBag(document, true);
        final Map<String, int[]> possiblyUnusedLabel2Tokens = new HashMap<String, int[]>();
        final Set<String> seenLabels = new HashSet<String>();
        
        dn.traverse(new TraverseHandler() {
            public boolean commandStart(final CommandNode node) {
                if (cancelled.get()) {
                    return false;
                }
                
                if (node.getStartingPosition().getDocument() != document)
                    return false;
                
                document.render(new Runnable() {
                    public void run() {
                        try {
                            Token cmd = (Token) node.getNodeTokens().iterator().next();
                            AttributeSet coloringSet =   node.isValid()
                                                       ? getColoringForName(TexColoringNames.COMMAND_CORRECT)
                                                       : getColoringForName(TexColoringNames.COMMAND_INCORRECT);
                            coloring.addHighlight(cmd.offset(null), cmd.offset(null) + cmd.length(), coloringSet);
                        } catch (IOException e) {
                            Exceptions.printStackTrace(e);
                        }
                    }
                });
                
                return true;
            }

            public void commandEnd(CommandNode node) {}

            public boolean argumentStart(final ArgumentNode node) {
                if (cancelled.get()) {
                    return false;
                }
                
                if (node.getStartingPosition().getDocument() != document)
                    return false;
                
                AttributeSet attrs = null;
                
                if (node.getArgument().hasAttribute(Command.Param.ATTR_NO_PARSE)) {
                    attrs = getColoringForName(TexColoringNames.DEFINITION);
                } else {
                    if (node.getArgument().hasAttribute("#label")) { // NOI18N
                        String label = node.getText().toString();

                        if (!seenLabels.contains(label)) {
                            possiblyUnusedLabel2Tokens.put(label, new int[] {node.getStartingPosition().getOffsetValue(),
                                                                             node.getEndingPosition().getOffsetValue()});
                        }
                    } else {
                        if (node.getArgument().hasAttribute("#ref")) { // NOI18N
                            String label = node.getText().toString();

                            seenLabels.add(label);
                            possiblyUnusedLabel2Tokens.remove(label);
                        } else {
                            if (node.getArgument().isEnumerable()) {
                                if (node.isValidEnum()) {
                                    attrs = getColoringForName(TexColoringNames.ENUM_ARG_CORRECT);
                                } else {
                                    attrs = getColoringForName(TexColoringNames.ENUM_ARG_INCORRECT);
                                }
                            } else {
                                if (node.hasAttribute("extended-coloring-modifier")) {
                                    String modifier = node.getAttribute("extended-coloring-modifier");
                                    
                                    if (modifier.endsWith("*")) {
                                        modifier = modifier.substring(0, modifier.length() - 1);
                                    }

                                    if (captionColorings.contains(modifier)) {
                                        attrs = getColoringForName("mod-caption");
                                    }
                                } else {
                                    if (node.hasAttribute("#caption")) {
                                        attrs = getColoringForName("mod-caption");
                                    } else {
                                        if (node.hasAttribute("font-style")) {
                                            attrs = getColoringForName("mod-font-style-" + node.getAttribute("font-style"));
                                        } else {
                                            ArgumentContainingNode cnode = node.getCommand();

                                            if (cnode instanceof CommandNode && cnode.getParent() instanceof BlockNode) {
                                                BlockNode bnode = (BlockNode) cnode.getParent();
                                                Environment env = bnode.getEnvironment();

                                                if (env != null) {
                                                    attrs = getColoringForName(TexColoringNames.ENUM_ARG_CORRECT);
                                                } else {
                                                    attrs = getColoringForName(TexColoringNames.ENUM_ARG_INCORRECT);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (attrs != null) {
                    final AttributeSet attrsFin = attrs;

                    document.render(new Runnable() {
                        public void run() {
                            int start = node.getStartingPosition().getOffsetValue();
                            int end   = node.getEndingPosition().getOffsetValue();

                            TokenSequence<TexTokenId> ts = TokenHierarchy.get(document).tokenSequence(TexLanguage.description());

                            ts.move(start);

                            if (ts.moveNext() && ts.token().id() == TexTokenId.COMP_BRACKET_LEFT) {
                                start += ts.token().length();
                            }

                            ts.move(end);

                            if (ts.movePrevious() && ts.token().id() == TexTokenId.COMP_BRACKET_RIGHT) {
                                end -= ts.token().length();
                            }

                            if (start < end) { //hmmmmm
                                coloring.addHighlight(start, end, attrsFin);
                            }
                        }
                    });
                }
                
                return true;
            }

            public void argumentEnd(ArgumentNode node) {}

            public boolean blockStart(BlockNode node) {
                if (cancelled.get()) {
                    return false;
                }

                return node.getStartingPosition().getDocument() == document;
            }
            
            public void blockEnd(BlockNode node) {}

            @Override
            public boolean mathStart(final MathNode node) {
                if (cancelled.get()) {
                    return false;
                }
                
                if (node.getStartingPosition().getDocument() != document)
                    return false;
                
                final AttributeSet attrs = getColoringForName(TexColoringNames.MATH);
                
                document.render(new Runnable() {
                    public void run() {
                        coloring.addHighlight(node.getStartingPosition().getOffsetValue(),
                                              node.getEndingPosition().getOffsetValue(), attrs);
                    }
                });
                
                return true;
            }
        });

        final AttributeSet unused = AttributesUtilities.createImmutable(getColoringForName(TexColoringNames.UNUSED), AttributesUtilities.createImmutable("unused-browseable", Boolean.TRUE)); // NOI18N

        document.render(new Runnable() {
            public void run() {
                for (int[] span : possiblyUnusedLabel2Tokens.values()) {
                    coloring.addHighlight(span[0], span[1], unused);
                }
            }
        });

        if (cancelled.get())
            return ;

        getDelegate(document).setHighlights(coloring);
    }

    private AttributeSet getColoringForName(String name) {
        FontColorSettings fontColorSettings = MimeLookup.getLookup(MimePath.get("text/x-tex")).lookup(FontColorSettings.class);
        
        return fontColorSettings.getTokenFontColors(name);
    }
    
    private static final Set<String> captionColorings = new HashSet<String>(Arrays.asList(
        "mod-ext-part",
        "mod-ext-chapter",
        "mod-ext-section",
        "mod-ext-subsection",
        "mod-ext-subsubsection",
        "mod-ext-paragraph",
        "mod-ext-subparagraph"
    ));

    public static OffsetsBag getDelegate(Document doc) {
        OffsetsBag bag = (OffsetsBag) doc.getProperty(SemanticColoring.class);
        
        if (bag == null) {
            doc.putProperty(SemanticColoring.class, bag = new OffsetsBag(doc));
        }
        
        return bag;
    }
}
