/*
 *                          Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the LaTeX module.
 * The Initial Developer of the Original Code is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002,2003.
 * All Rights Reserved.
 *
 * Contributor(s): Jan Lahoda.
 */
package org.netbeans.modules.latex.gui.nb;

import java.util.Collection;
import org.netbeans.modules.latex.model.command.CommandNode;

import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.structural.DelegatedParser;
import org.netbeans.modules.latex.model.structural.StructuralElement;


/**
 *
 * @author Jan Lahoda
 */
public final class VauParser extends DelegatedParser {
    
    /** Creates a new instance of VauParser */
    public VauParser() {
    }

    public StructuralElement getElement(Node node, Collection errors) {
        if (node instanceof CommandNode) {
            return new VauStructuralElement((CommandNode) node, errors);
        }
        
        return null;
    }
    
    public String[] getSupportedAttributes() {
        return new String[] {"#vcdraw-command"};
    }
    
}
