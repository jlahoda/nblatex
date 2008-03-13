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
 * The Original Software is the LaTeX module.
 * The Initial Developer of the Original Software is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2008.
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
package org.netbeans.modules.latex.model.structural.parser;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.text.StyledDocument;
import org.netbeans.api.lexer.Language;
import org.netbeans.core.startup.Main;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.gsf.api.CancellableTask;
import org.netbeans.modules.latex.UnitUtilities;
import org.netbeans.modules.latex.editor.TexLanguage;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.impl.NBUtilities;
import org.netbeans.modules.latex.model.structural.StructuralElement;
import org.netbeans.modules.latex.model.structural.StructuralNodeFactory;
import org.netbeans.modules.latex.model.structural.section.SectionStructuralElement;
import org.netbeans.napi.gsfret.source.CompilationController;
import org.netbeans.napi.gsfret.source.Phase;
import org.netbeans.napi.gsfret.source.Source;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.xml.sax.SAXException;

/**
 *
 * @author Jan Lahoda
 */
public class StructuralParserTest extends NbTestCase {

    public StructuralParserTest(String name) {
        super(name);
    }
    
    protected void setUp() throws IOException, SAXException, PropertyVetoException {
        System.setProperty("netbeans.test.latex.enable", "true");
        
        UnitUtilities.initLookup();
        UnitUtilities.prepareTest(new String[] {"/org/netbeans/modules/latex/resources/mf-layer.xml"}, new Object[] {new NBUtilities()});
        
        FileUtil.setMIMEType("tex", "text/x-tex");
        FileUtil.setMIMEType("bib", "text/x-bibtex");
        
        Main.initializeURLFactory();
    }

    public void testStructuralNodeUpdating() throws Exception {
        getWorkDir().delete();
        
        FileObject file = FileUtil.createData(new File(getWorkDir(), "test.tex"));
        String code = "\\documentclass{article}\n" +
                      "\\begin{document}\n" +
                      "\\section{Test}to-replace\n" +
                      "\\section{Test}\n" +
                      "\\end{document}\n";
        
        copyStringToFile(file, code);
        
        DataObject od = DataObject.find(file);
        StyledDocument doc = od.getLookup().lookup(EditorCookie.class).openDocument();
 
        doc.putProperty("mime-type", "text/x-tex");
        doc.putProperty(Language.class, TexLanguage.description());

        Source s = Source.forDocument(doc);
        
        final StructuralElement[] section = new StructuralElement[1];
        
        s.runUserActionTask(new CancellableTask<CompilationController>() {
            public void cancel() {}
            public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(Phase.RESOLVED);
                
                StructuralElement root = LaTeXParserResult.get(parameter).getStructuralRoot();
                
                section[0] = root.getSubElements().get(1);
                assertEquals("Test", StructuralNodeFactory.createNode(section[0]).getDisplayName());
            }
        }, true);
        
        assertTrue(section[0] instanceof SectionStructuralElement);
        
        doc.insertString(code.indexOf("to-replace"), "long long long", null);

        s.runUserActionTask(new CancellableTask<CompilationController>() {
            public void cancel() {}
            public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(Phase.RESOLVED);
                
                StructuralElement root = LaTeXParserResult.get(parameter).getStructuralRoot();
                
                assertTrue(section[0] == root.getSubElements().get(1));
                assertEquals("Test", ((SectionStructuralElement) section[0]).getName());
            }
        }, true);
    }
    
    /**
     * Copies a string to a specified file.
     *
     * @param f the {@link FilObject} to use.
     * @param content the contents of the returned file.
     * @return the created file
     */
    public final static FileObject copyStringToFile (FileObject f, String content) throws Exception {
        OutputStream os = f.getOutputStream();
        InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
        FileUtil.copy(is, os);
        os.close ();
        is.close();
            
        return f;
    }   
}
