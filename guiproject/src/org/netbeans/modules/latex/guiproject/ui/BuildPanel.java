/*
 *                          Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the LaTeX module.
 * The Initial Developer of the Original Code is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2004.
 * All Rights Reserved.
 *
 * Contributor(s): Jan Lahoda.
 */
package org.netbeans.modules.latex.guiproject.ui;

import java.awt.Component;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.CharConversionException;
import java.util.Locale;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.netbeans.modules.latex.guiproject.LaTeXGUIProject;
import org.netbeans.modules.latex.guiproject.Utilities;
import org.netbeans.modules.latex.guiproject.build.BuildConfiguration;
import org.netbeans.modules.latex.guiproject.build.BuildConfigurationProvider;
import org.netbeans.modules.latex.guiproject.build.RunTypes;
import org.netbeans.modules.latex.guiproject.build.ShowConfiguration;
import org.openide.ErrorManager;
import org.openide.awt.HtmlRenderer;
import org.openide.xml.XMLUtil;

/**
 *
 * @author Jan Lahoda
 */
public class BuildPanel extends javax.swing.JPanel implements StorableSettingsPresenter {

    private LaTeXGUIProject p;

    /** Creates new form BuildPanel */
    public BuildPanel(LaTeXGUIProject p) {
        this.p = p;
        initComponents();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jComboBox4 = new javax.swing.JComboBox();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, "Build &Configuration:");

        jComboBox1.setModel(createBuildConfigurationModel());
        jComboBox1.setRenderer(new BuildConfigurationListCellRendererImpl());
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, "Run &BiBTeX:");

        jComboBox2.setModel(createRunTypesModel());
        jComboBox2.setRenderer(new RunTypeListCellRendererImpl());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, "&Show Command:");

        jComboBox3.setModel(createShowConfigurationModel());
        jComboBox3.setRenderer(new ShowConfigurationListCellRendererImpl());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, "Document &Locale:");

        jComboBox4.setEditable(true);
        jComboBox4.setModel(createLocaleModel());

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(jLabel2)
                            .add(jLabel3)
                            .add(jLabel4))
                        .add(6, 6, 6)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jComboBox1, 0, 256, Short.MAX_VALUE)
                            .add(jComboBox2, 0, 256, Short.MAX_VALUE)
                            .add(jComboBox3, 0, 256, Short.MAX_VALUE)
                            .add(jComboBox4, 0, 256, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(jComboBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(jComboBox3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jComboBox4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4))
                .add(48, 48, 48)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 138, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // need to redraw the show combo, as the set of supported show commands may have changed:
        jComboBox3.invalidate();
        jComboBox3.repaint();
    }//GEN-LAST:event_jComboBox1ActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JComboBox jComboBox1;
    public javax.swing.JComboBox jComboBox2;
    public javax.swing.JComboBox jComboBox3;
    public javax.swing.JComboBox jComboBox4;
    public javax.swing.JLabel jLabel1;
    public javax.swing.JLabel jLabel2;
    public javax.swing.JLabel jLabel3;
    public javax.swing.JLabel jLabel4;
    public javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    
    private PropertyDescriptor findPropertyDescriptor(BeanInfo info, String propertyName) {
        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        
        for (int cntr = 0; cntr < pds.length; cntr++) {
            PropertyDescriptor pd = pds[cntr];
            if (propertyName.equals(pd.getName()))
                return pd;
        }
        
        return null;
    }
    
    public void load(ProjectSettings settings) {
        String buildConfigurationName = settings.getBuildConfigurationName();
        BuildConfiguration configuration = Utilities.getBuildConfigurationProvider(p).getBuildConfiguration(buildConfigurationName);

        jComboBox1.setSelectedItem(configuration);

        String showConfigurationName = settings.getShowConfigurationName();
        ShowConfiguration showConfiguration = Utilities.getBuildConfigurationProvider(p).getShowConfiguration(showConfigurationName);

        jComboBox3.setSelectedItem(showConfiguration);

        RunTypes bibTeXRunType = settings.getBiBTeXRunType();

        jComboBox2.setSelectedItem(bibTeXRunType);
        
        jComboBox4.setSelectedItem(settings.getLocale());
    }
    
    public void store(ProjectSettings settings) {
        settings.setBuildConfigurationName(((BuildConfiguration) jComboBox1.getSelectedItem()).getName());
        settings.setShowConfigurationName(((ShowConfiguration) jComboBox3.getSelectedItem()).getName());
        settings.setBiBTeXRunType((RunTypes) jComboBox2.getSelectedItem());
        settings.setLocale((Locale) jComboBox4.getSelectedItem());
    }
    
    private ComboBoxModel createBuildConfigurationModel() {
        DefaultComboBoxModel dlm = new DefaultComboBoxModel();

        for (BuildConfiguration conf : Utilities.getBuildConfigurationProvider(p).getBuildConfigurations()) {
            dlm.addElement(conf);
        }

        return dlm;
    }

    private ComboBoxModel createRunTypesModel() {
        DefaultComboBoxModel dlm = new DefaultComboBoxModel();

        for (RunTypes type : RunTypes.values()) {
            dlm.addElement(type);
        }

        return dlm;
    }

    private ComboBoxModel createShowConfigurationModel() {
        DefaultComboBoxModel dlm = new DefaultComboBoxModel();

        for (ShowConfiguration conf : Utilities.getBuildConfigurationProvider(p).getShowConfigurations()) {
            dlm.addElement(conf);
        }

        return dlm;
    }
    
    private ComboBoxModel createLocaleModel() {
        DefaultComboBoxModel dlm = new DefaultComboBoxModel();
        
        for (Locale locale : Locale.getAvailableLocales()) {
            dlm.addElement(locale);
        }
        
        return dlm;
    }

    private class BuildConfigurationListCellRendererImpl implements ListCellRenderer {

        private ListCellRenderer delegate = HtmlRenderer.createRenderer();

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String displayName = null;
            BuildConfiguration configuration = (BuildConfiguration) value;

            if (configuration.isSupported(p))
                displayName = configuration.getDisplayName();
            else {
                try {
                    displayName = "<html><font color='FF0000'>" + XMLUtil.toElementContent(configuration.getDisplayName());
                } catch (CharConversionException e) {
                    ErrorManager.getDefault().notify(e);
                }
            }

            return delegate.getListCellRendererComponent(list, displayName, index, isSelected, cellHasFocus);
        }

    }

    private class ShowConfigurationListCellRendererImpl implements ListCellRenderer {

        private ListCellRenderer delegate = HtmlRenderer.createRenderer();

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String displayName = null;
            ShowConfiguration configuration = (ShowConfiguration) value;

            if (configuration.isSupported(p, (BuildConfiguration) jComboBox1.getSelectedItem()))
                displayName = configuration.getDisplayName();
            else {
                try {
                    displayName = "<html><font color='FF0000'>" + XMLUtil.toElementContent(configuration.getDisplayName());
                } catch (CharConversionException e) {
                    ErrorManager.getDefault().notify(e);
                }
            }

            return delegate.getListCellRendererComponent(list, displayName, index, isSelected, cellHasFocus);
        }

    }

    private class RunTypeListCellRendererImpl extends DefaultListCellRenderer {

        private ListCellRenderer delegate = HtmlRenderer.createRenderer();

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            RunTypes type = (RunTypes) value;

            return delegate.getListCellRendererComponent(list, type.getDisplayName(), index, isSelected, cellHasFocus);
        }

    }

}
