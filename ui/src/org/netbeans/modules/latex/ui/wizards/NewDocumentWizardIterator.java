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
package org.netbeans.modules.latex.ui.wizards;

import java.awt.Component;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.modules.latex.model.command.Option;

import org.openide.WizardDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
public class NewDocumentWizardIterator implements TemplateWizard.Iterator {
    
    private WizardDescriptor.Panel<WizardDescriptor>[] panels;
    private String[]                 panelNames;
    private boolean                  initialized;
    private int                      current;
    
    /** Creates a new instance of NewDocumentWizardDescriptor */
    public NewDocumentWizardIterator() {
        initialized = false;
        panels = null;
        current = 0;
    }
    
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        if (!initialized)
            throw new IllegalStateException("");
        
        Component c = panels[current].getComponent();
        
        if (c instanceof JComponent)
            ((JComponent) c).putClientProperty("WizardPanel_contentSelectedIndex", new Integer(current));
        
        return panels[current];
    }
    
    public boolean hasNext() {
        if (!initialized)
            throw new IllegalStateException("");
        
        return current + 1 < panels.length;
    }
    
    public boolean hasPrevious() {
        if (!initialized)
            throw new IllegalStateException("");
        
        return current > 0;
    }
    
    @SuppressWarnings("unchecked")
    private WizardDescriptor.Panel<WizardDescriptor>[] createPanels() {
        return new WizardDescriptor.Panel[2];
    }
    
    public void initialize(TemplateWizard wiz) {
        wiz.putProperty("WizardPanel_autoWizardStyle", Boolean.TRUE);
        panels = createPanels();
        
        panels[0] = wiz.targetChooser();
        panels[1] = new ContentPanel();
        
        panelNames = new String[2];
        
        Component chooser = panels[0].getComponent();
        
        panelNames[0] = chooser.getName();
        panelNames[1] = "LaTeX Info";

        if (chooser instanceof JComponent) {
            ((JComponent) chooser).putClientProperty("WizardPanel_contentData", panelNames);
        }
        
        current = 0;
        initialized = true;
    }
    
    private String preparePreamble(LaTeXWizardData data) {
        Option fontSize = data.getFontSize();
        Option paperSize = data.getPaperSize();
        
        boolean      isFontDefault = fontSize == null || fontSize.hasAttribute("default-fontsize");
        boolean      isPaperDefault = paperSize == null || paperSize.hasAttribute("default-papersize");
        boolean      hasOptionalOptions = data.getOptions().length > 0;
        StringBuffer result = new StringBuffer();
        
        result.append("\\\\documentclass");
        
        if (!isFontDefault || !isPaperDefault || hasOptionalOptions) {
            result.append("[");
            
            if (!isFontDefault) {
                result.append(fontSize.getName());
                
                if (!isPaperDefault || hasOptionalOptions) {
                    result.append(",");
                }
            }
            
            if (!isPaperDefault) {
                result.append(paperSize.getName());
                
                if (hasOptionalOptions) {
                    result.append(",");
                }
            }
        
            if (hasOptionalOptions) {
                for (int cntr = 0; cntr < data.getOptions().length; cntr++) {
                    result.append(data.getOptions()[cntr].getName());
                    
                    if (cntr + 1 <= data.getOptions().length)
                        result.append(",");
                }
            }
            result.append("]");
        }
        
        result.append("{");
        result.append(data.getDocclass().getName());
        result.append("}\n");
        
        if (!data.getInputEnc().hasAttribute("default-encoding")) {
            result.append("\\\\usepackage[");
            result.append(data.getInputEnc().getName());
            result.append("]{");
            result.append("inputenc"); //XXX:better???
            result.append("}\n\\\\usepackage[T1]{fontenc}\n");
        }
        
        return result.toString();
    }
    
    private String prepareEmptyPreamble() {
        return "\\\\documentclass{article}\n";
    }
    
    public Set<DataObject> instantiate(TemplateWizard wiz) throws IOException {
        if (!initialized)
            throw new IllegalStateException("");
        
        DataFolder target = wiz.getTargetFolder();
        DataObject template = wiz.getTemplate();
        String     targetName = wiz.getTargetName();
        
        DataObject instatied = template.createFromTemplate(target, targetName);
        
        EditorCookie ec = instatied.getLookup().lookup(EditorCookie.class);
        
        if (ec != null) {
            try {
                Document doc = ec.openDocument();
                String   content = doc.getText(0, doc.getLength());
                String   preamble;
                
                LaTeXWizardData data = (LaTeXWizardData) wiz.getProperty("latex_settings");
                
                if (data != null) {
                    preamble = preparePreamble(data);
                } else {
                    preamble = prepareEmptyPreamble();
                }
                
                content = content.replaceFirst("__PREAMBLE__", preamble);
                
                doc.remove(0, doc.getLength());
                doc.insertString(0, content, null);
                
                ec.saveDocument();
                ec.open();
            } catch (BadLocationException e) {
                throw (IOException) new IOException().initCause(e);
            }
        }
        
        return Collections.singleton(instatied);
    }
    
    public String name() {
        return "TESTETSTETSTETSTE";
    }
    
    public void nextPanel() {
        if (!initialized)
            throw new IllegalStateException("");
        
        current++;
    }
    
    public void previousPanel() {
        if (!initialized)
            throw new IllegalStateException("");
        
        current--;
    }
    
    public void uninitialize(TemplateWizard wiz) {
        initialized = false;
        panels = null;
        panelNames = null;
        current = 0;
    }
    
    private static NewDocumentWizardIterator instance = null;
    
    public static synchronized NewDocumentWizardIterator create() {
        if (instance == null)
            instance = new NewDocumentWizardIterator();
        
        return instance;
    }
    
    public void addChangeListener(ChangeListener l) {
    }
    
    public void removeChangeListener(ChangeListener l) {
    }
    
    private static class ContentPanel implements WizardDescriptor.Panel<WizardDescriptor> {
        
        private PreambleForm panel;
        
        public ContentPanel() {
            panel = null;
        }
        
        public void addChangeListener(ChangeListener l) {
        }
        
        private synchronized JPanel getPanel() {
            if (panel == null) {
                panel = new PreambleForm();
                panel.setName(NbBundle.getBundle("org/netbeans/modules/latex/wizards/Bundle").getString("LaTeX_document_preamble"));
            }
            
            return panel;
        }
        
        public Component getComponent() {
            return getPanel();
        }
        
        public HelpCtx getHelp() {
            return null;
        }
        
        public boolean isValid() {
            return true;
        }
        
        public void readSettings(WizardDescriptor descr) {
            LaTeXWizardData data = (LaTeXWizardData) descr.getProperty("latex_settings");

            if (data == null) {
                return;
            }
            panel.setDocumentClass(data.getDocclass());
            panel.setFontSize(data.getFontSize());
            panel.setPaperSize(data.getPaperSize());
            panel.setInputEnc(data.getInputEnc());
            panel.setAuthor(data.getAuthor());
            panel.setTitle(data.getTitle());
            panel.setOptions(data.getOptions());
        }
        
        public void removeChangeListener(ChangeListener l) {
        }
        
        public void storeSettings(WizardDescriptor descr) {
            LaTeXWizardData data = (LaTeXWizardData) descr.getProperty("latex_settings");

            if (data == null) {
                data = new LaTeXWizardData();
                descr.putProperty("latex_settings", data);
            }

            data.setDocclass(panel.getDocumentClass());
            data.setFontSize(panel.getFontSize());
            data.setPaperSize(panel.getPaperSize());
            data.setInputEnc(panel.getInputEnc());
            data.setAuthor(panel.getAuthor());
            data.setTitle(panel.getTitle());
            data.setOptions(panel.getOptions());
        }
        
    }
}
