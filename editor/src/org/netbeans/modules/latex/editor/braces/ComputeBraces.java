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

package org.netbeans.modules.latex.editor.braces;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DocumentNode;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.command.TextNode;
import org.netbeans.modules.latex.model.command.TraverseHandler;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;
import org.openide.util.Exceptions;

/**
 *
 * @author Jan Lahoda
 */
public class ComputeBraces {

    private ComputeBraces() {}

    public static List<int[]> doComputeBraces(LaTeXParserResult lpr, MatcherContext context, AtomicBoolean cancel, AtomicBoolean privateCancel) {
        try {
            return doComputeBracesImpl(lpr, context, cancel, privateCancel);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    private static List<int[]> doComputeBracesImpl(LaTeXParserResult lpr, MatcherContext context, AtomicBoolean cancel, AtomicBoolean privateCancel) throws IOException {
        if (privateCancel.get() || cancel.get()) {
            return null;
        }

        Node n = lpr.getCommandUtilities().findNode(context.getDocument(), context.getSearchOffset());

        while (!(n instanceof DocumentNode)) {
            if (privateCancel.get() || cancel.get()) {
                return null;
            }

            List<int[]> result = handleBlocks(n);

            if (result != null) {
                return result;
            }

            result = handleBracketCommands(lpr, n);
            
            if (result != null) {
                return result;
            }
            
            n = n.getParent();
        }

        return Collections.emptyList();
    }
    
    private static List<int[]> handleBlocks(Node n) {
        if (n instanceof CommandNode && n.getParent() instanceof BlockNode) {
            BlockNode bn = (BlockNode) n.getParent();

            if (bn.getBeginCommand() == n) {
                List<int[]> result = new LinkedList<int[]>();

                result.add(span(bn.getBeginCommand()));
                if (bn.getEndCommand() != null) {
                    result.add(span(bn.getEndCommand()));
                }

                return result;
            }
            if (bn.getEndCommand() == n) {
                List<int[]> result = new LinkedList<int[]>();

                result.add(span(bn.getEndCommand()));
                if (bn.getBeginCommand() != null) {
                    result.add(span(bn.getBeginCommand()));
                }

                return result;
            }
        }

        return null;
    }

    private static List<int[]> handleBracketCommands(LaTeXParserResult lpr, Node n) {
        if (n instanceof CommandNode && isBracketCommand(lpr, n)) {
            List<int[]> result = new LinkedList<int[]>();

            for (CommandNode f : matchingBrackets(lpr, (CommandNode) n)) {
                result.add(span(f));
            }

            return result;
        }

        return null;
    }

    private static int[] span(Node n) {
        if (n instanceof CommandNode && "command-name".equals(n.getAttribute("bracket-span"))) {
            CommandNode f = (CommandNode) n;
            return new int[] {
                f.getStartingPosition().getOffsetValue(),
                f.getStartingPosition().getOffsetValue() + f.getCommand().getCommand().length()
            };
        }
        return new int[] {
            n.getStartingPosition().getOffsetValue(),
            n.getEndingPosition().getOffsetValue(),
        };
    }

    private static boolean isBracketCommand(LaTeXParserResult lpr, Node cnode) {
        return cnode.hasAttribute("bracket-polarity");
    }

    private static List<CommandNode> matchingBrackets(LaTeXParserResult lpr, CommandNode cnode) {
        String kind = cnode.getAttribute("bracket-type");

        if (cnode.getParent() instanceof TextNode) {
            TextNode parent = (TextNode) cnode.getParent();

            while (parent.getParent() instanceof TextNode) {
                parent = (TextNode) parent.getParent();
            }
            Stack<CommandNode> s = new Stack<CommandNode>();

            FindFirstLevelCommands c = new FindFirstLevelCommands();

            parent.traverse(c);

            for (CommandNode n : c.commands) {
                if (!kind.equals(n.getAttribute("bracket-type")))
                    continue;
                
                String attr = n.getAttribute("bracket-polarity");
                BracketPolarity p = BracketPolarity.valueOf(attr);

                switch (p) {
                    case LEFT:
                        s.push(n);
                        break;
                    case RIGHT:
                        if (s.isEmpty()) {
                            break;
                        }
                        
                        CommandNode node = s.pop();

                        if (node == cnode) {
                            return Arrays.asList(node, n);
                        }

                        if (n == cnode) {
                            return Arrays.asList(n, node);
                        }
                        break;
                }
            }
        }

        return Arrays.asList(cnode);
    }

    private static final class FindFirstLevelCommands extends TraverseHandler {

        private List<CommandNode> commands = new LinkedList<CommandNode>();

        @Override
        public boolean commandStart(CommandNode node) {
            commands.add(node);
            return false;
        }

        @Override
        public void commandEnd(CommandNode node) {
        }

        @Override
        public boolean argumentStart(ArgumentNode node) {
            return true;
        }

        @Override
        public void argumentEnd(ArgumentNode node) {
        }

        @Override
        public boolean blockStart(BlockNode node) {
            return false;
        }

        @Override
        public void blockEnd(BlockNode node) {}

    }

    private enum BracketPolarity {
        LEFT,
        MIDDLE,
        RIGHT;
    }
}

