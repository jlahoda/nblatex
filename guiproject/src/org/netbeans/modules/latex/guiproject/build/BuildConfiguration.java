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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.latex.guiproject.build;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.swing.text.Document;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.guiproject.LaTeXGUIProject;
import org.netbeans.modules.latex.guiproject.Utilities;
import org.netbeans.modules.latex.guiproject.ui.ProjectSettings;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DefaultTraverseHandler;
import org.netbeans.modules.latex.model.platform.LaTeXPlatform;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.HintsController;
import org.openide.ErrorManager;
import org.openide.execution.NbProcessDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.MapFormat;
import org.openide.util.NbBundle;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

/**
 *
 * @author Jan Lahoda
 */
public final class BuildConfiguration implements Builder {

    private static final ErrorManager ERR = ErrorManager.getDefault().getInstance(BuildConfiguration.class.getName());
    private static final Pattern      RERUN_PATTERN = Pattern.compile("[Rr]erun to");

    private String   name;
    private String   displayName;
    private String[] tools;
    
    /** Creates a new instance of BuildConfiguration */
    BuildConfiguration(String name, String displayName, String[] tools) {
        this.name        = name;
        this.displayName = displayName;
        this.tools       = tools;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean build(final LaTeXGUIProject p, final InputOutput inout) {
        if (getErrorIfAny(p) != null)
            throw new IllegalArgumentException();
        
        FileObject file = p.getMainFile();
        File wd = FileUtil.toFile(file.getParent());
        LaTeXPlatform platform = Utilities.getPlatform(p);
        Map<String, String> format = new HashMap<String, String>();
        boolean result = true;
        
        format.put(LaTeXPlatform.ARG_INPUT_FILE_BASE, file.getName());
        
        for (String tool : tools) {
            NbProcessDescriptor desc = platform.getTool(tool);
            
            if (LaTeXPlatform.TOOL_LATEX.equals(tool)) {
                result &= runLaTeX(p, file, format, wd, inout);
            } else {
                result &= BuildConfiguration.run(desc, format, wd, inout.getOut(), inout.getErr(), null).passed();
            }
            
            if (!result) {
                break;
            }
        }
        
        file.getParent().refresh(false);
        
        for (FileObject child : file.getChildren()) {
            child.refresh();
        }
        
        return result;
    }


    private boolean runLaTeX(LaTeXGUIProject p, FileObject mainFile, Map format, File wd, InputOutput inout) {
        boolean forceReparse = false;
        LaTeXPlatform platform = Utilities.getPlatform(p);
        NbProcessDescriptor latex  = platform.getTool(LaTeXPlatform.TOOL_LATEX);
        NbProcessDescriptor bibtex = platform.getTool(LaTeXPlatform.TOOL_BIBTEX);
        int pass = 0;
        boolean again = true;
        
        while (again && pass < 5) {
            boolean doLatex = true;
            
            again = false;
            
//            if (isUpToDate()) {
//                if (pass == 0) {
//                    //The first pass is assured always, but there is no point in latexing...
//                    ERR.log(ErrorManager.INFORMATIONAL, "Up-to-date mainfile, first pass, included files not checked, latexing forced.");
//                    doLatex = true;
//                } else {
//                    if (forceReparse) {
//                        ERR.log(ErrorManager.INFORMATIONAL, "Up-to-date, forceReparse == true.");
//                        doLatex = true;
//                    } else {
//                        ERR.log(ErrorManager.INFORMATIONAL, "Up-to-date, no latexing, exit");
//                        break;
//                    }
//                }
//            }

            if (doLatex) {
                ERR.log(ErrorManager.INFORMATIONAL, "LaTeXing, mainfile:" + mainFile);
                
                Result result = BuildConfiguration.run(latex, format, wd, inout.getOut(), inout.getErr(), RERUN_PATTERN);

                if (result == Result.FAILED)
                    return false;

                again |= result == Result.PASSED_AND_RERUN;
            }
            
            if (pass == 0) {
                if (shouldRunBiBTeX(p)) {
                    ERR.log(ErrorManager.INFORMATIONAL, "Running bibtex tasks.");
                    
                    Result result = BuildConfiguration.run(bibtex, format, wd, inout.getOut(), inout.getErr(), null);
                    
//                    if (!result)
//                        return false;

                    again = true;
                }
            }
            
            pass++;
        }

        return true;
    }

    private boolean shouldRunBiBTeX(LaTeXGUIProject p) {
        switch (ProjectSettings.getDefault(p).getBiBTeXRunType()) {
            case YES:
                return true;
            case NO:
                return false;
            case AUTO:
            default:
                final boolean [] result = new boolean[1];
                Source source = Source.create(p.getMainFile());
                
                try {
                    ParserManager.parse(Collections.singleton(source), new UserTask() {
                        @Override
                        public void run(ResultIterator resultIterator) throws Exception {
                            LaTeXParserResult.get(resultIterator).getDocument().traverse(new DefaultTraverseHandler() {
                                @Override
                                public boolean commandStart(CommandNode node) {
                                    if ("\\bibliography".equals(node.getCommand().getCommand())) {
                                        result[0] = true;
                                        return false;
                                    }

                                    return true;
                                }
                            });
                        }
                    });
                } catch (ParseException e) {
                    Exceptions.printStackTrace(e);
                }
                
                return result[0];
        }
    }
    
    public boolean clean(final LaTeXGUIProject p, final InputOutput inout) {
        if (getErrorIfAny(p) != null)
            throw new IllegalArgumentException();

        FileObject file = p.getMainFile();
        LaTeXPlatform platform = Utilities.getPlatform(p);
        List<URI> targets = new ArrayList<URI>();
        
        for (String tool : tools) {
            targets.addAll(platform.getTargetFiles(tool, file));
        }
        
        for (URI u : targets) {
            try {
                FileObject f = URLMapper.findFileObject(u.toURL());
                
                if (f == null) {
                    //the file does not exist
                    continue;
                }
                
                File toDelete = FileUtil.toFile(f);
                
                inout.getOut().println("Going to delete: " + toDelete.getAbsolutePath());
                
                toDelete.delete();
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            }
        }
        
        return true;
    }

    public String getErrorIfAny(LaTeXGUIProject p) {
        LaTeXPlatform platform = Utilities.getPlatform(p);
        
        for (String tool : tools) {
            if (!platform.isToolConfigured(tool))
                return NbBundle.getMessage(BuildConfiguration.class, "LBL_ToolNotConfigured", new Object[] {String.valueOf(tool)});
        }
        
        return null;
    }

    public boolean isSupported(LaTeXGUIProject p) {
        return getErrorIfAny(p) == null;
    }
    
    private static Result run(NbProcessDescriptor descriptor, Map format, File wd, OutputWriter stdOut, OutputWriter stdErr, Pattern rerunPattern) {
        try {
            Process process = descriptor.exec(new MapFormat(format), null, true, wd);
            
            Map<Document, List<ErrorDescription>> errors = new HashMap<Document, List<ErrorDescription>>();
            LaTeXCopyMaker scOut = new LaTeXCopyMaker(wd, process.getInputStream(), stdOut, rerunPattern);
            LaTeXCopyMaker scErr = new LaTeXCopyMaker(wd, process.getErrorStream(), stdErr);
            
            scOut.start();
            scErr.start();
            
            scOut.join();
            scErr.join();

            boolean result = process.waitFor() == 0;
            
            errors.putAll(scOut.getErrors());
            errors.putAll(scErr.getErrors());
            
            for (Entry<Document, List<ErrorDescription>> e : errors.entrySet()) {
                HintsController.setErrors(e.getKey(), BuildConfiguration.class.getName(), e.getValue());
            }
            return result ? scOut.containsPattern() ? Result.PASSED_AND_RERUN : Result.PASSED : Result.FAILED;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return Result.FAILED;
        }
        
        return Result.PASSED;
    }

    String[] getTools() {
        return tools;
    }

    private enum Result {
        FAILED(true), PASSED(true), PASSED_AND_RERUN(true);

        private final boolean p;

        private Result(boolean p) {
            this.p = p;
        }

        public boolean passed() {
            return p;
        }
    }
}
