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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.latex.ui;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jan Lahoda
 */
public class Autodetector {

    private static final String AUTODETECTOR_VERSION = "autodetector-version";
    private static final int CURRENT_AUTODETECTOR_VERSION = 3;
    
    private static final boolean debug = false;
    
    private Autodetector() {
    }
    
    public static final void registerAutodetection() {
        new Autodetector().registerAutodetectionImpl();
    }
    
    private void registerAutodetectionImpl() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Frame f = WindowManager.getDefault().getMainWindow();

                if (!f.isShowing()) {
                    f.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowOpened(WindowEvent e) {
                            RequestProcessor.getDefault().post(new Runnable() {
                                public void run() {
                                    autodetect();
                                }
                            });
                        }
                    });
                } else {
                    autodetect();
                }
            }
        });
    }
    
    private void autodetect() {
        Map m = ModuleSettings.getDefault().readSettings();
        
        if (m != null) {
            Object version = m.get(AUTODETECTOR_VERSION);

            if (version != null &&((Integer) version) >= CURRENT_AUTODETECTOR_VERSION)
            return ;
        }
        
        final ProgressHandle handle = ProgressHandleFactory.createHandle("Autodetecting LaTeX Commands");
        
        new Thread() {
            @Override
            public void run() {
                Map<String, Object> results = new HashMap<String, Object>();
                
                int count = 0;
                
                for (Iterator i = defaultLocations.values().iterator(); i.hasNext(); ) {
                    count += ((String[] ) i.next()).length;
                }
                
                handle.start(count);
                
                int current = 0;
                
                for (Iterator i = defaultLocations.keySet().iterator(); i.hasNext(); ) {
                    String key = (String) i.next();
                    String[] locations = defaultLocations.get(key);
                    List<String> targetLocations = new ArrayList<String>();
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
                    
                    String[] targetLocationsArray = targetLocations.toArray(new String[0]);
                    
                    if (targetLocationsArray.length > 0) {
                        results.put(key, targetLocationsArray[0]);
                        results.put(key + "-quality", Boolean.valueOf(foundPerfect));
                    }
                }
                
                handle.finish();
                
                Map<String, Object> m = ModuleSettings.getDefault().readSettings();
                
                if (m != null) {
                    m.putAll(results);
                    m.put(AUTODETECTOR_VERSION, CURRENT_AUTODETECTOR_VERSION);
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
        //make sure no windows will pop-up (unless necessary):
        if (Utilities.isWindows() && null == shouldContain && new File(file).isFile())
            return OK;
        
        if (null == shouldContain)
            shouldContain = "";
        
        try {
            final StringBuffer contentOut = new StringBuffer();
            final StringBuffer contentErr = new StringBuffer();
            final Process      p          = Runtime.getRuntime().exec(new String[] {file, argument});
            
            Thread out = new Thread() {
                @Override
                public void run() {
                    try {
                        InputStream  ins     = p.getInputStream();
                        int read;
                        
                        while ((read = ins.read()) != (-1)) {
                            contentOut.append((char) read);
                            if (debug)
                                System.err.print((char) read);
                        }
                    } catch (IOException e) {
                        Logger.getLogger("global").log(Level.INFO,null, e);
                    }
                }
            };
            
            out.start();
            
            Thread err = new Thread() {
                @Override
                public void run() {
                    try {
                        InputStream  ins     = p.getErrorStream();
                        int read;
                        
                        while ((read = ins.read()) != (-1)) {
                            contentErr.append((char) read);
                            if (debug)
                                System.err.print((char) read);
                        }
                    } catch (IOException e) {
                        Logger.getLogger("global").log(Level.INFO,null, e);
                    }
                }
            };
            
            err.start();
            
            p.getOutputStream().close();
            
            Thread waitFor = new Thread() {
                @Override
                public void run() {
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        Logger.getLogger("global").log(Level.INFO,null, e);
                    }
                }
            };
            
            waitFor.start();
            
            try {
                waitFor.join(20000); //For safety, we give 20 second until the program is completed.
            } catch (InterruptedException e) {
                Logger.getLogger("global").log(Level.INFO,null, e);
                p.destroy();
                waitFor.join();
            }
            
            out.join(20000);
            err.join(20000);
            
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
        String awaitedContent = content.get(type);
        int result = NOT_FOUND;
        
        if (debug) {
            System.err.println("arguments=" + arguments);
            System.err.println("awaitedContent=" + awaitedContent);
        }
        
        assert arguments != null;
        
        for (Iterator/*<String>*/ i = arguments.iterator(); i.hasNext(); ) {
            result = getBetter(result, verifyLocation(program, awaitedContent, (String) i.next()));
        }
        
        if (debug)
            System.err.println("result=" + result);
        
        return result;
    }
    
    private static final Map<String, String> content;
    private static final Map<String, List<String>> type2Arguments;
    private static final Map<String, String[]> defaultLocations;
    
    static {
        content = new HashMap<String, String>();
        
        content.put("latex", "");
        content.put("bibtex", "");
        content.put("dvips", "");
        content.put("ps2pdf", "");
//        content.put("gs", "pngalpha");
        content.put("gs", "png16m");
        content.put("xdvi", null);
        content.put("gv", "");
        
        type2Arguments = new HashMap<String, List<String>>();
        
        type2Arguments.put("latex", Arrays.asList(new String[] {"--version"}));
        type2Arguments.put("bibtex", Arrays.asList(new String[] {"--version"}));
        type2Arguments.put("dvips", Arrays.asList(new String[] {"--version"}));
        type2Arguments.put("ps2pdf", Arrays.asList(new String[] {"--version"}));
        type2Arguments.put("gs",    Arrays.asList(new String[] {"--version", "--help"}));
        type2Arguments.put("xdvi",    Arrays.asList(new String[] {"--version"}));
        type2Arguments.put("gv",    Arrays.asList(new String[] {"--version"}));

        defaultLocations = new HashMap<String, String[]>();
        
        defaultLocations.put("latex", new String[] {"latex", "/usr/share/texmf/bin/latex", "C:\\texmf\\miktex\\bin\\latex.exe"});
        defaultLocations.put("bibtex", new String[] {"bibtex", "/usr/share/texmf/bin/bibtex", "C:\\texmf\\miktex\\bin\\bibtex.exe"});
        defaultLocations.put("dvips", new String[] {"dvips", "/usr/share/texmf/bin/dvips", "C:\\texmf\\miktex\\bin\\dvips.exe"});
        defaultLocations.put("ps2pdf", new String[] {"ps2pdf", "/usr/bin/ps2pdf", "C:\\texmf\\miktex\\bin\\ps2pdf.exe"});
        defaultLocations.put("gs", new String[] {"gs-gpl", "gs", "/usr/bin/gs", "/usr/local/bin/gs", "C:\\Program Files\\gs\\gs8.53\\bin\\gswin32c.exe"});
        defaultLocations.put("xdvi", new String[] {"xdvi", "C:\\texmf\\miktex\\bin\\yap.exe"});
        defaultLocations.put("gv", new String[] {"gv", "kghostview", "ggv", "evince"});
    }
    
    private int getBetter(int status1, int status2) {
        if (status1 == OK || status2 == OK)
            return OK;
        
        if (status1 == NOT_CONTENT || status2 == NOT_CONTENT)
            return NOT_CONTENT;
        
        return NOT_FOUND;
    }
    
}
