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
 * The Original Software is the LaTeX module.
 * The Initial Developer of the Original Software is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2009.
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
package org.netbeans.modules.latex.editor.fold;

import java.util.ArrayList;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.Scheduler;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Stack;
import javax.swing.text.Document;
import org.netbeans.api.editor.fold.FoldType;
import org.netbeans.modules.latex.editor.fold.FoldMaintainerImpl.FoldInfo;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DefaultTraverseHandler;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.netbeans.modules.latex.model.hacks.RegisterParsingTaskFactory;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;

/**
 *
 * @author Jan Lahoda
 */
public final class FoldTask extends ParserResultTask {

    private class BlockFoldMaintainerSourceTraverseHandler extends DefaultTraverseHandler {
        
        private Collection<FoldInfo> blocks;
        
        public BlockFoldMaintainerSourceTraverseHandler() {
            blocks = new LinkedList<FoldInfo>();
        }
        
        @Override
        public boolean blockStart(BlockNode node) {
            blocks.add(create(node));
            return true;
        }
        
        public Collection<? extends FoldInfo> getBlocks() {
            return blocks;
        }
    }
    
    private static FoldInfo create(BlockNode bn) {
        FoldInfo f = new FoldInfo();

        f.setStart(bn.getBeginCommand().getEndingPosition());

        if (bn.getEndCommand() != null) {
            f.setEnd(bn.getEndCommand().getStartingPosition());
        }
        else {
            f.setEnd(bn.getEndingPosition());
        }
        f.setBlockName("...");

        f.setBeginDamage(0);
        f.setEndDamage(0);

        f.setType(BLOCK_FOLD_TYPE);

        return f;
    }
        
    private static final FoldType BLOCK_FOLD_TYPE = new FoldType("block-fold-type");

    public void cancel() {
    }

    @Override
    public void run(Result result, SchedulerEvent evt) {
        Document doc = result.getSnapshot().getSource().getDocument(false);
        
        if (doc == null)
            return ;
        
        LaTeXParserResult lpr = LaTeXParserResult.get(result);

        //XXX:
//        if (lpr.getUpdateState().isUnchanged())
//            return ;
        
        Collection<FoldInfo> folds = new LinkedList<FoldInfo>();
        
        BlockFoldMaintainerSourceTraverseHandler blockHandler = new BlockFoldMaintainerSourceTraverseHandler();
        
        lpr.getDocument().traverse(blockHandler);
        
        folds.addAll(blockHandler.blocks);
        
        SectionFoldMaintainerSourceTraverseHandler sectionHandler = new SectionFoldMaintainerSourceTraverseHandler(doc);
        
        lpr.getDocument().traverse(sectionHandler);
        
        folds.addAll(sectionHandler.folds);
        
        FoldMaintainerImpl.getHolder(doc).setFolds(folds);
    }
    
    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    private class SectionFoldInfo extends FoldInfo {
        
        private int type;
        
        public SectionFoldInfo(CommandNode cn, int type) {
            setStart(cn.getEndingPosition());
            setBlockName("...");
            setBeginDamage(0);
            setEndDamage(0);
            setType(SECTION_FOLD_TYPE);
            this.type = type;
        }
        
    }
    
    protected class SectionFoldMaintainerSourceTraverseHandler extends DefaultTraverseHandler {
        
        private Collection<SectionFoldInfo> folds;
        private Stack<SectionFoldInfo>      sections;
        private Document doc;
        
        public SectionFoldMaintainerSourceTraverseHandler(Document doc) {
            folds = new ArrayList<SectionFoldInfo>();
            sections = new Stack<SectionFoldInfo>();
            this.doc = doc;
        }
        
        public boolean commandStart(CommandNode node) {
            if (!Utilities.getDefault().compareFiles(node.getStartingPosition().getFile(), Utilities.getDefault().getFile(doc)))
                return true;

            int type = getTypeForName(node.getCommand().getCommand());
            
            if (type == (-1))
                return true;
            
            while (!sections.isEmpty() && sections.peek().type >= type) {
                SectionFoldInfo info = sections.pop();
                
                info.setEnd(node.getStartingPosition());
                folds.add(info);
            }
            
            sections.push(new SectionFoldInfo(node, type));
            return true;
        }
        
        public void blockEnd(BlockNode node) {
            if ("document".equals(node.getBlockName())) {
                while (!sections.isEmpty()) {
                    SectionFoldInfo info = sections.pop();
                    
                    if (Utilities.getDefault().compareFiles(node.getEndingPosition().getFile(), Utilities.getDefault().getFile(doc))) {
                        CommandNode endCommand = node.getEndCommand();
                        
                        if (endCommand != null)
                            info.setEnd(endCommand.getStartingPosition());
                        else
                            info.setEnd(node.getEndingPosition());
                    } else
                        //XXX: locking
                        info.setEnd(new SourcePosition(doc, doc.getLength()));
                    
                    folds.add(info);
                }
            }
        }
        
        public Collection<? extends FoldInfo> getFolds() {
            return folds;
        }
    }
    
    private static final FoldType SECTION_FOLD_TYPE = new FoldType("section-fold-type");
    
    private static String[] sectionNames = new String[] {
        "\\chapter",
        "\\section",
        "\\subsection",
        "\\subsubsection",
        "\\paragraph",
        "\\subparagraph"
    };
    
    /**Starting from 1.*/
    public static int getTypeForName(String name) {
        if (name.charAt(name.length() - 1) == '*') {
            name = name.substring(0, name.length() - 2);
            //            System.err.println("getTypeForName: name=" + name);
        }
        
        for (int cntr = 0; cntr < sectionNames.length; cntr++) {
            if (sectionNames[cntr].equals(name))
                return cntr + 1;
        }
        
        return (-1);
    }

    @RegisterParsingTaskFactory(mimeType="text/x-tex")
    public static final class FactoryImpl extends TaskFactory {

        @Override
        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singleton(new FoldTask());
        }
        
    }
    
}
