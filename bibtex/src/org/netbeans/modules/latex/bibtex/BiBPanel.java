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
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2007.
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
package org.netbeans.modules.latex.bibtex;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;


import java.lang.reflect.Field;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.JTextField;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.event.ListDataListener;

import javax.swing.table.DefaultTableModel;

import org.netbeans.modules.latex.model.bibtex.PublicationEntry;

/**
 *
 * @author Jan Lahoda
 */
public class BiBPanel extends javax.swing.JPanel implements ActionListener {
    
    public static final String AUTHOR = "author";
    public static final String TITLE  = "title";
    public static final String YEAR   = "year";
    public static final String BOOK_TITLE = "booktitle";
    public static final String JOURNAL = "journal";
    public static final String PAGES = "pages";
    
    private static final Map<String, String> NAMES2CODES;
    
    private String type;
    
    static {
        NAMES2CODES = new HashMap<String, String>();
        
        NAMES2CODES.put(AUTHOR, "Author");
        NAMES2CODES.put(TITLE, "Title");
        NAMES2CODES.put(YEAR, "Year");
        NAMES2CODES.put(BOOK_TITLE, "BookTitle");
        NAMES2CODES.put(JOURNAL, "Journal");
        NAMES2CODES.put(PAGES, "Pages");
    }
    
    /** Creates new form JPanel */
    public BiBPanel() {
        initComponents();
	
	jComboBox1.setModel(new DefaultComboBoxModel(FieldDatabase.getDefault().getKnownTypes().toArray()));
        jComboBox1.addActionListener(this);
//        jComboBox1.getModel().addListDataListener(
    }
    
    private JTextField getTextField(String name) {
        try {
            String codeBase = NAMES2CODES.get(name);
            
            if (codeBase == null)
                return null;
            
            String code = "t" + codeBase;
            
            if (code == null)
                return null;
            
            Field field = getClass().getDeclaredField(code);
            
            return (JTextField) field.get(this);
        } catch (NoSuchFieldException e) {
            //Should never happen:
            e.printStackTrace(System.err);
        } catch (IllegalArgumentException e) {
            //Should never happen:
            e.printStackTrace(System.err);
        } catch (IllegalAccessException e) {
            //Should never happen:
            e.printStackTrace(System.err);
        }
        
        return null;
    }

    private JLabel getLabel(String name) {
        try {
            String codeBase = NAMES2CODES.get(name);
            
            if (codeBase == null)
                return null;
            
            String code = "l" + codeBase;
            
            if (code == null)
                return null;
            
            Field field = getClass().getDeclaredField(code);
            
            return (JLabel) field.get(this);
        } catch (NoSuchFieldException e) {
            //Should never happen:
            e.printStackTrace(System.err);
        } catch (IllegalArgumentException e) {
            //Should never happen:
            e.printStackTrace(System.err);
        } catch (IllegalAccessException e) {
            //Should never happen:
            e.printStackTrace(System.err);
        }
        
        return null;
    }
    
    private void setEnabled(Collection toEnable) {
        Iterator keys = NAMES2CODES.keySet().iterator();
        
        while (keys.hasNext()) {
            String key = (String) keys.next();
            JTextField field = getTextField(key);
	    JLabel     label = getLabel(key);
            
            field.setEnabled(toEnable.contains(key));
            field.setText("");
	    label.setEnabled(toEnable.contains(key));
        }
    }
    
    private void adjustToType(String type) {
        Map map = getContent();

        this.type = type;
        
        Collection<String> c = new ArrayList<String>();
	
	c.addAll(FieldDatabase.getDefault().getRequiredFields(type));
	c.addAll(FieldDatabase.getDefault().getOptionalFields(type));

    	setEnabled(c);
        
        setContent(map);
    }
    
    private void setContent(Map content) {
        Collection<String> c = new ArrayList<String>();
	
	c.addAll(FieldDatabase.getDefault().getRequiredFields(type));
	c.addAll(FieldDatabase.getDefault().getOptionalFields(type));
        
        Iterator keys = c.iterator();
        
        while (keys.hasNext()) {
            String key = (String) keys.next();
            String value = (String) content.get(key);
            JTextField field = getTextField(key);
            
            if (field != null)
                field.setText(value != null ? value : "");
        }
        
        setVariableTable(content);
    }
    
    public void setContent(PublicationEntry entry) {
        type = entry.getType().toUpperCase();
        
        Collection<String> c = new ArrayList<String>();
	
	c.addAll(FieldDatabase.getDefault().getRequiredFields(type));
	c.addAll(FieldDatabase.getDefault().getOptionalFields(type));
	
	jComboBox1.setSelectedItem(type);
        
        tTag.setText(entry.getTag());
	
	setEnabled(c);
	
        setContent(entry.getContent());
    }
    
    private void setVariableTable(Map content) {
        Collection<String> c = new ArrayList<String>();
	
	c.addAll(FieldDatabase.getDefault().getRequiredFields(type));
	c.addAll(FieldDatabase.getDefault().getOptionalFields(type));

        Set toUse = new HashSet(content.keySet());
        
        toUse.removeAll(c);
        
        Iterator iter = toUse.iterator();
        int rowIndex = 0;
        
        DefaultTableModel dtm = (DefaultTableModel) jTable1.getModel();
        
        dtm.setRowCount(toUse.size());
        
        while (iter.hasNext()) {
            Object key = iter.next();
            
            jTable1.setValueAt(key, rowIndex, 0);
            jTable1.setValueAt(content.get(key), rowIndex, 1);
            rowIndex++;
        }
    }
    
