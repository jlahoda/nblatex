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
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2005.
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
package org.netbeans.modules.latex.bibtex.nodes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.Action;
import org.netbeans.modules.latex.bibtex.*;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.modules.latex.model.bibtex.BiBTeXModel;

import org.netbeans.modules.latex.model.bibtex.Entry;

import org.openide.actions.DeleteAction;
import org.openide.actions.OpenAction;
import org.openide.actions.PropertiesAction;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jan Lahoda
 */
public class BiBEntryNode extends AbstractNode implements PropertyChangeListener {
    private FileObject source;
    private Entry entry;
    
    public BiBEntryNode(Entry entry, FileObject source) {
        super(Children.LEAF, Lookups.fixed(new Object[] {entry, new OpenCookieImpl(entry)}));
        this.entry = entry;
        this.source = source;
        entry.addPropertyChangeListener(this);
        
        setDisplayName("<unknown>");
    }
    
    public Action[] getActions(boolean context) {
        return new Action[] {
            OpenAction.get(OpenAction.class),
            null,
            EditEntryAction.get(EditEntryAction.class),
            SystemAction.get(DeleteAction.class),
            null,
            SystemAction.get(PropertiesAction.class)
        };
    }
    
    public Entry getEntry() {
        return entry;
    }
    
    public boolean canDestroy() {
        return true;
    }
    
    public Action getPreferredAction() {
        return OpenAction.get(OpenAction.class);
    }
    
    public void destroy() throws IOException {
        BiBTeXModel.getModel(source).removeEntry(getEntry());
        super.destroy();
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
        //TODO: this is not absolutely correct, see the IllegalStateException being thrown to the console.
        firePropertyChange(evt.getPropertyName(), null, null);
    }
    
    private static final class OpenCookieImpl implements OpenCookie {
        
        private Entry entry;
        
        public OpenCookieImpl(Entry entry) {
            this.entry = entry;
        }
        
        public void open() {
            Utilities.getDefault().openPosition(entry.getStartPosition());
        }
        
    }
}