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
package org.netbeans.modules.latex.editor.spellchecker;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.modules.gsf.api.CancellableTask;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.napi.gsfret.source.CompilationInfo;
import org.netbeans.napi.gsfret.source.Phase;
import org.netbeans.napi.gsfret.source.Source.Priority;
import org.netbeans.napi.gsfret.source.support.EditorAwareSourceTaskFactory;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.modules.latex.model.command.ArgumentContainingNode;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DefaultTraverseHandler;
import org.netbeans.modules.latex.model.command.InputNode;
import org.netbeans.modules.latex.model.command.MathNode;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.command.TextNode;
import org.netbeans.modules.latex.model.lexer.TexTokenId;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.WeakSet;

/**
 *
 * @author Jan Lahoda
 */
public class SpellcheckerDataCollector implements CancellableTask<CompilationInfo> {

    private static final Logger LOG = Logger.getLogger(SpellcheckerDataCollector.class.getName());
    private AtomicBoolean cancel = new AtomicBoolean();
    
    public void cancel() {
        cancel.set(true);
        LOG.fine("cancelled");
    }

    public void run(CompilationInfo parameter) throws Exception {
        cancel.set(false);
        
        long startTime = System.currentTimeMillis();
        
        try {
            LaTeXParserResult lpr = LaTeXParserResult.get(parameter);
            Document doc = parameter.getDocument();
            Node rootNode = lpr.getDocument().getRootForFile(parameter.getFileObject());

            if (rootNode == null) {
                LOG.log(Level.SEVERE, "No root for: {0}", FileUtil.getFileDisplayName(parameter.getFileObject()));
                LaTeXTokenListProvider.findTokenListImpl(doc).setAcceptedTokens(Collections.<Token>emptySet());
                return ;
            }

            Set<Token> acceptedTokens = getAcceptedTokens(doc, cancel, rootNode);

            if (cancel.get()) {
                return ;
            }
            
            LaTeXTokenListProvider.findTokenListImpl(doc).setAcceptedTokens(acceptedTokens);
        } finally {
            long endTime = System.currentTimeMillis();
            
            Logger.getLogger("TIMER").log(Level.FINE, "Spellchecker Data Collector", new Object[] {parameter.getFileObject(), endTime - startTime});
        }
    }

    static Set<Token> getAcceptedTokens(Document doc, AtomicBoolean cancel, Node root) {
        VisitorImpl vi = new VisitorImpl(cancel, doc);

        root.traverse(vi);

        return vi.acceptedTokens;
    }

    private static final class VisitorImpl extends DefaultTraverseHandler {

        private final AtomicBoolean cancel;
        private final Document doc;
        private final Set<Token> acceptedTokens = new WeakSet<Token>();
        private TokenSequence ts;

        public VisitorImpl(AtomicBoolean cancel, Document doc) {
            this.cancel = cancel;
            this.doc = doc;

            this.doc.render(new Runnable() {
                public void run() {
                    ts = TokenHierarchy.get(VisitorImpl.this.doc).tokenSequence();
                    ts.moveNext();
                }
            });
        }
        
        @Override
        public boolean argumentStart(ArgumentNode node) {
            if (node.getArgument().isEnumerable()) {
                return false;
            } else {
                ArgumentContainingNode cnode = node.getCommand();

                if (cnode instanceof CommandNode && cnode.getParent() instanceof BlockNode) {
                    return false;
                } else {
                    return !node.getArgument().isCodeLike() && !cancel.get();
                }
            }
        }

        @Override
        public boolean mathStart(MathNode node) {
            return false;
        }

        @Override
        public boolean textStart(final TextNode node) {
            moveToOffset(node.getStartingPosition().getOffsetValue());

            Iterable<Node> children = getChildren(node);
            final AtomicBoolean end = new AtomicBoolean();

            while (getOffset() < node.getEndingPosition().getOffsetValue() && !cancel.get() && !end.get()) {
                Node c = findChild(node.getStartingPosition().getFile(), getOffset(), cancel, children);

                if (c == null) {
                    doc.render(new Runnable() {
                        public void run() {
                            if (!cancel.get()) {
                                Token token = ts.token();

                                if (token.id() == TexTokenId.WORD) {
                                    acceptedTokens.add(token);
                                }

                                end.set(!ts.moveNext());
                            }
                        }
                    });
                } else {
                    c.traverse(this);

                    moveToOffset(c.getEndingPosition().getOffsetValue());
                }
            }
            
            return false;
        }

        @Override
        public boolean commandStart(CommandNode node) {
            return!(node instanceof InputNode);
        }

        private void moveToOffset(final int offset) {
            doc.render(new Runnable() {
                public void run() {
                    while (ts.offset() < offset && !cancel.get() && ts.moveNext())
                        ;
                }
            });
        }
        
        private int getOffset() {
            final int[] result = new int[1];
            doc.render(new Runnable() {
                public void run() {
                    if (!cancel.get()) {
                        result[0] = ts.offset();
                    }
                }
            });

            return result[0];
        }
        
    }
    
    private static TokenSequence<TexTokenId> getTS(Object file) throws IOException {
        Document doc = Utilities.getDefault().openDocument(file);

        if (doc == null) {
            throw new IllegalStateException();
        }

        TokenHierarchy h = TokenHierarchy.get(doc);
        
        @SuppressWarnings("unchecked")
        TokenSequence<TexTokenId> ts = h.tokenSequence();

        return ts;
    }
    
    private static boolean isIn(Object file, int pos, Node node) {
        assert Utilities.getDefault().compareFiles(node.getStartingPosition().getFile(), node.getEndingPosition().getFile());
        
        if (!Utilities.getDefault().compareFiles(file, node.getStartingPosition().getFile()))
            return false;
        
        return node.getStartingPosition().getOffsetValue() <= pos && pos < node.getEndingPosition().getOffsetValue();
    }
    
    private static Node findChild(Object file, int pos, AtomicBoolean cancel, Iterable<Node> children) {
        for (Node n : children) {
            if (cancel.get()) {
                return null;
            }
            if (isIn(file, pos, n)) {
                return n;
            }
        }
        
        return null;
    }

    public static Iterable<Node> getChildren(Node n) {
        final class FindChildren extends DefaultTraverseHandler {
            private boolean secondLevel;
            private List<Node> children = new LinkedList<Node>();

            @Override
            public boolean commandStart(CommandNode node) {
                if (secondLevel)
                    children.add(node);
                return secondLevel = !secondLevel;
            }

            @Override
            public boolean argumentStart(ArgumentNode node) {
                if (secondLevel)
                    children.add(node);
                return secondLevel = !secondLevel;
            }

            @Override
            public boolean blockStart(BlockNode node) {
                if (secondLevel)
                    children.add(node);
                return secondLevel = !secondLevel;
            }

            @Override
            public boolean textStart(TextNode node) {
                if (secondLevel)
                    children.add(node);
                return secondLevel = !secondLevel;
            }

            @Override
            public boolean mathStart(MathNode node) {
                if (secondLevel)
                    children.add(node);
                return secondLevel = !secondLevel;
            }
        }

        FindChildren fc = new FindChildren();
        
        n.traverse(fc);

        return fc.children;
    }
    
    public static final class Factory extends EditorAwareSourceTaskFactory {

        public Factory() {
            super(Phase.RESOLVED, Priority.NORMAL);
        }
        
        protected CancellableTask<CompilationInfo> createTask(FileObject file) {
            return new SpellcheckerDataCollector();
        }
        
    }
    
}
