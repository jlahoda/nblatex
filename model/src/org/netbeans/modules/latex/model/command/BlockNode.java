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
package org.netbeans.modules.latex.model.command;

/**
 *
 * @author Jan Lahoda
 */
public interface BlockNode extends Node {
    
    public CommandNode getBeginCommand();
    
    public CommandNode getEndCommand();
    
    public TextNode    getContent();
    
    public String      getBlockName();

    public Environment getEnvironment();
}
