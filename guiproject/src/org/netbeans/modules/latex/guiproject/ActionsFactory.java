/*
 *                          Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the LaTeX module.
 * The Initial Developer of the Original Code is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2006.
 * All Rights Reserved.
 *
 * Contributor(s): Jan Lahoda.
 */
package org.netbeans.modules.latex.guiproject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.latex.guiproject.build.BuildConfiguration;
import org.netbeans.modules.latex.guiproject.build.BuildConfigurationProvider;
import org.netbeans.modules.latex.guiproject.build.ShowConfiguration;
import org.netbeans.modules.latex.guiproject.ui.ProjectSettings;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.support.MainProjectSensitiveActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.ErrorManager;
import org.openide.LifecycleManager;
import org.openide.execution.ExecutionEngine;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.modules.InstalledFileLocator;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author Jan Lahoda
 */
public class ActionsFactory implements ActionProvider {
    
    private LaTeXGUIProject project;
    
    /** Creates a new instance of ActionsFactory */
    public ActionsFactory(LaTeXGUIProject project) {
        this.project = project;
    }
    
    public String[] getSupportedActions() {
        return new String[] {
            COMMAND_BUILD,
            COMMAND_CLEAN,
            COMMAND_REBUILD,
            LaTeXGUIProject.COMMAND_SHOW
        };
    }

    private static Map<String, String> command2DisplayName;

    static {
        command2DisplayName = new HashMap();

        command2DisplayName.put(COMMAND_CLEAN, "clean");
        command2DisplayName.put(COMMAND_REBUILD, "rebuild");
        command2DisplayName.put(COMMAND_BUILD, "build");
        command2DisplayName.put(LaTeXGUIProject.COMMAND_SHOW, "show");
    }

    public void invokeAction(final String command, Lookup context) throws IllegalArgumentException {
        String name = ProjectUtils.getInformation(project).getDisplayName() + "(" + command2DisplayName.get(command) + ")";
        final InputOutput inout = IOProvider.getDefault().getIO(name, false);

        try {
            inout.getOut().reset();
            inout.select();
            
            class Exec implements Runnable {
                public void run() {
                    LifecycleManager.getDefault().saveAll();
                    if (doBuild()) {
                        inout.getOut().println("Build passed.");
                    } else {
                        inout.getOut().println("Build failed, more info should be provided above.");
                    }
                }

                private boolean doBuild() {
                    if (COMMAND_CLEAN.equals(command) || COMMAND_REBUILD.equals(command)) {
                        BuildConfiguration conf = BuildConfigurationProvider.getDefault().getBuildConfiguration(ProjectSettings.getDefault(project).getBuildConfigurationName());
                        
                        if (!conf.clean(project, inout))
                            return false;
                    }
                    
                    if (COMMAND_BUILD.equals(command) || COMMAND_REBUILD.equals(command) || LaTeXGUIProject.COMMAND_SHOW.equals(command)) {
                        BuildConfiguration conf = BuildConfigurationProvider.getDefault().getBuildConfiguration(ProjectSettings.getDefault(project).getBuildConfigurationName());
                        
                        if (!conf.build(project, inout))
                            return false;
                    }
                    
                    if (LaTeXGUIProject.COMMAND_SHOW.equals(command)) {
                        ShowConfiguration conf = BuildConfigurationProvider.getDefault().getShowConfiguration(ProjectSettings.getDefault(project).getShowConfigurationName());
                        
                        if (!conf.build(project, inout))
                            return false;
                    }

                    return true;
                }
            }
            
            ExecutionEngine.getDefault().execute(name, new Exec(), inout);
        } catch (IOException io) {
            throw new IllegalStateException(io);
        } finally {
            inout.getOut().close();
            inout.getErr().close();
        }
    }
    
    public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
        return true;//TODO:....
    }

    private static File findLideClient() {
        return InstalledFileLocator.getDefault().locate("modules/bin/lide-editor-client", null, false);
    }
    
    public static Action createShowAction() {
        return ProjectSensitiveActions.projectCommandAction(LaTeXGUIProject.COMMAND_SHOW, "Show Project Resulting Document", null);
    }
    
    public static Action createMainProjectShowAction() {
        return MainProjectSensitiveActions.mainProjectCommandAction(LaTeXGUIProject.COMMAND_SHOW, "Show Main Project Resulting Document", null);
    }


    
}
