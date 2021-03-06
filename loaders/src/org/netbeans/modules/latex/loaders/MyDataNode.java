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
package org.netbeans.modules.latex.loaders;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import org.netbeans.modules.latex.model.command.LaTeXSourceFactory;

import org.openide.loaders.DataNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.NbBundle;


/** A node to represent this object.
 *
 * @author Jan Lahoda
 */
public class MyDataNode extends DataNode {
    
    public static final String TEXT_SET = "Text";
    
    private LaTeXSourceFactory.MainFileListener factoryListener;
    
    public MyDataNode(TexDataObject obj) {
        this(obj, Children.LEAF);
    }
    
    protected MyDataNode(TexDataObject obj, Children ch) {
        super(obj, ch);
        setIconBaseWithExtension("org/netbeans/modules/latex/loaders/MyDataIcon.gif");
    }
    
    protected TexDataObject getMyDataObject() {
        return (TexDataObject)getDataObject();
    }
    
//     Example of adding Executor / Debugger / Arguments to node:
    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        
        Sheet.Set textSet = sheet.get(TEXT_SET);
        
        if (textSet == null) {
            textSet = new Sheet.Set();
            textSet.setName(TEXT_SET);
            textSet.setDisplayName(NbBundle.getMessage(MyDataNode.class, "LBL_DataNode_Text"));
            textSet.setShortDescription(NbBundle.getMessage(MyDataNode.class, "HINT_DataNode_Text"));
        }
        
        sheet.put(textSet);
        
        addTextProperties(textSet);
        
        return sheet;
    }

    private void addTextProperties(Sheet.Set textSet) {
        textSet.put(createTextEncodingProperty());
        textSet.put(createLocaleProperty());
    }
    
    private PropertySupport createTextEncodingProperty() {
        return new PropertySupport.ReadWrite<String>(
                  TexDataObject.ENCODING_PROPERTY_NAME,
                  String.class,
                  "Encoding",
                  "Encoding of the document") {
             public String getValue() {
                 return ((TexDataObject) getDataObject()).getCharSet();
             }
             
             public void setValue(String value) throws InvocationTargetException {
                 try {
                     ((TexDataObject) getDataObject()).setCharSet(value);
                 } catch (IOException e) {
                     throw new InvocationTargetException(e);
                 }
             }
             
            @Override
             public boolean supportsDefaultValue() {
                 return true;
             }
             
            @Override
             public void restoreDefaultValue() throws InvocationTargetException {
                 setValue(null);
             }
             
            @Override
             public boolean canWrite() {
                 return getDataObject().getPrimaryFile().canWrite();
             }
        };
    }

    private PropertySupport createLocaleProperty() {
        return new PropertySupport.ReadWrite<String>(
        TexDataObject.LOCALE_PROPERTY_NAME,
        String.class,
        "Locale",
        "Locale of the document") {
            public String getValue() {
                return ((TexDataObject) getDataObject()).getLocale();
            }
            
            public void setValue(String value) throws InvocationTargetException {
                try {
                    ((TexDataObject) getDataObject()).setLocale(value);
                } catch (IOException e) {
                    throw new InvocationTargetException(e);
                }
            }
            
            @Override
            public boolean supportsDefaultValue() {
                return true;
            }
            
            @Override
            public void restoreDefaultValue() throws InvocationTargetException {
                setValue(Locale.getDefault().toString());
            }
            
            @Override
            public boolean canWrite() {
                return getDataObject().getPrimaryFile().canWrite();
            }
        };
    }

    // Don't use getDefaultAction(); just make that first in the data loader's getActions list
    
}
