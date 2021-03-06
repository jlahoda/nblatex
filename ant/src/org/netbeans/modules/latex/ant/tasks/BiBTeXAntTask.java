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
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2004.
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
package org.netbeans.modules.latex.ant.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;

/**
 *
 * @author Jan Lahoda
 */
public class BiBTeXAntTask extends Task {
    
    private String mainFile;
    private String command   = "bibtex";
    private String arguments = "";
    
    /** Creates a new instance of LaTeXAntTask */
    public BiBTeXAntTask() {
        mainFile = null;
    }
    
    public void setMainfile(String mainFile) {
        this.mainFile = mainFile;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    private boolean isUpToDate(File source) {
        File result = Utilities.replaceExtension(source, ".bbl");
        
        if (!Utilities.isUpToDate(source, result))
            return false;
        
        File aux = Utilities.replaceExtension(source, ".aux");
        
        if (!aux.exists())
            return false;
        
        try {
            InputStream ins = new BufferedInputStream(new FileInputStream(aux));
            StringBuffer content = new StringBuffer();
            int read;
            
            while ((read = ins.read()) != (-1)) {
                content.append((char) read);
            }
            
            String contentString = content.toString();
            
            Pattern p = Pattern.compile("\\\\bibdata\\{([^}]*)\\}");
            Matcher m = p.matcher(contentString);
            
            while (m.find()) {
                String database = m.group(1);
                File   databaseFile = new File(source.getParentFile(), database);
                
                if (!databaseFile.exists())
                    databaseFile = new File(source.getParentFile(), database + ".bib");
                
                if (databaseFile.exists()) {
                    if (!Utilities.isUpToDate(databaseFile, result))
                        return false;
                }
            }
        } catch (IOException e) {
            log("BiBTeX: During checking for up-to-date, IOException=" + e.getLocalizedMessage(), Project.MSG_VERBOSE);
            return false;
        }
        
        return true;
    }
    
    public void execute() throws BuildException {
        if (mainFile == null)
            throw new BuildException("The mainfile has to be set!");
        
        File source = Utilities.resolveFile(getProject(), mainFile);

        log("BiBTeX: " + source, Project.MSG_VERBOSE);
        
        if (isUpToDate(source)) {
            log("BiBTeX: Up-to-date, no processing.", Project.MSG_VERBOSE);
        } else {
            Commandline cmdLine = new Commandline();
            
            cmdLine.setExecutable(command);
            cmdLine.addArguments(Commandline.translateCommandline(arguments));
            
            File aux = Utilities.replaceExtension(source, ".aux");
            
            cmdLine.addArguments(new String[] {aux.getName()});
            
            Execute exec = new Execute();
            
            exec.setCommandline(cmdLine.getCommandline());
            exec.setWorkingDirectory(aux.getParentFile());
            
            try {
                exec.execute();
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
    }
    
}
