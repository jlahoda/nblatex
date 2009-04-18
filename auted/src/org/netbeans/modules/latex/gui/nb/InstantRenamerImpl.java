/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * The Original Software is the LaTeX module.
 * The Initial Developer of the Original Software is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2008-2009.
 * All Rights Reserved.
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
 *
 * Contributor(s): Jan Lahoda.
 */
package org.netbeans.modules.latex.gui.nb;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.DefaultTraverseHandler;
import org.netbeans.modules.latex.model.command.DocumentNode;
import org.netbeans.modules.latex.model.command.Node;

/**
 *
 * @author Jan Lahoda
 */
public class InstantRenamerImpl {

    private static final Logger LOG = Logger.getLogger(InstantRenamerImpl.class.getName());

//    public boolean isRenameAllowed(CompilationInfo info, int caretOffset, String[] explanationRetValue) {
//        return lookupNodes(info, caretOffset, new Node[1], new String[1]);
//    }
//
//    public Set<OffsetRange> getRenameRegions(CompilationInfo info, int caretOffset) {
//        Set<OffsetRange> result = new HashSet<OffsetRange>();
//
//        for (int[] span : getRenameRegionsImpl(info, caretOffset)) {
//            result.add(new OffsetRange(span[0], span[1]));
//        }
//
//        return result;
//    }

    static List<int[]> getRenameRegionsImpl(final LaTeXParserResult lpr, int caretOffset) {
        Node[] vcpicture = new Node[1];
        final String[] name = new String[1];

        if (!lookupNodes(lpr, caretOffset, vcpicture, name)) {
            return Collections.emptyList();
        }

        final List<int[]> span = new LinkedList<int[]>();

        vcpicture[0].traverse(new DefaultTraverseHandler() {
            @Override
            public boolean argumentStart(ArgumentNode node) {
                if (node.hasAttribute("#vc-state-name")) {
                    if (name[0].equals(findText(lpr, node))) {
                        span.add(findSpan(node));
                    }
                }
                return super.argumentStart(node);
            }
        });

        return span;
    }

    private static boolean lookupNodes(LaTeXParserResult lpr, int offset, Node[] vcpicture, String[] name) {
        try {
            Node n = lpr.getCommandUtilities().findNode(lpr.getSnapshot().getSource().getDocument(true), offset);

            while (!(n instanceof DocumentNode)) {
                if (n instanceof ArgumentNode && n.hasAttribute("#vc-state-name")) {
                    name[0] = findText(lpr, (ArgumentNode) n);
                }

                if (n instanceof BlockNode && "VCPicture".equals(((BlockNode) n).getEnvironment().getName())) {
                    vcpicture[0] = n;
                    return name[0] != null;
                }

                n = n.getParent();
            }

            return false;
        } catch (IOException ex) {
            LOG.log(Level.FINE, null, ex);
            return false;
        }
    }

    private static String findText(LaTeXParserResult lpr, ArgumentNode n) {
        int[] span = findSpan((ArgumentNode) n);

        return lpr.getSnapshot().getText().subSequence(span[0], span[1] + 1).toString();
    }

    private static int[] findSpan(ArgumentNode n) {
        return new int[] {
            n.getStartingPosition().getOffsetValue() + 1,//XXX: brackets
            n.getEndingPosition().getOffsetValue() - 1,
        };
    }

}
