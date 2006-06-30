/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is the LaTeX module.
 * The Initial Developer of the Original Software is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2006.
 * All Rights Reserved.
 *
 * Contributor(s): Jan Lahoda.
 */
package org.netbeans.modules.latex.editor.completion.latex.help;

import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Jan Lahoda
 */
public class InstallHelpAction extends CallableSystemAction {

    /** Creates a new instance of ViewFilesTabAction */
    public InstallHelpAction() {
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx(InstallHelpAction.class);
    }

    public String getName() {
        return NbBundle.getMessage(InstallHelpAction.class, "LBL_InstallHelpAction");
    }

    protected String iconResource() {
        return "org/netbeans/modules/latex/editor/completion/resources/InstallHelpActionIcon.gif";
    }

    public void performAction() {
        InstallHelp.installHelp();
    }
    
    protected boolean asynchronous() {
        return false;
    }

}
