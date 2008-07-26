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
package org.netbeans.modules.latex.ui.viewer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.api.CancellableTask;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.platform.FilePosition;
import org.netbeans.napi.gsfret.source.CompilationInfo;
import org.netbeans.napi.gsfret.source.Phase;
import org.netbeans.napi.gsfret.source.Source.Priority;
import org.netbeans.napi.gsfret.source.support.CaretAwareSourceTaskFactory;
import org.netbeans.napi.gsfret.source.support.EditorAwareSourceTaskFactory;
import org.netbeans.spi.project.ActionProvider;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jan Lahoda
 */
public class ProjectRebuilDer implements FileChangeListener {
    
    public static final ProjectRebuilDer INSTANCE = new ProjectRebuilDer();

    private static final int DELAY = 500;
    
    private Project p;
    private DocumentTopComponent dtc;
    
    public synchronized void registerProject(Project p, DocumentTopComponent dtc) {
        unregisterProject(this.p);
        
        if (p == null) {
            return ;
        }
        
        FileObject masterFile = p.getLookup().lookup(FileObject.class);
        
        if (masterFile == null) {
            return ;
        }
        
        this.p = p;
        this.dtc = dtc;
        
        FactoryImpl.getInstance().reschedule();
    }
    
    private final Set<FileObject> files = new HashSet<FileObject>();
    
    public synchronized void setFiles(Collection<FileObject> files) {
        if (files.isEmpty()) {
            return ;
        }
        
        if (this.p != FileOwnerQuery.getOwner(files.iterator().next())) {
            return ;
        }
        
        doSetFiles(files);
    }
    
    private synchronized void caretMoved(final CompilationInfo info) throws IOException {
        if (dtc != null && this.p == FileOwnerQuery.getOwner(dtc.getFile())) {
            Document doc = info.getDocument();
            if (doc == null) {
                return;
            }

            FileObject file = info.getFileObject();
            int offset = CaretAwareSourceTaskFactory.getLastPosition(file);
            final FilePosition pos = new FilePosition(file, NbDocument.findLineNumber((StyledDocument) doc, offset), NbDocument.findLineColumn((StyledDocument) doc, offset));
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    synchronized (ProjectRebuilDer.this) {
                        dtc.setFilePosition(pos);
                    }
                }
            });
        }
    }
    
    private static final class ReparseTaskImpl implements CancellableTask<CompilationInfo> {

        public void cancel() {
        }

        public void run(CompilationInfo parameter) throws Exception {
            INSTANCE.setFiles((Collection<FileObject>) LaTeXParserResult.get(parameter).getDocument().getFiles());
        }
        
    }
    
    public static final class FactoryImpl extends EditorAwareSourceTaskFactory {

        public FactoryImpl() {
            super(Phase.RESOLVED, Priority.NORMAL);
        }

        @Override
        protected CancellableTask<CompilationInfo> createTask(FileObject file) {
            return new ReparseTaskImpl();
        }
        
        public void reschedule() {
            for (FileObject f : getFileObjects()) {
                reschedule(f);
            }
        }
        
        public static FactoryImpl getInstance() {
            return Lookup.getDefault().lookup(FactoryImpl.class);
        }
    }
    
    public static final class CaretFactoryImpl extends CaretAwareSourceTaskFactory {
        
        public CaretFactoryImpl() {
            super(Phase.RESOLVED, Priority.NORMAL);
        }

        @Override
        protected CancellableTask<CompilationInfo> createTask(FileObject file) {
            return new CancellableTask<CompilationInfo>() {
                public void cancel() {}
                public void run(CompilationInfo parameter) throws Exception {
                    ProjectRebuilDer.INSTANCE.caretMoved(parameter);
                }
            };
        }
        
    }

    public synchronized void fileChanged(FileEvent fe) {
        if (p == null) {
            return;
        }
        
        final ActionProvider ap = p.getLookup().lookup(ActionProvider.class);

        if (ap == null) {
            return;
        }

        WORKER.post(new Runnable() {
            public void run() {
                ap.invokeAction(ActionProvider.COMMAND_BUILD, Lookup.EMPTY);
            }
        }, DELAY);
    }

    public void fileFolderCreated(FileEvent fe) {}

    public void fileDataCreated(FileEvent fe) {}

    public void fileDeleted(FileEvent fe) {}

    public void fileRenamed(FileRenameEvent fe) {}

    public void fileAttributeChanged(FileAttributeEvent fe) {}

    public synchronized void unregisterProject(Project p) {
        if (this.p == p) {
            this.p = null;
            this.dtc = null;
            doSetFiles(Collections.<FileObject>emptyList());
        }
    }

    private void doSetFiles(Collection<FileObject> files) {
        Set<FileObject> added = new HashSet<FileObject>(files);
        Set<FileObject> removed = new HashSet<FileObject>(this.files);

        added.removeAll(this.files);
        removed.removeAll(files);

        for (FileObject f : removed) {
            f.removeFileChangeListener(this);
        }
        for (FileObject f : added) {
            f.addFileChangeListener(this);
        }

        this.files.removeAll(removed);
        this.files.addAll(added);
    }

    private static final RequestProcessor WORKER = new RequestProcessor("LaTeX Compile on Save", 1);
}
