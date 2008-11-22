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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

package org.netbeans.modules.latex.hints;

import org.netbeans.modules.latex.editor.LaTeXSettings;

/**
 *
 * @author Jan Lahoda
 */
public class CiteFormatHintTest extends HintsTestBase {
    
    public CiteFormatHintTest(String testName) {
        super(testName);
    }            

    public void testSimple1() throws Exception {
        testAnalyze("\\documentclass{article}\n" +
                    "\\begin{document}\n" +
                    "\\cite{TEST}\n" +
                    "\\bibliography{bib.bib}" +
                    "\\end{document}",
                    "bib.bib",
                    "@inproceedings(TEST,\n" +
                    "    author=\"A. B. Foo and C. D. Bar\"" +
                    ")\n",
                    true,
                    "2:0-2:11:warning:Incorrect format of a citation");
    }

    public void testSimple2() throws Exception {
        testAnalyze("\\documentclass{article}\n" +
                    "\\begin{document}\n" +
                    "Foo and Bar~\\cite{TEST}\n" +
                    "\\bibliography{bib.bib}" +
                    "\\end{document}",
                    "bib.bib",
                    "@inproceedings(TEST,\n" +
                    "    author=\"A. B. Foo and C. D. Bar\"" +
                    ")\n",
                    true);
    }

    public void testSimple3() throws Exception {
        testAnalyze("\\documentclass{article}\n" +
                    "\\begin{document}\n" +
                    "Foo et al.~\\cite{TEST}\n" +
                    "\\bibliography{bib.bib}" +
                    "\\end{document}",
                    "bib.bib",
                    "@inproceedings(TEST,\n" +
                    "    author=\"A. B. Foo and C. D. Bar and E. F. Foo\"" +
                    ")\n",
                    true);
    }

    public void testSimple4() throws Exception {
        testAnalyze("\\documentclass{article}\n" +
                    "\\begin{document}\n" +
                    "Foo and Bar~\\cite{TEST}\n" +
                    "\\bibliography{bib.bib}" +
                    "\\end{document}",
                    "bib.bib",
                    "@inproceedings(TEST,\n" +
                    "    author=\"Foo, A. B. and Bar, C. D.\"" +
                    ")\n",
                    true);
    }

    public void testDisable() throws Exception {
        testAnalyze("\\documentclass{article}\n" +
                    "\\begin{document}\n" +
                    "Wrong~\\cite{TEST}%NO-CITE-FORMAT\n" +
                    "\\bibliography{bib.bib}" +
                    "\\end{document}",
                    "bib.bib",
                    "@inproceedings(TEST,\n" +
                    "    author=\"Foo, A. B. and Bar, C. D.\"" +
                    ")\n",
                    true);
    }

    public void testMultiple1() throws Exception {
        testAnalyze("\\documentclass{article}\n" +
                    "\\begin{document}\n" +
                    "Foo and Bar~\\cite{TEST1, TEST2}\n" +
                    "\\bibliography{bib.bib}" +
                    "\\end{document}",
                    "bib.bib",
                    "@inproceedings(TEST1,\n" +
                    "    author=\"Foo, A. B. and Bar, C. D.\"" +
                    ")\n" +
                    "@inproceedings(TEST2,\n" +
                    "    author=\"Foo, A. B. and Bar, C. D.\"" +
                    ")\n",
                    true);
    }

    public void testMultiple2() throws Exception {
        testAnalyze("\\documentclass{article}\n" +
                    "\\begin{document}\n" +
                    "Foo and Bar~\\cite{TEST1, TEST2}\n" +
                    "\\bibliography{bib.bib}" +
                    "\\end{document}",
                    "bib.bib",
                    "@inproceedings(TEST1,\n" +
                    "    author=\"Foo, A. B. and Bar, C. D.\"" +
                    ")\n" +
                    "@inproceedings(TEST2,\n" +
                    "    author=\"Bad, C. D.\"" +
                    ")\n",
                    true,
                    "2:12-2:31:warning:Different authors");
    }

    public void testMultiple3() throws Exception {
        testAnalyze("\\documentclass{article}\n" +
                    "\\begin{document}\n" +
                    "Wrong~\\cite{TEST1, TEST2}\n" +
                    "\\bibliography{bib.bib}" +
                    "\\end{document}",
                    "bib.bib",
                    "@inproceedings(TEST1,\n" +
                    "    author=\"Foo, A. B. and Bar, C. D.\"" +
                    ")\n" +
                    "@inproceedings(TEST2,\n" +
                    "    author=\"Foo, A. B. and Bar, C. D.\"" +
                    ")\n",
                    true,
                    "2:6-2:25:warning:Incorrect format of a citation");
    }

    @Override
    protected HintProvider createProvider() {
        LaTeXSettings.setCiteFormatHintEnabled(true);
        return new CiteFormatHint();
    }

}
