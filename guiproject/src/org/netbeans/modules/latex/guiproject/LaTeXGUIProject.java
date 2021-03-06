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
package org.netbeans.modules.latex.guiproject;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.latex.guiproject.build.BuildConfigurationProvider;
import org.netbeans.modules.latex.guiproject.ui.LaTeXGUIProjectCustomizer;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.spi.navigator.NavigatorLookupHint;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.ErrorManager;
import org.openide.actions.FindAction;
import org.openide.filesystems.*;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Jan Lahoda
 */
public class LaTeXGUIProject implements Project, LogicalViewProvider {
    
    private FileObject dir;
    private FileObject masterFile;
    private Lookup lookup;
    
    private PropertyChangeSupport pcs;
    
    public static final String PROP_CONTAINED_FILES = "containedFiles";
    
    public static final String COMMAND_SHOW = "latex-show";//NOI18N
    
    private static final NavigatorLookupHint NAVIGATOR_HINT = new NavigatorHintImpl();
    private static final RequestProcessor WORKER = new RequestProcessor(LaTeXGUIProject.class.getName(), 1, false, false);
    
    private static final Image LaTeXGUIProjectIMAGE;
    private static final Icon LaTeXGUIProjectICON;
    
    static {
        LaTeXGUIProjectIMAGE = ImageUtilities.loadImage("org/netbeans/modules/latex/guiproject/resources/latex_gui_project_icon.png");//NOI18N
        LaTeXGUIProjectICON  = new ImageIcon(LaTeXGUIProjectIMAGE);
    }
    
    /** Creates a new instance of LaTeXGUIProject */
    public LaTeXGUIProject(FileObject dir, FileObject masterFile) {
        this.dir = dir;
        this.masterFile = masterFile;
        pcs = new PropertyChangeSupport(this);
        lookup = Lookups.fixed(new Object[] {
            new Info(),
            this,
            new ActionsFactory(this),
            GenericSources.genericOnly(this),
            masterFile,
            new LaTeXGUIProjectOpenedHookImpl(this),
            new LaTeXAuxiliaryConfigurationImpl(this),
            new LaTeXSharabilityQuery(this),
            new LaTeXGUIProjectCustomizer(this),
            new LaTeXGUIProjectLocaleQueryImplementation(this),
            new BuildConfigurationProvider(this),
        });
    }
    
    public Lookup getLookup() {
        return lookup;
    }
    
