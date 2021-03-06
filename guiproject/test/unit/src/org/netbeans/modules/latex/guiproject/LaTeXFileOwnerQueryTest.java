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
package org.netbeans.modules.latex.guiproject;

import java.io.IOException;
import java.util.Collections;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jan Lahoda
 */
public class LaTeXFileOwnerQueryTest extends ProjectTestCase {
    
    /** Creates a new instance of LaTeXFileOwnerQueryTest */
    public LaTeXFileOwnerQueryTest(String name) {
        super(name);
    }
    
    private void parse(final FileObject mainFile) throws Exception {
        Source s = Source.create(mainFile);
        
        assertNotNull(s);
        
        ParserManager.parse(Collections.singleton(s), new UserTask() {
            public void run(ResultIterator it) throws Exception {
                LaTeXParserResult lpr = LaTeXParserResult.get(it);
                new ProjectReparsedTaskFactory.TaskImpl().run(lpr, null);
            }
        });
    }
    
    private void checkFile(FileObject prj, FileObject mainFile, String name) throws Exception {
        parse(mainFile);
        
        FileObject file = prj.getFileObject(name);
        
        if (file == null)
            fail("The test file was not found.");
        
        Project p = FileOwnerQuery.getOwner(file);
        
        if (p == null)
            fail("No project corresponding to file: " + name + " found.");
        
        FileObject foundMainFile = p.getLookup().lookup(FileObject.class);
        
        assertTrue("Incorrect project found!", foundMainFile /*!!*/ == /*!!*/ mainFile);
    }
    
    public void test_main1_tex_File() throws Exception {
        checkFile(prj1, main1, "main1.tex");
    }
    
    public void test_included1a_tex_File() throws Exception {
        checkFile(prj1, main1, "included1a.tex");
    }
    
    public void test_main2_tex_File() throws Exception {
        checkFile(prj2, main2, "main2.tex");
    }
    
    public void test_included2a_tex_File() throws Exception {
        checkFile(prj2, main2, "included2a.tex");
    }
    
    public void test_included1b_tex_File() throws Exception {
        checkFile(prj1, main1, "included1b.tex");
    }
    
    public void test_included2b_tex_File() throws Exception {
        checkFile(prj2, main2, "included2b.tex");
    }

    public void test_bibdatabase1a_bib_File() throws Exception {
        checkFile(prj1, main1, "bibdatabase1a.bib");
    }

    public void test_bibdatabase1b_bib_File() throws Exception {
        checkFile(prj1, main1, "bibdatabase1b.bib");
    }

    public void test_bibdatabase2a_bib_File() throws Exception {
        checkFile(prj2, main2, "bibdatabase2a.bib");
    }

    public void test_bibdatabase2b_bib_File() throws Exception {
        checkFile(prj2, main2, "bibdatabase2b.bib");
    }
    
    public void testShouldNotBeFound() {
        assertNull("Some project found for file outside of all projects.", FileOwnerQuery.getOwner(notIncluded));
    }
    
}
