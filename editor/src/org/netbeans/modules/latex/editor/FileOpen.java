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
 * Portions created by Jan Lahoda_ are Copyright (C) 2002,2003.
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
package org.netbeans.modules.latex.editor;

import java.io.File;
import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;

import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.windows.WindowManager;

/**
 *
 * @author  Jan Lahoda
 */
public abstract class FileOpen {
    
    /** Creates a new instance of FileOpen */
    private FileOpen() {
    }
    
    private static FileOpen instance = null;
    
    public static synchronized FileOpen getDefault() {
        if (instance == null) {
            //In the IDE?
            try {
                Class.forName("org.openide.filesystems.FileObject");
                instance = new NBFileOpen();
            } catch (ClassNotFoundException e) {
                //StandAlone:
                instance = new SAFileOpen();
            }
        }
        
        return instance;
    }
    
    /** WARNING: It should be called out from thread that cannot lock with EditorCookie.open!
     *  @return value if the file is open
     *          null  if the file is not open (it was not possible to find it).
     */
    public abstract JTextComponent assureOpen(File fileName);
    
    public abstract void showApplicationWindow();
    
    private static class NBFileOpen extends FileOpen {
        
        public JTextComponent assureOpen(File fileName) {
            fileName = FileUtil.normalizeFile(fileName);
            
            FileObject toOpenFO = FileUtil.toFileObject(fileName);
            
            if (toOpenFO == null)
                return null; //Cannot open the file.
            
            try {
                DataObject toOpenDO = DataObject.find(toOpenFO);
                
                EditorCookie ec = (EditorCookie) toOpenDO.getLookup().lookup(EditorCookie.class);
                
                if (ec == null)
                    return null;
                
                JEditorPane[] panes = ec.getOpenedPanes();
                
                if (panes == null || panes.length == 0) {
                    ec.open();
                    
                    while ((panes = ec.getOpenedPanes()) == null || panes.length == 0) {
                        try {
                            Thread.sleep(100); //Wait until it is open. It should be opened or a deadlock occurs.
                        } catch (InterruptedException e) {
                            //Nothing...
                        }
                    }
                }
                
                return panes[0];
            } catch (DataObjectNotFoundException e) {
                ErrorManager.getDefault().notify(e);
                return null;
            }
        }
        
        public void showApplicationWindow() {
            WindowManager.getDefault().getMainWindow().toFront();
            WindowManager.getDefault().getMainWindow().requestFocus();
        }
        
    }
    
    private static class SAFileOpen extends FileOpen {
        
        public JTextComponent assureOpen(File fileName) {
            return null;//.
        }
        
        public void showApplicationWindow() {
            //ignore..
        }
        
    }
    
}
