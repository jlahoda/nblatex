/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2007.
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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ActionProvider;
import org.openide.util.Lookup;

/**
 *
 * @author Jan Lahoda
 */
public class LaTeXGUIProjectTest extends ProjectTestCase {

    public LaTeXGUIProjectTest(java.lang.String testName) {
        super(testName);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();//LaTeXGUIProjectTest.class);
        
        //only one test is currently working:
        suite.addTest(TestSuite.createTest(LaTeXGUIProjectTest.class, "testGetLookup"));
        
        return suite;
    }

    protected void tearDown() throws java.lang.Exception {
    }

    /**
     * Test of getLookup method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetLookup() throws IOException {
        Project p = ProjectManager.getDefault().findProject(prj1Impl);
        Lookup l = p.getLookup();
        
        assertNotNull("ActionProvider missing in the project lookup.", l.lookup(ActionProvider.class));
        //TODO: other...
    }

    /**
     * Test of getProjectDirectory method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetProjectDirectory() {

        System.out.println("testGetProjectDirectory");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of getDisplayName method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetDisplayName() {

        System.out.println("testGetDisplayName");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of getIcon method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetIcon() {

        System.out.println("testGetIcon");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of getName method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetName() {

        System.out.println("testGetName");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of getProject method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetProject() {

        System.out.println("testGetProject");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of createLogicalView method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testCreateLogicalView() {

        System.out.println("testCreateLogicalView");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of findPath method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testFindPath() {

        System.out.println("testFindPath");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of getSource method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetSource() {

        System.out.println("testGetSource");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of getSupportedActions method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetSupportedActions() {

        System.out.println("testGetSupportedActions");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of invokeAction method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testInvokeAction() {

        System.out.println("testInvokeAction");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of getProjectInternalDir method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testGetProjectInternalDir() {

        System.out.println("testGetProjectInternalDir");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of contains method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testContains() {

        System.out.println("testContains");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of isActionEnabled method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testIsActionEnabled() {

        System.out.println("testIsActionEnabled");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }

    /**
     * Test of showCustomizer method, of class org.netbeans.modules.latex.guiproject.LaTeXGUIProject.
     */
    public void testShowCustomizer() {

        System.out.println("testShowCustomizer");
        
        // TODO add your test code below by replacing the default call to fail.
        fail("The test case is empty.");
    }
    
    // TODO add test methods here, they have to start with 'test' name.
    // for example:
    // public void testHello() {}
    
    
}
