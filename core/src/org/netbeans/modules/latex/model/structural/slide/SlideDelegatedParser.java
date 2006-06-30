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
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2005.
 * All Rights Reserved.
 *
 * Contributor(s): Jan Lahoda.
 */
package org.netbeans.modules.latex.model.structural.slide;

import java.util.Collection;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.Environment;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.netbeans.modules.latex.model.structural.DelegatedParser;
import org.netbeans.modules.latex.model.structural.StructuralElement;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author Jan Lahoda
 */
public class SlideDelegatedParser extends DelegatedParser {
    
    /** Creates a new instance of SectionDelegatedParser */
    public SlideDelegatedParser() {
    }
    
    public StructuralElement getElement(Node node, Collection/*<ParseError>*/ errors) {
        //Only for case that some malicious module marked some Command with our attributes ;-(.
        if (node instanceof BlockNode) {
            BlockNode bnode = (BlockNode) node;
            Environment env = bnode.getEnvironment();
            
            if (env.hasAttribute("#slide-environment"))
                return new SlideStructuralElement(bnode);
            else
                return null;
        } else
            return null;
    }
    
    public String[] getSupportedAttributes() {
        return new String[] {
            "#slide-environment",
        };
    }
    
    public StructuralElement updateElement(Node node, Collection/*<ParseError>*/ errors, StructuralElement element) {
        if (!(element instanceof SlideStructuralElement))
            throw new IllegalStateException("");
        
        ((SlideStructuralElement) element).fireNameChanged();
        return element;
    }
    
    public Object getKey(Node node) {
        if (node instanceof BlockNode) {
            BlockNode bnode = (BlockNode) node;
            Environment env = bnode.getEnvironment();
            
            if (env.hasAttribute("#slide-environment"))
                return new SlideKey(bnode.getClass(), bnode.getStartingPosition(), bnode.getEndingPosition());
            else
                return null;
        } else
            return null;
    }

    private static class SlideKey {
        private Class nodeClass;
        private SourcePosition start;
        private SourcePosition end;
//        private String name;
        
        public SlideKey(Class nodeClass, SourcePosition start, SourcePosition end/*, String name*/) {
            this.nodeClass = nodeClass;
            this.start = start;
            this.end = end;
//            this.name = name;
        }

        public boolean equals(Object o) {
            if (!getClass().equals(o.getClass()))
                return false;
            
            SlideKey key = (SlideKey) o;
            
            if (!nodeClass.equals(key.nodeClass))
                return false;
            
            if (!start.equals(key.start))
                return false;
            
            if (!end.equals(key.end))
                return false;

            return true;
        }
        
        public int hashCode() {
            return 1; //just for testing!!!!
        }
    }

}
