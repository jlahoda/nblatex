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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 2008-2009 Sun
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

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.StyledDocument;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.Token;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.latex.UnitUtilities;
import org.netbeans.modules.latex.bibtex.loaders.MyDataLoader;
import static org.junit.Assert.*;
import org.netbeans.modules.latex.lexer.TexTokenId;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.openide.filesystems.FileUtil;
import org.xml.sax.SAXException;


import org.netbeans.core.startup.Main;
import org.netbeans.modules.latex.lexer.impl.TexLanguage;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

/**
 *
 * @author Jan Lahoda
 */
public class SpellcheckerDataCollectorTest extends NbTestCase {

    public SpellcheckerDataCollectorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws IOException, SAXException, PropertyVetoException {
        System.setProperty("netbeans.user", getWorkDir().getAbsolutePath());
        new File(new File(getWorkDir(), "var"), "log").mkdirs();
        UnitUtilities.initLookup();
        UnitUtilities.prepareTest(new String[] {"/org/netbeans/modules/latex/resources/mf-layer.xml", "/org/netbeans/modules/latex/bibtex/layer.xml"}, new Object[] {MyDataLoader.getLoader(MyDataLoader.class)});
        
        FileUtil.setMIMEType("tex", "text/x-tex");
        FileUtil.setMIMEType("bib", "text/x-bibtex");
        
        Main.initializeURLFactory();
    }

    public void testCollector1() throws Exception {
        performer("\\documentclass{article}\n" +
                  "\\begin{document}\n" +
                  "testa\n" +
                  "\\end{document}\n",
                  "testa");
    }
    
    public void testCollector2() throws Exception {
        performer("\\documentclass{article}\n" +
                  "\\begin{document}\n" +
                  "$testa$\n" +
                  "\\end{document}\n");
    }

    private void performer(String code, final String... golden) throws Exception {
        FileObject     testFileObject = getTestFile("text.tex");
        
        copyStringToFile(testFileObject, code);
        
        DataObject od = DataObject.find(testFileObject);
        final StyledDocument doc = od.getLookup().lookup(EditorCookie.class).openDocument();
 
        doc.putProperty("mimeType", "text/x-tex");
        doc.putProperty(Language.class, TexTokenId.language());

        Source s = Source.create(doc);

        ParserManager.parse(Collections.singleton(s), new UserTask() {
            public void run(ResultIterator it) throws Exception {
                LaTeXParserResult lpr = LaTeXParserResult.get(it);
                Set<String> text = new HashSet<String>();

                for (Token t : SpellcheckerDataCollector.getAcceptedTokens(doc, new AtomicBoolean(), lpr.getDocument())) {
                    text.add(t.text().toString());
                }

                assertEquals(new HashSet<String>(Arrays.asList(golden)), text);
            }
        });
    }

    private FileObject getTestFile(String testFile) throws IOException, InterruptedException {
        clearWorkDir();
        
        FileObject workdir = FileUtil.toFileObject(getWorkDir());
        
        assertNotNull(workdir);
        
        FileObject test = workdir.createData("test.tex");
        
        assertNotNull(test);
        
        return test;
    }
    
    public final static FileObject copyStringToFile(FileObject f, String content) throws Exception {
        OutputStream os = f.getOutputStream();
        InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
        FileUtil.copy(is, os);
        os.close();
        is.close();

        return f;
    }

}