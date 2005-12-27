/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.latex.ui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.ErrorManager;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jan Lahoda
 */
public class Autodetector {
    
    private static final boolean debug = false;
    
    /** Creates a new instance of Autodetector */
    private Autodetector() {
    }
    
    public static final void registerAutodetection() {
        new Autodetector().registerAutodetectionImpl();
    }
    
    private void registerAutodetectionImpl() {
        WindowManager.getDefault().getMainWindow().addWindowListener(new WindowListener() {
            public void windowActivated(WindowEvent e) {
            }
            public void windowClosed(WindowEvent e) {
            }
            public void windowClosing(WindowEvent e) {
            }
            public void windowDeactivated(WindowEvent e) {
            }
            public void windowDeiconified(WindowEvent e) {
            }
            public void windowIconified(WindowEvent e) {
            }
            public void windowOpened(WindowEvent e) {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        autodetect();
                    }
                });
            }
        });
    }
    
    private void autodetect() {
        Map m = ModuleSettings.getDefault().readSettings();
        
        if (m != null) {
            return ;
        }
        
        final ProgressHandle handle = ProgressHandleFactory.createHandle("Autodetecting LaTeX Commands");
        final Map defaultLocations = new HashMap();
        
        defaultLocations.put("latex", new String[] {"latex", "/usr/share/texmf/bin/latex"});
        defaultLocations.put("dvips", new String[] {"dvips", "/usr/share/texmf/bin/dvips"});
        defaultLocations.put("gs", new String[] {"gs", "/usr/bin/gs", "/usr/local/bin/gs"});
        
        new Thread() {
            public void run() {
                Map results = new HashMap();
                
                int count = 0;
                
                for (Iterator i = defaultLocations.values().iterator(); i.hasNext(); ) {
                    count += ((String[] ) i.next()).length;
                }
                
                handle.start(count);
                
                int current = 0;
                
                for (Iterator i = defaultLocations.keySet().iterator(); i.hasNext(); ) {
                    String key = (String) i.next();
                    String[] locations = (String[] ) defaultLocations.get(key);
                    List targetLocations = new ArrayList();
                    boolean foundPerfect = false;
                    
                    for (int cntr = 0; cntr < locations.length; cntr++) {
                        handle.progress(current++);
                        String location = locations[cntr];
                        
                        switch (testProgram(key, location)) {
                            case NOT_FOUND:
                                break;
                            case NOT_CONTENT:
                                targetLocations.add(location);
                                break;
                            case OK:
                                if (!foundPerfect) {
                                    foundPerfect = true;
                                    targetLocations.add(0, location);
                                } else {
                                    targetLocations.add(location);
                                }
                                break;
                        }
                    }
                    
                    String[] targetLocationsArray = (String[] ) targetLocations.toArray(new String[0]);
                    
                    if (targetLocationsArray.length > 0) {
                        results.put(key, targetLocationsArray[0]);
                        results.put(key + "-quality", Boolean.valueOf(foundPerfect));
                    }
                }
                
                handle.finish();
                
                Map m = ModuleSettings.getDefault().readSettings();
                
                if (m != null) {
                    m.putAll(results);
                } else {
                    m = results;
                }
                
                ModuleSettings.getDefault().writeSettings(m);
                
                IconsCreator.getDefault().reloadSettings();
            }
        }.start();
        
    }
    
    public static final int NOT_FOUND = 0;
    public static final int NOT_CONTENT = 1;
    public static final int OK = 2;
    
    private int verifyLocation(String file, String shouldContain, String argument) {
        try {
            final StringBuffer contentOut = new StringBuffer();
            final StringBuffer contentErr = new StringBuffer();
            final Process      p          = Runtime.getRuntime().exec(new String[] {file, argument});
            
            Thread out = new Thread() {
                public void run() {
                    try {
                        InputStream  ins     = p.getInputStream();
                        int read;
                        
                        while ((read = ins.read()) != (-1)) {
                            contentOut.append((char) read);
                        }
                    } catch (IOException e) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    }
                }
            };
            
            out.start();
            
            Thread err = new Thread() {
                public void run() {
                    try {
                        InputStream  ins     = p.getErrorStream();
                        int read;
                        
                        while ((read = ins.read()) != (-1)) {
                            contentErr.append((char) read);
                        }
                    } catch (IOException e) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    }
                }
            };
            
            err.start();
            
            Thread waitFor = new Thread() {
                public void run() {
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    }
                }
            };
            
            waitFor.start();
            
            try {
                waitFor.join(20000); //For safety, we give 20 second until the program is completed.
            } catch (InterruptedException e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                p.destroy();
                waitFor.join();
            }
            
            out.join();
            err.join();
            
            String content = new StringBuffer().append(contentOut).append(contentErr).toString();
            
            if (debug) {
                System.err.println("contentOut = " + contentOut );
                System.err.println("contentErr = " + contentErr );
                System.err.println("content=" + content);
                System.err.println("shouldContain = " + shouldContain );
                System.err.println("content.toString().indexOf(shouldContain)=" + content.toString().indexOf(shouldContain));
            }
            
            if (content.indexOf(shouldContain) != (-1))
                return OK;
            else
                return NOT_CONTENT;
        } catch (Exception e) {
            return NOT_FOUND;
        }
    }
    
    public static int checkProgram(String type, String program) {
        return new Autodetector().testProgram(type, program);
    }
    
    private int testProgram(String type, String program) {
        if (debug)
            System.err.println("testProgram(" + type + ", " + program + ")");
        
        List/*<String>*/ arguments = (List) type2Arguments.get(type);
        String awaitedContent = (String) content.get(type);
        int result = NOT_FOUND;
        
        if (debug) {
            System.err.println("arguments=" + arguments);
            System.err.println("awaitedContent=" + awaitedContent);
        }
        
        assert arguments != null;
        assert awaitedContent != null;
        
        for (Iterator/*<String>*/ i = arguments.iterator(); i.hasNext(); ) {
            result = getBetter(result, verifyLocation(program, awaitedContent, (String) i.next()));
        }
        
        if (debug)
            System.err.println("result=" + result);
        
        return result;
    }
    
    private static final Map content;
    private static final Map/*<String, List<String>>*/ type2Arguments; 
    
    static {
        content = new HashMap();
        
        content.put("latex", "");
        content.put("dvips", "");
        content.put("gs", "pngalpha");
        
        type2Arguments = new HashMap();
        
        type2Arguments.put("latex", Arrays.asList(new String[] {"--version"}));
        type2Arguments.put("dvips", Arrays.asList(new String[] {"--version"}));
        type2Arguments.put("gs",    Arrays.asList(new String[] {"--version", "--help"}));
    }
    
    private int getBetter(int status1, int status2) {
        if (status1 == OK || status2 == OK)
            return OK;
        
        if (status1 == NOT_CONTENT || status2 == NOT_CONTENT)
            return NOT_CONTENT;
        
        return NOT_FOUND;
    }
    
}