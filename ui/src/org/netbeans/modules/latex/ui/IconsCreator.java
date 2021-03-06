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
package org.netbeans.modules.latex.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;


/**module private class
 *
 * @author Jan Lahoda
 */
public final class IconsCreator {
    
    private static boolean doDelete = false;
    
    private String latex;
    private String dvips;
    private String gs;
    
    private boolean configurationUsable;
    
    /** Creates a new instance of IconsCreator */
    private IconsCreator(/*Map settings*/) {
        reloadSettings();
    }
    
    private static IconsCreator instance = null;
    
    public static synchronized IconsCreator getDefault() {
        if (instance == null)
            instance = new IconsCreator();
        
        return instance;
    }
    
    public void reloadSettings() {
        Map settings = ModuleSettings.getDefault().readSettings();
        
        if (settings == null) {
            configurationUsable = false;
            ((IconsStorageImpl) IconsStorageImpl.getDefault()).configurationChanged();
            return ;
        }
        
        latex = (String) settings.get("latex");
        dvips = (String) settings.get("dvips");
        gs    = (String) settings.get("gs");
        
        Boolean latex_quality = (Boolean) settings.get("latex-quality");
        Boolean dvips_quality = (Boolean) settings.get("dvips-quality");
        Boolean gs_quality    = (Boolean) settings.get("gs-quality");
        
        configurationUsable = !(    latex == null || latex_quality == null || !latex_quality.booleanValue()
                                 || dvips == null || dvips_quality == null || !dvips_quality.booleanValue()
                                 || gs    == null || gs_quality    == null || !gs_quality.booleanValue());
        
        ((IconsStorageImpl) IconsStorageImpl.getDefault()).configurationChanged();
    }
    
    public boolean isConfigurationUsable() {
        return configurationUsable;
    }
    
    public static void waitFor(final Process p) throws InterruptedException {
        new Thread() {
            public void run() {
                InputStream is = null;
                
                try {
                    is = p.getInputStream();
                    
                    int read;
                    
                    while ((read = is.read()) != (-1)) {
                        System.err.print((char) read);
                    }
                } catch (IOException e) {
                    Logger.getLogger("global").log(Level.INFO,null, e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            Logger.getLogger("global").log(Level.INFO,null, e);
                        }
                    }
                }
            }
        }.start();
        