    public FileObject getProjectDirectory() {
        return dir;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
    
    /*package private*/FileObject getMasterFile() {
        return masterFile;
    }
    
    public Node createLogicalView() {
        return new LaTeXGUIProjectNode(this);
    }
    
    public Node findPath(Node root, Object target) {
        if (root.getLookup().lookup(LaTeXGUIProject.class) != this)
            return null;
        
        Node[] files = root.getChildren().getNodes(true);
        Lookup.Template<Object> searchingTemplate= new Lookup.Template<Object>(null, null, target);
        
        for (int cntr = 0; cntr < files.length; cntr++) {
            if (files[cntr].getLookup().lookup(searchingTemplate).allInstances().size() > 0)
                return files[cntr];
        }
        
        return null;
    }
    
    public FileObject getMainFile() {
        return masterFile;
    }
    
    private static final String SOURCE_NODE_NAME = "Sources";
    private static final Object SOURCE_NODE_TAG  = new Object();
    
    private Node createSourcesNode() {
        AbstractNode an = new AbstractNode(new LaTeXChildren(this), Lookups.singleton(SOURCE_NODE_TAG));
        
        an.setDisplayName(SOURCE_NODE_NAME);
        
        return an;
    }
    
    private Children createChildren() {
        Children.Array mainChildren = new Children.Array();
        
        mainChildren.add(new Node[] {createSourcesNode()/*, createStructuralNode()*/});
        
        return mainChildren;
    }
    
    /*package private*/ File getProjectInternalDir() {
        return FileUtil.toFile(dir);
    }
    
    private Collection<FileObject> containedFilesCache;
    
    /*package private*/synchronized boolean contains(FileObject file) {
        //TODO: more effeciently:
        if (containedFilesCache == null)
            return Utilities.getDefault().compareFiles(getMainFile(), file);
        else {
            return containedFilesCache.contains(file);
        }
    }
    
    public synchronized Collection<FileObject> getContainedFiles() {
        if (containedFilesCache == null)
            return Collections.singletonList(getMainFile());
        else {
            return containedFilesCache;
        }
    }
    
    synchronized void setContainedFile(Collection<FileObject> files) {
        Set<FileObject> originalFiles = new HashSet<FileObject>();

        if (containedFilesCache != null) {
            originalFiles.addAll(containedFilesCache);
        }
        
        HashSet<FileObject> nueFilesCopy = new HashSet<FileObject>(files);

        boolean modified = !originalFiles.equals(nueFilesCopy);

        containedFilesCache = new LinkedList<FileObject>(files);
        cs.fireChange();

        if (modified) {
            Hacks.revalidateAll(nueFilesCopy);
        }
    }
    
    private ChangeSupport cs = new ChangeSupport(this);
    
    public void addChangeListener(ChangeListener l) {
        cs.addChangeListener(l);
    }
    
    public void removeChangeListener(ChangeListener l) {
        cs.removeChangeListener(l);
    }
    
    private static class LaTeXGUIProjectNode extends AbstractNode implements Runnable, FileStatusListener, PropertyChangeListener {
        
        private LaTeXGUIProject project;
        
        // icon badging >>>
        private Set<FileObject> files;
        private Map<FileSystem, FileStatusListener> fileSystemListeners;
        private RequestProcessor.Task task;
        private final Object privateLock = new Object();
        private boolean iconChange;
        private boolean nameChange;        
        private ChangeListener sourcesListener;
        private Map groupsListeners;
        // icon badging <<<
        
        public LaTeXGUIProjectNode(LaTeXGUIProject project) {
            super(new LaTeXChildren(project), Lookups.fixed(new Object[] {project, NAVIGATOR_HINT, new SearchInfoImpl(project)}));
            setDisplayName(ProjectUtils.getInformation(project).getDisplayName());
            setIconBaseWithExtension("org/netbeans/modules/latex/guiproject/resources/latex_gui_project_icon.png");
            this.project = project;
            setProjectFiles(project);
            project.addPropertyChangeListener(this);
        }
        
        @Override
        public Action[] getActions(boolean context) {
            List<Action> actions = new ArrayList<Action>();
            
            actions.add(ActionsFactory.createShowAction());
            actions.add(null);
            actions.add(ProjectSensitiveActions.projectCommandAction(ActionsFactory.COMMAND_BUILD, "Build Project", null));
            actions.add(ProjectSensitiveActions.projectCommandAction(ActionsFactory.COMMAND_REBUILD, "Clean and Build Project", null));
            actions.add(ProjectSensitiveActions.projectCommandAction(ActionsFactory.COMMAND_CLEAN, "Clean Project", null));
            actions.add(null);
            actions.add(CommonProjectActions.setAsMainProjectAction());
            actions.add(CommonProjectActions.closeProjectAction());

            actions.add(null);
            actions.add(FindAction.get(FindAction.class));
            actions.add(null);
            
            // honor 57874 contact
            
            Iterator<? extends Object> it = Lookups.forPath("Projects/Actions").lookupAll(Object.class).iterator();
            if (it.hasNext()) {
                actions.add(null);
            }
            while (it.hasNext()) {
                Object next = it.next();
                if (next instanceof Action) {
                    actions.add((Action) next);
                } else if (next instanceof JSeparator) {
                    actions.add(null);
                }
            }
            
            actions.add(null);
            actions.add(CommonProjectActions.customizeProjectAction());
            
            return actions.toArray(new Action[actions.size()]);
        }

        
        protected final void setProjectFiles(LaTeXGUIProject project) {
            setFiles(new HashSet<FileObject>(project.getContainedFiles()));
        }
        
        protected final void setFiles(Set<FileObject> files) {
            if (fileSystemListeners != null) {
                Iterator it = fileSystemListeners.keySet().iterator();
                while (it.hasNext()) {
                    FileSystem fs = (FileSystem) it.next();
                    FileStatusListener fsl = fileSystemListeners.get(fs);
                    fs.removeFileStatusListener(fsl);
                }
            }
                        
            fileSystemListeners = new HashMap<FileSystem, FileStatusListener>();
            this.files = files;
            if (files == null) return;

            Iterator it = files.iterator();
            Set<FileSystem> hookedFileSystems = new HashSet<FileSystem>();
            while (it.hasNext()) {
                FileObject fo = (FileObject) it.next();
                try {
                    FileSystem fs = fo.getFileSystem();
                    if (hookedFileSystems.contains(fs)) {
                        continue;
                    }
                    hookedFileSystems.add(fs);
                    FileStatusListener fsl = FileUtil.weakFileStatusListener(this, fs);
                    fs.addFileStatusListener(fsl);
                    fileSystemListeners.put(fs, fsl);
                } catch (FileStateInvalidException e) {
                    ErrorManager err = ErrorManager.getDefault();
                    Exceptions.attachLocalizedMessage(e, "Can not get " + fo + " filesystem, ignoring...");  // NO18N
                    err.notify(ErrorManager.INFORMATIONAL, e);
                }
            }
        }

        @Override
        public String getDisplayName () {
            String s = super.getDisplayName ();

            if (files != null && files.iterator().hasNext()) {
                try {
                    FileObject fo = files.iterator().next();
                    s = fo.getFileSystem ().getDecorator().annotateName (s, files);
                } catch (FileStateInvalidException e) {
                    Logger.getLogger("global").log(Level.INFO,null, e);
                }
            }

            return s;
        }

        @Override
         public String getHtmlDisplayName() {
            if (files != null && files.iterator().hasNext()) {
                try {
                    FileObject fo = files.iterator().next();
                    StatusDecorator decorator = fo.getFileSystem().getDecorator();
                    String result = decorator.annotateNameHtml (
                       super.getDisplayName(), files);

                    //Make sure the super string was really modified
                    if (result != null && !result.equals(getDisplayName())) {
                        return result;
                    }
                } catch (FileStateInvalidException e) {
                    Logger.getLogger("global").log(Level.INFO,null, e);
                }
            }
            return getDisplayName();
        }
         
        @Override
        public Image getIcon (int type) {
            Image img = super.getIcon(type);

            if (files != null && files.iterator().hasNext()) {
                try {
                    FileObject fo = files.iterator().next();
                    img = FileUIUtils.getImageDecorator(fo.getFileSystem()).annotateIcon(img, type, files);
                } catch (FileStateInvalidException e) {
                    Logger.getLogger("global").log(Level.INFO,null, e);
                }
            }

            return img;
        }
        @Override
        public Image getOpenedIcon (int type) {
            Image img = super.getOpenedIcon(type);

            if (files != null && files.iterator().hasNext()) {
                try {
                    FileObject fo = files.iterator().next();
                    img = FileUIUtils.getImageDecorator(fo.getFileSystem()).annotateIcon(img, type, files);
                } catch (FileStateInvalidException e) {
                    Logger.getLogger("global").log(Level.INFO,null, e);
                }
            }

            return img;
        }

        public void run() {
            boolean fireIcon;
            boolean fireName;
            synchronized (privateLock) {
                fireIcon = iconChange;
                fireName = nameChange;
                iconChange = false;
                nameChange = false;
            }
            if (fireIcon) {
                fireIconChange();
                fireOpenedIconChange();
            }
            if (fireName) {
                fireDisplayNameChange(null, null);
            }
        }

        public void annotationChanged(FileStatusEvent event) {
            if (task == null) {
                task = WORKER.create(this);
            }

            synchronized (privateLock) {
                if ((iconChange == false && event.isIconChange())  || (nameChange == false && event.isNameChange())) {
                    Iterator it = files.iterator();
                    while (it.hasNext()) {
                        FileObject fo = (FileObject) it.next();
                        if (event.hasChanged(fo)) {
                            iconChange |= event.isIconChange();
                            nameChange |= event.isNameChange();
                        }
                    }
                }
            }

            task.schedule(50);  // batch by 50 ms
        }

        public void propertyChange(PropertyChangeEvent evt) {
            setProjectFiles(project);
         }

    }
    
    
    private static class LaTeXChildren extends Children.Keys<FileObject> implements ChangeListener/*TODO: Weak?*/ {

        private LaTeXGUIProject project;

        public LaTeXChildren(LaTeXGUIProject project) {
            this.project = project;
        }

        @Override
        public void addNotify() {
            project.addChangeListener(this);
            doSetKeys();
        }
        
        private void doSetKeys() {
//            Thread.dumpStack();
            List<FileObject> toAdd = new ArrayList<FileObject>();
            
            toAdd.addAll(project.getContainedFiles());
            
            FileObject main = project.getMainFile();
            
            toAdd.remove(main);
            toAdd.add(0, main);
            
//            System.err.println("toAdd=" + toAdd);
            setKeys(toAdd);
        }
        
        protected Node[] createNodes(FileObject key) {
            try {
                DataObject od = DataObject.find(key);

                return new Node[]{new SourceFileNode(od.getNodeDelegate(), key)};
            } catch (DataObjectNotFoundException e) {
                Exceptions.printStackTrace(e);
                return new Node[0];
            }
        }

        public void stateChanged(ChangeEvent e) {
            doSetKeys();
        }

    }
    
    private static class SourceFileNode extends FilterNode {
        public SourceFileNode(Node shadow, FileObject file) {
            super(shadow, Children.LEAF, new ProxyLookup(new Lookup[] {shadow.getLookup(), Lookups.singleton(file)}));
        }
    }
    
    private class Info implements ProjectInformation {
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }
        
        public String getDisplayName() {
            return getName();
        }
        
        public Icon getIcon() {
            return LaTeXGUIProjectICON;
        }
        
        public String getName() {
            return masterFile.getNameExt();
        }
        
        public Project getProject() {
            return LaTeXGUIProject.this;
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }
        
    }
    
}
