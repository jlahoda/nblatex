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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import org.openide.util.WeakListeners;

/**
 * @author Jan Lahoda
 */
public abstract class LaTeXSourceFactory {
    
    private List/*<MainFileListener>*/ listeners;
    
    public LaTeXSourceFactory() {
        listeners = new ArrayList();
    }
    
    public synchronized void addMainFileListener(MainFileListener l) {
        listeners.add(l);
    }

    public synchronized void removeMainFileListener(MainFileListener l) {
        listeners.remove(l);
    }
    
    protected void fireMainFileEvent(Object mainFile, boolean isMainFile, boolean isKnownFile) {
        List clone;
        
        synchronized (this) {
            clone = new ArrayList(listeners);
        }
        
        MainFileEvent evt = null;
        
        for (Iterator i = clone.iterator(); i.hasNext(); ) {
            MainFileListener l = (MainFileListener) i.next();
            
            if (evt == null)
                evt = new MainFileEvent(this, mainFile, isMainFile, isKnownFile);
            
            l.mainFileChanged(evt);
        }
    }
    
    public abstract boolean supports(Object file);
    
    public abstract LaTeXSource get(Object file);
    
    public abstract boolean isKnownFile(Object file);
    
    public abstract boolean isMainFile(Object file);
    
    /**Return collection of known files. This method should return collection of
     * files for which holds <code>get(file) != null</code> for this factory.
     * The implementors should use best-effort approach. If for some or all of the
     * files cannot be determined whether holds <code>get(file) != null</code>,
     * it is legal not to list them in the resulting collection.
     * 
     * @return list of all known files (<code>get(file) != null</code>). Should
     *              never return <code>null</code>.
     */
    public abstract Collection getAllKnownFiles();

    public static interface MainFileListener extends EventListener {
        
        public void mainFileChanged(MainFileEvent evt);
    }
    
    public static class MainFileEvent extends EventObject {
        
        private Object             mainFile;
        private boolean            isMainFile;
        private boolean            isKnownFile;
        
        public MainFileEvent(LaTeXSourceFactory factory, Object mainFile, boolean isMainFile, boolean isKnownFile) {
            super(factory);
            this.mainFile = mainFile;
            this.isMainFile = isMainFile;
            this.isKnownFile = isKnownFile;
        }
        
        public LaTeXSourceFactory getFactory() {
            return (LaTeXSourceFactory) getSource();
        }
        
        public Object getMainFile() {
            return mainFile;
        }
        
        public boolean isMainFile() {
            return isMainFile;
        }
        
        //TODO: currently not fired...
        public boolean isKnownFile() {
            return isKnownFile;
        }
        
    }
    
    public static MainFileListener weakMainFileListener(MainFileListener l, LaTeXSourceFactory source) {
        return (MainFileListener) WeakListeners.create(MainFileListener.class, l, source);
    }
}

