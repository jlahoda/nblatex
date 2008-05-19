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

package org.netbeans.modules.latex.gui.nb;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.netbeans.modules.gsf.api.CancellableTask;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.modules.latex.model.command.Command;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.netbeans.modules.latex.model.structural.StructuralElement;
import org.netbeans.modules.latex.model.structural.StructuralNodeFactory;
import org.netbeans.napi.gsfret.source.CompilationController;
import org.netbeans.napi.gsfret.source.Phase;
import org.netbeans.napi.gsfret.source.Source;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Jan Lahoda
 */
public class AddAutomaton implements CodeGenerator {

    private final boolean addUsePackage;
    private final JTextComponent comp;

    public AddAutomaton(JTextComponent comp, boolean addUsePackage) {
        this.comp = comp;
        this.addUsePackage = addUsePackage;
    }

    public String getDisplayName() {
        return "Insert Automaton";
    }

    public void invoke() {
        try {
            int offset = comp.getCaretPosition();
            String begin = "\\VCDraw{\n\\begin{VCPicture}{(0,0)(0,0)}\n";
            String end   = "\\end{VCPicture}\n}\n";

            comp.getDocument().insertString(offset, begin + end, null);
            comp.setCaretPosition(offset + begin.length());
            
            final int targetOffset = offset + begin.length();
            
            Source s = Source.forDocument(comp.getDocument());
            
            s.runUserActionTask(new CancellableTask<CompilationController>() {
                public void cancel() {}
                public void run(CompilationController parameter) throws Exception {
                    parameter.toPhase(Phase.RESOLVED);
                    
                    LaTeXParserResult lpr = LaTeXParserResult.get(parameter);
                    
                    StructuralElement e = findAutomaton(lpr.getStructuralRoot(), (FileObject) Utilities.getDefault().getFile(comp.getDocument()), targetOffset);
                    
                    if (e != null) {
                        Node node = StructuralNodeFactory.createNode(e);
                        
                        node.getLookup().lookup(OpenCookie.class).open();
                    }
                }
            }, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private StructuralElement findAutomaton(StructuralElement el, FileObject file, int offset) {
        if (el instanceof VauStructuralElement) {
            VauStructuralElement e = (VauStructuralElement) el;
            
            if (Utilities.getDefault().compareFiles(file, e.getStartingPosition().getFile())) {
                if (e.getStartingPosition().getOffsetValue() <= offset && offset <= e.getEndingPosition().getOffsetValue()) {
                    return e;
                }
            }
        }
        
        for (StructuralElement e : el.getSubElements()) {
            StructuralElement res = findAutomaton(e, file, offset);
            
            if (res != null) {
                return res;
            }
        }
        
        return null;
    }
    
    public static final class FactoryImpl implements Factory {

        public List<? extends CodeGenerator> create(Lookup context) {
            try {
                JTextComponent comp = context.lookup(JTextComponent.class);
                LaTeXParserResult lpr = context.lookup(LaTeXParserResult.class);
                SourcePosition pos = new SourcePosition(comp.getDocument(), comp.getCaretPosition());
                boolean found = false;
                
                for (Command cmd : lpr.getCommandUtilities().getCommands(pos)) {
                    if ("\\VCDraw".equals(cmd.getCommand())) {
                        found = true;
                        break;
                    }
                }

                return Collections.singletonList(new AddAutomaton(comp, !found));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return Collections.emptyList();
            }
        }
        
    }

}