    private void putVariableTable(Map content) {
        for (int cntr = 0; cntr < jTable1.getRowCount(); cntr++) {
            String key        = (String) jTable1.getValueAt(cntr, 0);
            String contentVal = (String) jTable1.getValueAt(cntr, 1);
            
            if (contentVal != null && contentVal.length() > 0)
                content.put(key, contentVal);
        }
    }
    
    private Map getContent() {
        Collection c = new ArrayList();
	
	c.addAll(FieldDatabase.getDefault().getRequiredFields(type));
	c.addAll(FieldDatabase.getDefault().getOptionalFields(type));
        
        System.err.println("getContent, c= " + c);
        Iterator keys = c.iterator();
        Map result = new HashMap();
        
        while (keys.hasNext()) {
            String key = (String) keys.next();
            System.err.println("key = " + key );
            JTextField field = getTextField(key);
            
            if (field == null || !field.isEnabled())
                continue;
            
            String contentVal = field.getText();
            
            if (contentVal != null && contentVal.length() > 0)
                result.put(key, contentVal);
            
            System.err.println("result = " + result );
        }
        
        putVariableTable(result);
        
        return result;
    }
    
    public void actionPerformed(ActionEvent evt) {
        String wanted = (String) jComboBox1.getSelectedItem();
        
        if (wanted.equals(type))
            return ;
        
        adjustToType(wanted);
    }
    
    public void fillIntoEntry(PublicationEntry entry) {
        entry.setType(type);
        entry.setTag(tTag.getText());
        entry.setContent(getContent());
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jComboBox1 = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        lTitle = new javax.swing.JLabel();
        tTitle = new javax.swing.JTextField();
        lAuthor = new javax.swing.JLabel();
        tAuthor = new javax.swing.JTextField();
        lJournal = new javax.swing.JLabel();
        tJournal = new javax.swing.JTextField();
        lYear = new javax.swing.JLabel();
        tYear = new javax.swing.JTextField();
        lPages = new javax.swing.JLabel();
        tPages = new javax.swing.JTextField();
        lBookTitle = new javax.swing.JLabel();
        tBookTitle = new javax.swing.JTextField();
        lTag = new javax.swing.JLabel();
        tTag = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        bAdd = new javax.swing.JButton();
        bRemove = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(jComboBox1, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        lTitle.setLabelFor(tTitle);
        lTitle.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_title"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lTitle, gridBagConstraints);

        tTitle.setText("jTextField1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(tTitle, gridBagConstraints);

        lAuthor.setLabelFor(tAuthor);
        lAuthor.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_author"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lAuthor, gridBagConstraints);

        tAuthor.setText("jTextField2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(tAuthor, gridBagConstraints);

        lJournal.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_journal"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lJournal, gridBagConstraints);

        tJournal.setText("jTextField3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(tJournal, gridBagConstraints);

        lYear.setLabelFor(tYear);
        lYear.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_year"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lYear, gridBagConstraints);

        tYear.setText("jTextField4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(tYear, gridBagConstraints);

        lPages.setLabelFor(tPages);
        lPages.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_pages"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lPages, gridBagConstraints);

        tPages.setText("jTextField5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(tPages, gridBagConstraints);

        lBookTitle.setLabelFor(tBookTitle);
        lBookTitle.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_booktitle"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lBookTitle, gridBagConstraints);

        tBookTitle.setText("jTextField6");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(tBookTitle, gridBagConstraints);

        lTag.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_Tag"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(lTag, gridBagConstraints);

        tTag.setText("jTextField1");
        tTag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tTagActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(tTag, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Name", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jScrollPane1, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        bAdd.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_Add_Attribute"));
        bAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bAddActionPerformed(evt);
            }
        });

        jPanel3.add(bAdd, new java.awt.GridBagConstraints());

        bRemove.setText(java.util.ResourceBundle.getBundle("org/netbeans/modules/latex/bibtex/Bundle").getString("LBL_RemoveAttribute"));
        bRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bRemoveActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        jPanel3.add(bRemove, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jPanel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jPanel2, gridBagConstraints);

    }//GEN-END:initComponents

    private void bRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bRemoveActionPerformed
        // TODO add your handling code here:
        int[] rows = jTable1.getSelectedRows();
        
        if (rows.length == 1) {
            DefaultTableModel dtm = (DefaultTableModel) jTable1.getModel();
            
            dtm.removeRow(rows[0]);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_bRemoveActionPerformed

    private void bAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAddActionPerformed
        // TODO add your handling code here:
        DefaultTableModel dtm = (DefaultTableModel) jTable1.getModel();
        
        dtm.addRow(new String[] {"<newAttribute>", "<newValue>"});
    }//GEN-LAST:event_bAddActionPerformed

    private void tTagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tTagActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tTagActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bAdd;
    private javax.swing.JButton bRemove;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lAuthor;
    private javax.swing.JLabel lBookTitle;
    private javax.swing.JLabel lJournal;
    private javax.swing.JLabel lPages;
    private javax.swing.JLabel lTag;
    private javax.swing.JLabel lTitle;
    private javax.swing.JLabel lYear;
    private javax.swing.JTextField tAuthor;
    private javax.swing.JTextField tBookTitle;
    private javax.swing.JTextField tJournal;
    private javax.swing.JTextField tPages;
    private javax.swing.JTextField tTag;
    private javax.swing.JTextField tTitle;
    private javax.swing.JTextField tYear;
    // End of variables declaration//GEN-END:variables
    
}
