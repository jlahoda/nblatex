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

package org.netbeans.modules.latex.ui.options;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.latex.editor.LaTeXSettings;
import org.netbeans.modules.latex.ui.Autodetector;
import org.netbeans.modules.latex.ui.IconsStorageImpl;
import org.netbeans.modules.latex.ui.ModuleSettings;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jan Lahoda
 */
public class LaTeXOptionsPanel extends javax.swing.JPanel {
    
    private static final int SCHEDULE = 1000;
    
    private RequestProcessor PROCESSING = new RequestProcessor("LaTeXOptionsPanel", 1);
    private Map<String, RequestProcessor.Task> tasks = new HashMap<String, RequestProcessor.Task>();
    private Map<String, JTextField> program2Field = new HashMap<String, JTextField>();

    private static final List<String> programs = Arrays.asList(new String[] {
            "latex",
            "bibtex",
            "dvips",
            "ps2pdf",
            "gs",
            "xdvi",
            "gv",
        });
    
    /** Creates new form LaTeXOptionsPanel */
    public LaTeXOptionsPanel() {
        initComponents();

        jPanel3.add(createPanel(programs));
    }

    private static Map<String, String> program2Name;

    static {
        program2Name = new HashMap<String, String>();

        program2Name.put("latex", "latex:");
        program2Name.put("bibtex", "bibtex:");
        program2Name.put("dvips", "dvips:");
        program2Name.put("ps2pdf", "ps2pdf:");
        program2Name.put("gs", "gs:");
        program2Name.put("xdvi", "DVI viewer:");
        program2Name.put("gv", "PS/PDF viewer:");
    }

    private JPanel createPanel(List<String> programs) {
        JPanel result = new JPanel();

        result.setLayout(new GridBagLayout());

        int index = 0;

        for (String p : programs) {
            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2 * index;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 6, 6);

            result.add(new JLabel(program2Name.get(p)), c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2 * index;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 6, 6);

            final JTextField command = new JTextField();

            result.add(command, c);
            program2Field.put(p, command);

            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 2 * index;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 6, 0);

            JButton b = new JButton("Browse");

            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    browse(command);
                }
            });

            result.add(b, c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2 * index + 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 9, 0);

            JLabel working = new JLabel("Working...");

            result.add(working, c);

            tasks.put(p, PROCESSING.create(new CheckProgram(p, command, working)));

            invalidate(tasks.get(p), working);

            index++;
        }

        return result;
    }

    private void browse(JTextField field) {
        File content = new File(field.getText());
        JFileChooser chooser = new JFileChooser();

        if (content.exists())
            chooser.setCurrentDirectory(content);

        chooser.setMultiSelectionEnabled(false);

        if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void invalidate(RequestProcessor.Task task, final JLabel result) {
        task.schedule(SCHEDULE);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                result.setText("Working...");
            }
        });
    }
    
    public void update() {
        Map settings = ModuleSettings.getDefault().readSettings();
        
        for (String p : programs) {
            JTextField f = program2Field.get(p);
            
            if (settings != null) {
                f.setText((String) settings.get(p));
            } else {
                f.setText("");
            }
        }
        allowHardWrap.setSelected(LaTeXSettings.isHardWrapAllowed());
        allowCiteFormatHint.setSelected(LaTeXSettings.isCiteFormatHintEnabled());
    }
    
    public void commit() {
        Map settings = ModuleSettings.getDefault().readSettings();
        
        if (settings == null) {
            settings = new HashMap();
        }
        
        //TODO: outside the AWT:
        for (String p : programs) {
            JTextField f = program2Field.get(p);
            String command = f.getText();

            settings.put(p, command);
            settings.put(p + "-quality", Boolean.valueOf(Autodetector.checkProgram(p, command) == Autodetector.OK));
        }
        
        ModuleSettings.getDefault().writeSettings(settings);
        
        LaTeXSettings.setHardWrapAllowed(allowHardWrap.isSelected());
        LaTeXSettings.setCiteFormatHintEnabled(allowCiteFormatHint.isSelected());
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        allowHardWrap = new javax.swing.JCheckBox();
        allowCiteFormatHint = new javax.swing.JCheckBox();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Commands", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.TOP));
        jPanel1.setOpaque(false);

        jPanel3.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 857, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Icons"));

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, "Clear Icons Cache");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1)
                .addContainerGap(725, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Editor Options"));

        org.openide.awt.Mnemonics.setLocalizedText(allowHardWrap, "Allow Hard Wrap");

        org.openide.awt.Mnemonics.setLocalizedText(allowCiteFormatHint, "Allow Cite Format Hint");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(allowHardWrap)
                .addGap(18, 18, 18)
                .addComponent(allowCiteFormatHint)
                .addContainerGap(558, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(allowHardWrap)
                    .addComponent(allowCiteFormatHint))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        NotifyDescriptor confirm = new NotifyDescriptor.Confirmation("Do you really wish the clear icons cache? All icons will be recreated on demand.");

        if (DialogDisplayer.getDefault().notify(confirm) == NotifyDescriptor.YES_OPTION) {
            if (!IconsStorageImpl.getDefaultImpl().clearIconsCache()) {
                NotifyDescriptor error = new NotifyDescriptor.Message("Cannot clear icons cache.", NotifyDescriptor.ERROR_MESSAGE);

                DialogDisplayer.getDefault().notify(error);
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox allowCiteFormatHint;
    private javax.swing.JCheckBox allowHardWrap;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    // End of variables declaration//GEN-END:variables
    
    private final class CheckProgram implements Runnable, DocumentListener {
        
        private String type;
        private JTextField command;
        private JLabel result;
        
        public CheckProgram(String type, JTextField command, JLabel result) {
            this.type = type;
            this.command = command;
            this.result = result;

            command.getDocument().addDocumentListener(this);
        }
        
        public void run() {
//            System.err.println("check program started for type: " + type);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    result.setText("Working...");
                }
            });
            
            int checkResult = Autodetector.checkProgram(type, command.getText());
            String text = "Unknown status";
            
            switch (checkResult) {
                case Autodetector.NOT_FOUND:
                    text = "Command not found.";
                    break;
                case Autodetector.NOT_CONTENT:
                    text = "Command found, but has an incompatible version.";
                    break;
                case Autodetector.OK:
                    text = "Command found.";
                    break;
            }
            
            final String textFin = text;
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    result.setText(textFin);
                }
            });
//            System.err.println("check program finished for type: " + type);
        }

        public void changedUpdate(DocumentEvent e) {
        }

        public void insertUpdate(DocumentEvent e) {
            invalidate(tasks.get(type), result);
        }

        public void removeUpdate(DocumentEvent e) {
            invalidate(tasks.get(type), result);
        }

    }

}
