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

package org.netbeans.modules.latex.gui.nb;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.text.StyledDocument;
import static org.junit.Assert.*;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.latex.UnitUtilities;
import org.openide.filesystems.FileUtil;
import org.xml.sax.SAXException;

import org.netbeans.modules.latex.editor.TexLanguage;
import org.netbeans.core.startup.Main;
import org.netbeans.modules.gsf.Language;
import org.netbeans.modules.gsf.api.CancellableTask;
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
public class InstantRenamerImplTest extends NbTestCase {

    public InstantRenamerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws IOException, SAXException, PropertyVetoException {
        System.setProperty("netbeans.user", getWorkDir().getAbsolutePath());
        new File(new File(getWorkDir(), "var"), "log").mkdirs();
        UnitUtilities.initLookup();
        UnitUtilities.prepareTest(new String[] {"/org/netbeans/modules/latex/resources/mf-layer.xml", "/org/netbeans/modules/latex/gui/nb/mf-layer.xml"/*, "/org/netbeans/modules/latex/bibtex/layer.xml"*/}, new Object[] {/*MyDataLoader.getLoader(MyDataLoader.class)*/});

        FileUtil.setMIMEType("tex", "text/x-tex");
        FileUtil.setMIMEType("bib", "text/x-bibtex");

        Main.initializeURLFactory();
    }

    public void testBlock1() throws Exception {
        performer("\\documentclass{article}\n" +
                  "\\usepackage{vaucanson-g}\n" +
                  "\\begin{document}\n" +
                  "\\VCDraw{\n" +
                  "\\begin{VCPicture}{(5,-3)(15,-7)}\n" +
                  "\\FinalState[D]{(15,-5)}{|__SYSTEMID7568|}\n" +
                  "\\State[C]{(12,-5)}{__SYSTEMID7567}\n" +
                  "\\State[B]{(9,-5)}{__SYSTEMID6165}\n" +
                  "\\State[A]{(6,-5)}{__SYSTEMID6164}\n" +
                  "\\Initial{__SYSTEMID6164}\n" +
                  "\\VArcR[0.45]{arcangle=-22.33,ncurv=1}{|__SYSTEMID7568|}{__SYSTEMID6165}{1}\n" +
                  "\\EdgeL[0.45]{__SYSTEMID7567}{|__SYSTEMID7568|}{0}\n" +
                  "\\LArcL[0.45]{__SYSTEMID7567}{__SYSTEMID6165}{1}\n" +
                  "\\LoopN[0.45]{__SYSTEMID6164}{0}\n" +
                  "\\EdgeL[0.45]{__SYSTEMID6164}{__SYSTEMID6165}{1}\n" +
                  "\\EdgeL[0.45]{__SYSTEMID6165}{__SYSTEMID7567}{0}\n" +
                  "\\LoopN[0.45]{__SYSTEMID6165}{1}\n" +
                  "\\VArcL[0.45]{arcangle=30,ncurv=1}{|__SYSTEMID7568|}{__SYSTEMID6164}{0}\n" +
                  "\\end{VCPicture}\n" +
                  "}\n" +
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

                for (int[] b : braces) {
                    List<int[]> result = InstantRenamerImpl.getRenameRegionsImpl(parameter, (b[0] + b[1]) / 2);
                    
                    assertEquals(braces, result);
                }
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
        String[] split = source.split("\\|");

        assertTrue("incorrect number of position markers (|)", split.length % 2 == 1);

        StringBuilder sb = new StringBuilder();
        int[] span = null;
        int offset = 0;

        for (String s : split) {
            sb.append(s);

            if (span == null) {
                span = new int[2];
                span[0] = (offset += s.length());
            } else {
                span[1] = (offset += s.length());
                output.add(span);
                span = null;
            }
        }

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
