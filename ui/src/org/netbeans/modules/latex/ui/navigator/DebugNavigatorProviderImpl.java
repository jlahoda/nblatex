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
package org.netbeans.modules.latex.ui.navigator;

import java.awt.BorderLayout;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.command.DebuggingSupport;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.spi.navigator.NavigatorPanel;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;


/**Copied from ant module
 *
 * @author Jesse Glick, Jan Lahoda
 */
public class DebugNavigatorProviderImpl implements NavigatorPanel {
    
    private JComponent panel;
    private final ExplorerManager manager = new ExplorerManager();
    
    /**
     * Default constructor for layer instance.
     */
    public DebugNavigatorProviderImpl() {
        manager.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                    if (manager.getSelectedNodes().length == 1) {
                        Node n = manager.getSelectedNodes()[0];
                        org.netbeans.modules.latex.model.command.Node node = (org.netbeans.modules.latex.model.command.Node) n.getLookup().lookup(org.netbeans.modules.latex.model.command.Node.class);
                        
                        DebuggingSupport.getDefault().setCurrentSelectedNode(node);
                    }
                }
            }
        });
    }
    
    public String getDisplayName() {
        return NbBundle.getMessage(LaTeXNavigatorPanel.class, "LBL_DebuggingTree");//NOI18N
    }
    
    public String getDisplayHint() {
        return NbBundle.getMessage(LaTeXNavigatorPanel.class, "SD_DebuggingTree");//NOI18N
    }
    
    public JComponent getComponent() {
        if (panel == null) {
            final BeanTreeView view = new BeanTreeView();
            view.setRootVisible(false);
            view.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            class Panel extends JPanel implements ExplorerManager.Provider, Lookup.Provider {
                // Make sure action context works correctly:
                private final Lookup lookup = ExplorerUtils.createLookup(manager, new ActionMap());
                {
                    setLayout(new BorderLayout());
                    add(view, BorderLayout.CENTER);
                }
                public ExplorerManager getExplorerManager() {
                    return manager;
                }
                public Lookup getLookup() {
                    return lookup;
                }
            }
            panel = new Panel();
        }
        return panel;
    }
    
    public void panelActivated(Lookup context) {
        instance.set(this);
    }

    public void panelDeactivated() {
        instance.set(null);
    }

    public Lookup getLookup() {
        return null;
    }

    private static final AtomicReference<DebugNavigatorProviderImpl> instance = new AtomicReference<DebugNavigatorProviderImpl>();

    public final static class TaskImpl extends ParserResultTask<LaTeXParserResult> {
        @Override
        public void run(LaTeXParserResult lpr, SchedulerEvent event) {
            DebugNavigatorProviderImpl instance = DebugNavigatorProviderImpl.instance.get();

            if (instance == null) return ;
            
            try {
                instance.manager.setRootContext(NodeNode.constructRootNodeFor(lpr.getDocument()));
            } catch (IntrospectionException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public int getPriority() {
            return 100;
        }

        @Override
        public Class<? extends Scheduler> getSchedulerClass() {
            return Scheduler.SELECTED_NODES_SENSITIVE_TASK_SCHEDULER;
        }

        @Override
        public void cancel() {}
        
    }
    
}
