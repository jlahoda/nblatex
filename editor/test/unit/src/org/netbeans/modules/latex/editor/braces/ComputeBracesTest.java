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

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.StyledDocument;
import junit.framework.Assert;
import org.netbeans.api.lexer.Language;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.latex.UnitUtilities;
import org.netbeans.modules.latex.bibtex.loaders.MyDataLoader;
import static org.junit.Assert.*;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;
import org.openide.filesystems.FileUtil;
import org.xml.sax.SAXException;


import org.netbeans.core.startup.Main;
import org.netbeans.modules.editor.bracesmatching.SpiAccessor;
import org.netbeans.modules.gsf.api.CancellableTask;
import org.netbeans.modules.latex.editor.TexLanguage;
import org.netbeans.napi.gsfret.source.CompilationController;
import org.netbeans.napi.gsfret.source.Phase;
import org.netbeans.napi.gsfret.source.Source;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

/**
 *
 * @author Jan Lahoda
 */
public class ComputeBracesTest extends NbTestCase {

    public ComputeBracesTest(String name) {
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

    public void testBlock1() throws Exception {
        performer("\\documentclass{article}\n" +
                  "|1\\be&gin{document}|1\n" +
                  "|2\\end{document}|2\n");
    }

    public void testBlock2() throws Exception {
        performer("\\documentclass{article}\n" +
                  "|2\\begin{document}|2\n" +
                  "|1\\en&d{document}|1\n");
    }

    public void testAlgorithmForLoop1() throws Exception {
        performer("\\documentclass{article}\n" +
                  "\\usepackage{algorithm}\n" +
                  "\\usepackage{algorithmic}\n" +
                  "\\begin{document}\n" +
                  "\\begin{algorithmic}\n" +
                  "|1\\FO&R|1{}\n" +
                  "|2\\ENDFOR|2" +
                  "\\end{algorithmic}\n" +
                  "\\end{document}\n");
    }

    public void testAlgorithmForLoop2() throws Exception {
        performer("\\documentclass{article}\n" +
                  "\\usepackage{algorithm}\n" +
                  "\\usepackage{algorithmic}\n" +
                  "\\begin{document}\n" +
                  "\\begin{algorithmic}\n" +
                  "|2\\FOR|2{}\n" +
                  "|1\\END&FOR|1" +
                  "\\end{algorithmic}\n" +
                  "\\end{document}\n");
    }

    public void testCommandKindRespected() throws Exception {
        performer("\\documentclass{article}\n" +
                  "\\usepackage{algorithm}\n" +
                  "\\usepackage{algorithmic}\n" +
                  "\\begin{document}\n" +
                  "\\begin{algorithmic}\n" +
                  "|1\\FO&R|1{}\n" +
                  "\\ENDIF{}\n" +
                  "|2\\ENDFOR|2" +
                  "\\end{algorithmic}\n" +
                  "\\end{document}\n");
    }

    public void testLeftRightCommand() throws Exception {
        performer("\\documentclass{article}\n" +
                  "\\begin{document}\n" +
                  "$|1\\le&ft{|1ggg|2\\right.|2$\n" +
                  "\\end{algorithmic}\n" +
                  "\\end{document}\n");
    }

    public void testUnbalancedCommandBrackets() throws Exception {
        performer("\\documentclass{article}\n" +
                  "\\begin{document}\n" +
                  "$|1\\ri&ght.|1$\n" +
                  "\\end{algorithmic}\n" +
                  "\\end{document}\n");
    }

    private void performer(String code) throws Exception {
        final int[] caret = new int[1];
        final List<int[]> braces = new LinkedList<int[]>();

        code = detectOffsets(code, caret, braces);
        
        FileObject     testFileObject = getTestFile("text.tex");
        
        copyStringToFile(testFileObject, code);
        
        DataObject od = DataObject.find(testFileObject);
        final StyledDocument doc = od.getLookup().lookup(EditorCookie.class).openDocument();
 
        doc.putProperty("mime-type", "text/x-tex");
        doc.putProperty(Language.class, TexLanguage.description());

        Source s = Source.forDocument(doc);
        
        s.runUserActionTask(new CancellableTask<CompilationController>() {
            public void cancel() {}
            public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(Phase.RESOLVED);

                LaTeXParserResult lpr = LaTeXParserResult.get(parameter);
                MatcherContext mc = SpiAccessor.get().createCaretContext(doc, caret[0], true, -1);
                List<int[]> result = ComputeBraces.doComputeBraces(lpr, mc, new AtomicBoolean(), new AtomicBoolean());

                assertEquals(braces, result);
            }
        }, true);
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
    
    public static String detectOffsets(String source, int[] caret, List<int[]> output) {
        String[] split = source.split("\\&");
        
        Assert.assertTrue("incorrect number of position markers (&)", split.length == 2);

        caret[0] = split[0].replaceAll("\\|.", "").length();

        source = split[0] + split[1];
        
        split = source.split("\\|");
        
        Assert.assertTrue("incorrect number of position markers (|)", split.length % 2 == 1);

        SortedMap<Character, int[]> map = new TreeMap<Character, int[]>();
        StringBuilder sb = new StringBuilder();
        int[] span = null;
        int offset = 0;
        boolean first = true;
        
        for (String s : split) {
            Character c = null;

            if (!first) {
                c = s.charAt(0);
                s = s.substring(1);
            }
            
            first = false;
            sb.append(s);
            
            if (span == null) {
                span = new int[2];
                span[0] = (offset += s.length());
            } else {
                span[1] = (offset += s.length());
                map.put(c, span);
                span = null;
            }
        }

        output.addAll(map.values());
        
        return sb.toString();
    }

    private static void assertEquals(List<int[]> gL, List<int[]> oL) {
        Iterator<int[]> g = gL.iterator();
        Iterator<int[]> o = oL.iterator();

        while (g.hasNext() && o.hasNext()) {
            int[] gN = g.next();
            int[] oN = o.next();

            assertArrayEquals(gN, oN);
        }
        
        assertTrue(g.hasNext() == o.hasNext());
    }
    
}
