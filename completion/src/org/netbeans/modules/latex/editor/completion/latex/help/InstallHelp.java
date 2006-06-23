/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.latex.editor.completion.latex.help;
import java.awt.Dialog;
import org.netbeans.modules.latex.editor.completion.latex.help.DownloadHelpPanelImpl;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

/**
 *
 * @author Jan Lahoda
 */
public final class InstallHelp {
    
    public static void installHelp() {
        DownloadHelpPanelImpl panel = new DownloadHelpPanelImpl();
        
        DialogDescriptor dd = new DialogDescriptor(panel, "Install LaTeX Help");
        
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        
        d.setVisible(true);
        
        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
            PreprocessHelp.createHelpJar(panel.getHelpDirectory());
        }
    }
    
}