        new Thread() {
            public void run() {
                InputStream is = null;
                
                try {
                    is = p.getErrorStream();
                    
                    int read;
                    
                    while ((read = is.read()) != (-1)) {
                        System.err.print((char) read);
                    }
                } catch (IOException e) {
                    Logger.getLogger("global").log(Level.INFO,null, e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            Logger.getLogger("global").log(Level.INFO,null, e);
                        }
                    }
                }
            }
        }.start();
        
        p.waitFor();
    }
    
    private void createPNG(File input, File output, String size) throws IOException, InterruptedException {
        Process gs = null;
        
        if (size != null) {
            gs = Runtime.getRuntime().exec(new String[]{
                this.gs,
                "-sDEVICE=png16m",
                "-dBATCH",
                "-dNOPAUSE",
                "-dGraphicsAlphaBits=4",
                "-dTextAlphaBits=4",
                "-dEPSFitPage",
                "-g" + size,
                "-sOutputFile=" + output.getAbsolutePath(),
                input.getAbsolutePath()
            });
        } else {
            gs = Runtime.getRuntime().exec(new String[]{
                this.gs,
                "-sDEVICE=pngalpha",
                "-dBATCH",
                "-r200",
                "-dNOPAUSE",
                "-dEPSCrop",
                "-sOutputFile=" + output.getAbsolutePath(),
                input.getAbsolutePath()
            });
        }
        
        waitFor(gs);
    }
    
    private File createPS(String command) throws IOException, InterruptedException {
        return createPSText("\\[ " + command + " \\]");
    }
    
    private File createPSText(String text) throws IOException, InterruptedException {
        File temp = null;
        File dvi  = null;
        
        try {
            temp = new File(getTmpDirectory(), "temp.tex");
            dvi  = new File(getTmpDirectory(), "temp.dvi");
            
            PrintStream tempOut = new PrintStream(new FileOutputStream(temp));
            
            tempOut.println("\\documentclass{article}");
            tempOut.println("\\usepackage{amsfonts}");
            tempOut.println("\\usepackage{amssymb}");
            tempOut.println("\\usepackage{latexsym}");
            tempOut.println("\\pagestyle{empty}");
            tempOut.println("\\begin{document}");
            tempOut.println(text);
            tempOut.println("\\end{document}");
            
            tempOut.close();
            
            Process latex = Runtime.getRuntime().exec(new String[] {
                this.latex,
                "-interaction=batchmode",
                temp.getAbsolutePath()//,command
            }, new String[0], temp.getParentFile());
            
            waitFor(latex);
            
            File ps = new File(getTmpDirectory(), "temp.ps");
            
            Process dviP = Runtime.getRuntime().exec(new String[] {
                this.dvips,
                "-E",
                "-o",
                ps.getAbsolutePath(),
                dvi.getAbsolutePath(),
            });
            
            waitFor(dviP);
            
            return ps;
        } finally {
            if (temp != null && doDelete)
                temp.delete();
            
            if (dvi != null && doDelete)
                dvi.delete();
        }
    }
    
    private void createIcon(String command, String size, File outputDir) throws IOException, InterruptedException {
        File ps = createPS(command);
        File png = new File(outputDir, constructFileName(command, size));
        
        createPNG(ps, png, size);
    }
    
    private static String encode(String s) {
        StringBuffer result = new StringBuffer();
        
        for (int cntr = 0; cntr < s.length(); cntr++) {
            String hex = Integer.toHexString((int) s.charAt(cntr));
            
            result.append("0000".substring(0, 4 - hex.length()));
            result.append(hex);
        }
        
        return result.toString();
    }
    
    public static String constructFileName(String expression, String size) {
        if (size != null)
            return encode(expression) + "-" + size + ".png";
        else {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                StringBuffer sb = new StringBuffer();
                
                for (byte b : md.digest(expression.getBytes("UTF-8"))) {
                    String hex = Integer.toHexString(b);

                    sb.append(String.format("%02X", b));
                }
                
                return sb.toString() + ".png";
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException(ex);
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
    
    private File getIconDirectory() throws IOException {
        File iconDir = new File(new File(new File(new File(System.getProperty("netbeans.user"), "var"), "cache"), "latex"), "icons");
        
        iconDir.mkdirs();
        
        return iconDir;
    }
    
    private File getTmpDirectory() throws IOException {
        File tmpDir = new File(new File(new File(new File(System.getProperty("netbeans.user"), "var"), "cache"), "latex"), "tmp");
        
        tmpDir.mkdirs();
        
        return tmpDir;
    }
    
    private void createIconsForCommand(String command, String[] sizes) {
        int colon = command.indexOf(':');
        
        if (colon != (-1)) {
            createIconsForCommand(command.substring(0, colon), command.substring(colon + 1), sizes);
        } else {
            createIconsForCommand(command, "", sizes);
        }
    }
    
    private void createIconsForCommand(String command, String attributes, String[] sizes) {
        try {
            File iconDir = getIconDirectory();
            
            for (int cntr = 0; cntr < sizes.length; cntr++) {
                createIcon(command, sizes[cntr], iconDir);
            }
            
            if (attributes.indexOf("icon_") != (-1)) {
                createIcon(command, "16x16", iconDir);
            }
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        } catch (InterruptedException e) {
            Exceptions.printStackTrace(e);
        }
    }
    
    private void createIconsForText(String text) {
        try {
            File iconDir = getIconDirectory();
            
            File ps = createPSText(text);
            File png = new File(iconDir, constructFileName(text, null));

            createPNG(ps, png, null);
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        } catch (InterruptedException e) {
            Exceptions.printStackTrace(e);
        }
    }
    
    public boolean createIconForExpression(String expression, String size) {
        if (isConfigurationUsable()) {
            createIconsForCommand(expression, "", new String[] {size});
            
            return true;
        } else {
            return false;
        }
    }

    public boolean createIconForText(String text) {
        if (isConfigurationUsable()) {
            createIconsForText(text);
            
            return true;
        } else {
            return false;
        }
    }
    
}
