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
package org.netbeans.modules.latex.model.structural;

import java.io.IOException;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.text.Line.ShowOpenType;
import org.openide.text.Line.ShowVisibilityType;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

/**SPI
 *
 * @author Jan Lahoda
 */
public class GoToSourceAction extends NodeAction {
    
    /** Creates a new instance of GoToSourceAction */
    public GoToSourceAction() {
    }
    
    public HelpCtx getHelpCtx() {
        return new HelpCtx(GoToSourceAction.class);
    }
    
    public String getName() {
        return "Go To Source";
    }
    
    public void performAction(Node[] activatedNodes) {
        try {
            PositionCookie pc = activatedNodes[0].getLookup().lookup(PositionCookie.class);
            SourcePosition position = pc.getPosition();
            FileObject fileObject = (FileObject) position.getFile(); //!!!!
            DataObject file = DataObject.find(fileObject);
            
            LineCookie lc = file.getLookup().lookup(LineCookie.class);
            
            if (lc == null)
                return ;
            
            int line = position.getLine();
            
            lc.getLineSet().getCurrent(line).show(ShowOpenType.OPEN, ShowVisibilityType.FOCUS);
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        }
    }
    
    public boolean asynchronous() {
        return false;
    }
    
    protected boolean enable(Node[] activatedNodes) {
        if (activatedNodes.length != 1)
            return false;
        
        Node activatedNode = activatedNodes[0];
        PositionCookie pc = activatedNode.getLookup().lookup(PositionCookie.class);
        
        if (pc == null)
            return false;
        
        return pc.getPosition() != null;
    }
    
}
