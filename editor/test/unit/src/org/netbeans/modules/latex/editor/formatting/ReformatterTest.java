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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.text.StyledDocument;
import org.netbeans.api.lexer.Language;
import org.netbeans.core.startup.Main;
import org.netbeans.junit.NbTestCase;
import org.netbeans.lib.editor.util.swing.MutablePositionRegion;
import org.netbeans.modules.editor.indent.IndentSpiPackageAccessor;
import org.netbeans.modules.editor.indent.spi.Context.Region;
import org.netbeans.modules.latex.UnitUtilities;
import org.netbeans.modules.latex.bibtex.loaders.MyDataLoader;
import org.netbeans.modules.latex.editor.TexLanguage;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/**
 *
 * @author Jan Lahoda
 */
public class ReformatterTest extends NbTestCase {
    
    public ReformatterTest(String testName) {
        super(testName);
    }            

    @Override
    protected void setUp() throws Exception {
        System.setProperty("netbeans.user", getWorkDir().getAbsolutePath());
        new File(new File(getWorkDir(), "var"), "log").mkdirs();
        UnitUtilities.initLookup();
        UnitUtilities.prepareTest(new String[] {"/org/netbeans/modules/latex/resources/mf-layer.xml", "/org/netbeans/modules/latex/bibtex/layer.xml"}, new Object[] {MyDataLoader.getLoader(MyDataLoader.class)});
        
        FileUtil.setMIMEType("tex", "text/x-tex");
        FileUtil.setMIMEType("bib", "text/x-bibtex");
        
        Main.initializeURLFactory();
        
        Settings.getDefault().reset();
        
        super.setUp();
    }

//    public void testItemReformat() throws Exception {
//        Settings.getDefault().setWrapLength(Integer.MAX_VALUE);
//        //XXX: other settings:
//        testReformat(
//                     "\\documentclass{article}\n" +
//                     "\\begin{document}\n" +
//                     "\\begin{itemize}\n" +
//                     "\\item{test\n" +
//                     "test2}\n" +
//                     "\\end{itemize}\n" +
//                     "\\end{document}\n",
//                     "\\documentclass{article}\n" +
//                     "\\begin{document}\n" +
//                     "\\begin{itemize}\n" +
//                     "\\" + "item{test\n" +
//                     " "  + "     test2}\n" +
//                     "\\end{itemize}\n" +
//                     "\\end{document}\n");
//    }
//
//    public void testParagraphReformat1() throws Exception {
//        Settings.getDefault().setWrapLength(10);
//        testReformat(
//                     "\\documentclass{article}\n" +
//                     "\\begin{document}\n" +
//                     "yyy yyy yyy yyy yyy yyy yyy yyy\n" +
//                     "\\end{document}\n",
//                     "\\documentclass{article}\n" +
//                     "\\begin{document}\n" +
//                     "yyy yyy\n" +
//                     "yyy yyy\n" +
//                     "yyy yyy\n" +
//                     "yyy yyy\n" +
//                     "\\end{document}\n");
//    }
//
//    public void testParagraphReformat2() throws Exception {
//        Settings.getDefault().setWrapLength(10);
//        testReformat(
//                     "\\documentclass{article}\n" +
//                     "\\begin{document}\n" +
//                     "y yy% yyy yyy yyy\n" +
//                     "yyy yyy yyy yyy\n" +
//                     "\\end{document}\n",
//                     "\\documentclass{article}\n" +
//                     "\\begin{document}\n" +
//                     "y\n" +
//                     "yy% yyy yyy yyy\n" +
//                     "yyy yyy\n" +
//                     "yyy yyy\n" +
//                     "\\end{document}\n");
//    }
    
    public void testTabular1() throws Exception {
        testReformat(
                     "\\documentclass{article}\n" +
                     "\\begin{document}\n" +
                     "\\begin{tabular}{c c c}\n" +
                     "a &bbb  & c\\\\\n" +
                     " aaa&   c & ucc \\\\\n" +
                     "\\end{tabular}\n" +
                     "\\end{document}\n",
                     "\\documentclass{article}\n" +
                     "\\begin{document}\n" +
                     "\\begin{tabular}{c c c}\n" +
                     "a   & bbb & c  \\\\\n" +
                     "aaa & c   & ucc\\\\\n" +
                     "\\end{tabular}\n" +
                     "\\end{document}\n");
    }
    
    public void testTabular2() throws Exception {
        testReformat(
                     "\\documentclass{article}\n" +
                     "\\begin{document}\n" +
                     "|\\begin{tabular}{c c}\n" +
                     "asdfadsf & s \\\\\n" +
                     "s & lajdfjlakjdflkjalkdfj\\\\\n" +
                     "\\end{tabular}|\n" +
                     "\\begin{tabular}{c c}\n" +
                     "asdfadsf & s \\\\\n" +
                     "s & lajdfjlakjdflkjalkdfj\\\\\n" +
                     "\\end{tabular}\n" +
                     "\\end{document}\n",
                     "\\documentclass{article}\n" +
                     "\\begin{document}\n" +
                     "\\begin{tabular}{c c}\n" +
                     "asdfadsf & s                    \\\\\n" +
                     "s        & lajdfjlakjdflkjalkdfj\\\\\n" +
                     "\\end{tabular}\n" +
                     "\\begin{tabular}{c c}\n" +
                     "asdfadsf & s \\\\\n" +
                     "s & lajdfjlakjdflkjalkdfj\\\\\n" +
                     "\\end{tabular}\n" +
                     "\\end{document}\n");
    }

    private void testReformat(String code, final String golden) throws Exception {
        String[] split = code.split("\\|");

        switch (split.length) {
            case 1:
                testReformat(code, 0, code.length(), golden);
                return ;
            case 3:
                StringBuilder c = new StringBuilder();

                c.append(split[0]);
                c.append(split[1]);
                c.append(split[2]);
                
                testReformat(c.toString(), split[0].length(), split[0].length() + split[1].length(), golden);

                return ;

            default:
                fail(Arrays.asList(split).toString());
        }
    }

    private void testReformat(String code, int start, int end, final String golden) throws Exception {
        FileObject     testFileObject = getTestFile("text.tex");
        
        copyStringToFile(testFileObject, code);
        
        DataObject od = DataObject.find(testFileObject);
        StyledDocument doc = od.getLookup().lookup(EditorCookie.class).openDocument();
 
        doc.putProperty("mime-type", "text/x-tex");
        doc.putProperty(Language.class, TexLanguage.description());

        Region reg = IndentSpiPackageAccessor.get().createContextRegion(new MutablePositionRegion(doc, start, end));
        Reformatter r = new Reformatter(doc, Collections.singletonList(reg));
        
        r.reformat();
        
        String text = doc.getText(0, doc.getLength());
        
        assertEquals(golden, text);
    }
    
    private FileObject getTestFile(String testFile) throws IOException, InterruptedException {
        clearWorkDir();
        
        FileObject workdir = FileUtil.toFileObject(getWorkDir());
        
        assertNotNull(workdir);
        
        FileObject test = workdir.createData("test.tex");
        
        assertNotNull(test);
        
        return test;
    }
    
    @Override
    protected boolean runInEQ() {
        return true;
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
