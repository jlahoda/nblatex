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
package org.netbeans.modules.latex.editor.formatting;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.swing.text.BadLocationException;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.command.TextNode;

/**
 *
 * @author Jan Lahoda
 */
public class TabularFragment implements Fragment {

    public boolean accept(LaTeXParserResult lpr, Node n) {
        return n instanceof BlockNode && "tabular".equals(((BlockNode) n).getEnvironment().getName());
    }

    public Collection<Diff> format(LaTeXParserResult lpr, Node n, int baseIndent) throws BadLocationException {
        BlockNode tabular = (BlockNode) n;
        TextNode  content = tabular.getContent();
        int       start   = content.getStartingPosition().getOffsetValue();
//        int       end     = content.getEndingPosition().getOffsetValue();
        String    tableContent = content.getFullText().toString();
        System.err.println("tableContent=" + tableContent);
        int firstNL = tableContent.indexOf('\n') + 1;
        start += firstNL;
        tableContent = tableContent.substring(firstNL);

        List<Line> lines = new LinkedList<Line>();
        int offset = start;
        
        for (String line : tableContent.split("\n")) {
            lines.add(new Line(offset, line));

            offset += line.length() + 1;
        }

        int[] sizes = new int[lines.get(0).columnStart.size()];

        for (Line l : lines) {
            for (int c = 0; c < sizes.length; c++) {
                sizes[c] = Math.max(sizes[c], l.columnText.get(c).trim().length());
            }
        }

        List<Diff> result = new LinkedList<Diff>();

        for (Line l : lines) {
            for (int c = 0; c < sizes.length; c++) {
                System.err.println("c=" + c);
                String trimmed = l.columnText.get(c).trim();
                int realStart = l.columnText.get(c).indexOf(trimmed);
                int shouldBePreceding = c == 0 ? 0 : 1;
                int realEnd   = realStart + trimmed.length();
                int realTrailing  = l.columnText.get(c).length() - realEnd;
                int shouldBeTrailing = sizes[c] - trimmed.length() + (c == sizes.length - 1 ? 0 : 1);//(c == 0 ? 0 : 1);

                if (shouldBePreceding > realStart) {
                    result.add(new Diff(l.columnStart.get(c), 0, Utilities.spaces(shouldBePreceding - realStart)));
                }

                if (realStart > shouldBePreceding) {
                    int diff = realStart - shouldBePreceding;

                    result.add(new Diff(l.columnStart.get(c), diff, ""));
                }

                if (shouldBeTrailing > realTrailing) {
                    result.add(new Diff(l.columnEnd.get(c), 0, Utilities.spaces(shouldBeTrailing - realTrailing)));
                }

                if (realTrailing > shouldBeTrailing) {
                    int diff = realTrailing - shouldBeTrailing;
                    
                    result.add(new Diff(l.columnEnd.get(c) - diff, diff, ""));
                }
            }
        }

        return result;
    }

    private static final class Line {
        private final int localOffset;
        private final String line;
        private final List<Integer> columnStart;
        private final List<Integer> columnEnd;
        private final List<String>  columnText;

        private Line(int localOffset, String line) {
            int i = localOffset;
            List<Integer> columnStart = new LinkedList<Integer>();
            List<Integer> columnEnd   = new LinkedList<Integer>();
            List<String>  columnText  = new LinkedList<String>();

            for (String c : line.split("&")) {
                if (c.contains("\\\\")) {
                    c = c.substring(0, c.indexOf("\\\\"));
                }
                columnStart.add(i);
                columnEnd.add(i += c.length());
                columnText.add(c);
                i++;
            }

            this.localOffset = localOffset;
            this.line = line;

            this.columnStart = columnStart;
            this.columnEnd   = columnEnd;
            this.columnText  = columnText;
        }
    }

}
