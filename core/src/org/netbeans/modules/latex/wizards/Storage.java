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
package org.netbeans.modules.latex.wizards;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;

/**
 *
 * @author Jan Lahoda
 */
public class Storage {
    
    /** Creates a new instance of Storage */
    private Storage() {
    }
    
    private static Storage instance;
    
    public static synchronized Storage getDefault() {
        if (instance == null) {
            instance = new Storage();
        }
        
        return instance;
    }
    
    public Collection getAllDocumentClasses(boolean includeDefault) {
        Collection result = new ArrayList();
        FileObject latexCommandsFolder = Repository.getDefault().findResource("latex/commands");
        
        FileObject[] children = latexCommandsFolder.getChildren();
        
        for (int cntr = 0; cntr < children.length; cntr++) {
            FileObject current = children[cntr];
            Object     typeObj = current.getAttribute("type");
            
            if (!(typeObj instanceof String))
                continue;
            
            String type = (String) typeObj;
            
            if (!"docclass".equals(type))
                continue;
            
            if (!includeDefault) {
                Object defaultFlag = current.getAttribute("default");
                
                if (defaultFlag != null && defaultFlag instanceof Boolean && ((Boolean) defaultFlag).booleanValue())
                    continue;
            }
            
            result.add(current.getName());
        }
        
        return result;
    }
    
    public void addDocumentClass(String name) throws IOException {
        if (getAllDocumentClasses(true).contains(name))
            throw new IllegalArgumentException();
        
        FileObject latexCommandsFolder = Repository.getDefault().findResource("latex/commands");
        FileObject newFolder = latexCommandsFolder.createFolder(name);
        
        newFolder.setAttribute("type", "docclass");
    }
    
    public void addFontSize(String docclass, String fontSize) {
        throw new IllegalStateException("");
    }
    
    private List readFile(FileObject options) {
        if (options == null)
            return Collections.EMPTY_LIST;
        
        InputStream ins = null;
        BufferedReader reader  = null;
        List result = new ArrayList();
        
        try {
            ins = options.getInputStream();
            
            reader = new BufferedReader(new InputStreamReader(ins));
            String line;
            
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            return result;
        } catch (IOException e) {
            return result;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                    ins = null;
                } catch (IOException e) {
                }
            }
            
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    private List readFile(String name, String fileName) {
        return readFile(Repository.getDefault().findResource("latex/commands/" + name + "/" + fileName));
    }
    
    public Collection getOptions(String name) {
        return readFile(name, "options.txt");
    }
    
    public String getInputEncPackageName() {
        return "inputenc";
    }
    
    public Collection getSupportedFontSizes(String docclass) {
        if (!getAllDocumentClasses(true).contains(docclass))
            throw new IllegalArgumentException("");
        
        return readFile(docclass, "fontsizes.txt");
    }
    
    public boolean isDefaultFontSize(String docclass, String fontSize) {
        return readFile(docclass, "fontsizes.txt").get(0).equals(fontSize);
    }

    public Collection getSupportedPaperSizes(String docclass) {
        if (!getAllDocumentClasses(true).contains(docclass))
            throw new IllegalArgumentException("");
        
        return readFile(docclass, "papersizes.txt");
    }
    
    public boolean isDefaultPaperSize(String docclass, String paperSize) {
        return readFile(docclass, "papersizes.txt").get(0).equals(paperSize);
    }
    
    public boolean isDefaultEncoding(String encoding) {
        return readFile(getInputEncPackageName(), "options.txt").get(0).equals(encoding);
    }

}
