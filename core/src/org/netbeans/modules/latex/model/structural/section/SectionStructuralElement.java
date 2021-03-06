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
package org.netbeans.modules.latex.model.structural.section;

import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.netbeans.modules.latex.model.structural.StructuralElement;

/**
 *
 * @author Jan Lahoda
 */
public class SectionStructuralElement extends StructuralElement {

    public static final String NAME = "name";

    private String plainName;
    private SourcePosition start;
    private int         priority;
    private int         type;
    private int[]       nums;
    
    public SectionStructuralElement(CommandNode node, int priority, int type, int[] nums) {
        this.priority = priority;
        this.type = type;
        update(node, nums);
    }
    
    public int getPriority() {
        return priority;
    }
    
    public synchronized String getName() {
        return numsToString() + " " + plainName;
    }

    public int getType() {
        return type;
    }

    public String getPlainName() {
        return plainName;
    }

    public synchronized SourcePosition getStartingPosition() {
        return start;
    }
    
    synchronized final void update(CommandNode node, int[] nums) {
        this.nums = nums;

        if (node.getArgumentCount() > 0) {
            this.plainName = node.getArgument(0).getText().toString(); //!!!there is no assurance that there will be argument number 0!
        } else {
            this.plainName = "";
        }

        this.start = node.getStartingPosition();
        
        fireNameChanged();
    }
    
    private void fireNameChanged() {
        pcs.firePropertyChange(NAME, null, null);
    }

    private String numsToString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (int i : nums) {
            if (!first) {
                sb.append(".");
            }

            sb.append(i);
            first = false;
        }

        return sb.toString();
    }
}
