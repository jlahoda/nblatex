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

package org.netbeans.modules.latex.editor.codegen;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.Environment;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Jan Lahoda
 */
public class AddEnvironment implements CodeGenerator {

    private final JTextComponent comp;
    private final List<String> environments;

    public AddEnvironment(JTextComponent comp, List<String> environments) {
        this.comp = comp;
        this.environments = environments;
    }
    
    public String getDisplayName() {
        return "Insert Environment";
    }

    public void invoke() {
        try {
            JComboBox combo = new JComboBox(environments.toArray(new String[0]));

            DialogDescriptor dd = new DialogDescriptor(combo, "Choose Environment", true,
                    DialogDescriptor.OK_CANCEL_OPTION, DialogDescriptor.OK_OPTION, null);

            if (DialogDisplayer.getDefault().notify(dd) == DialogDescriptor.OK_OPTION) {
                String env = (String) combo.getSelectedItem();
                int offset = comp.getCaretPosition();
                String begin = "\\begin{" + env + "}\n";
                String end   = "\n\\end{" + env + "}\n";
                comp.getDocument().insertString(offset, begin + end, null);
                comp.setCaretPosition(offset + begin.length());
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public static final class FactoryImpl implements Factory {

        public List<? extends CodeGenerator> create(Lookup context) {
            try {
                JTextComponent comp = context.lookup(JTextComponent.class);
                LaTeXParserResult lpr = context.lookup(LaTeXParserResult.class);
                List<String> environments = new LinkedList<String>();
                SourcePosition pos = new SourcePosition(comp.getDocument(), comp.getCaretPosition());

                for (Environment env : lpr.getCommandUtilities().getEnvironments(pos)) {
                    environments.add(env.getName());
                }

                Collections.sort(environments);

                return Collections.singletonList(new AddEnvironment(comp, environments));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return Collections.emptyList();
            }
        }
        
    }

}
