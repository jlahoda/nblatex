/*
 *                          Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the Viewer module.
 * The Initial Developer of the Original Code is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002,2003.
 * All Rights Reserved.
 *
 * Contributor(s): Jan Lahoda.
 */
package org.netbeans.modules.latex.ui.actions;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallbackSystemAction;

/**
 *
 * @author Jan Lahoda
 */
public class CountWordsAction extends CallbackSystemAction {
    
    /** Creates a new instance of CiteAction */
    public CountWordsAction() {
    }
    
    public HelpCtx getHelpCtx() {
        return null;
    }
    
    public String getName() {
        return "Count Words in Document";
    }
    
}
