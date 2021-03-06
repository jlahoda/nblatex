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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.latex.model.IconsStorage;
import org.netbeans.modules.latex.model.IconsStorage.ChangeableIcon;
import org.netbeans.modules.latex.model.Queue;

import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Jan Lahoda
 */
public final class IconsStorageImpl extends IconsStorage {
    
    public static IconsStorageImpl getDefaultImpl() {
        return (IconsStorageImpl) getDefault();
    }

    private Map<String, List<String>> cathegory2Names;
    
    private Icon waitIcon;
    private Icon noIcons;
    
    /** Creates a new instance of IconsStorageImpl */
    public IconsStorageImpl() {
//        Thread.dumpStack();
        expression2Icon = new HashMap<String, Reference<ChangeableIcon>>();
        listeners = new HashMap();
        iconsToCreate = new Queue<DelegatingIcon>();
        iconsCreator  = new RequestProcessor("LaTeX Icons Creator");
        
        iconsCreator.post(new IconCreatorTask());
        
        Image waitIconImage = ImageUtilities.loadImage("org/netbeans/modules/latex/ui/resources/hodiny.gif");
        
        waitIcon = new ImageIcon(waitIconImage);
        
        Image noIconsImage = ImageUtilities.loadImage("org/netbeans/modules/latex/ui/resources/no_icon.gif");
        
        noIcons = new ImageIcon(noIconsImage);
        
        //prepopulate all listed icons:
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                for (String i : getAllIconNames()) {
                    getIcon(i);
                }
            }
        });
    }

    public ChangeableIcon getIcon(String command) {
        return getIcon(command, 16, 16);
    }

    public boolean getIconsInstalled() {
        return true;
    }

    public List<String> getIconNamesForCathegory(String catName) {
        assureLoaded();
        
        return cathegory2Names.get(catName);
    }

    public List<String> getAllIconNames() {
        assureLoaded();
        
        List<String> result = new ArrayList<String>();
        
        for (List<String> l : cathegory2Names.values()) {
            result.addAll(l);
        }
        
        return result;
    }
    
    private File getIconDirectory() throws IOException {
        File iconDir = new File(new File(new File(new File(System.getProperty("netbeans.user"), "var"), "cache"), "latex"), "icons");
        
        return iconDir;
    }

    public Collection<String> getCathegories() {
        assureLoaded();
        return cathegory2Names.keySet();
    }
    
    private void addIconDescription(String iconDescription) {
        if ("".equals(iconDescription))
            return ;
        
        int colon = iconDescription.indexOf(':');
        String command = "";
        String attributes = "";
        
        if (colon == (-1)) {
            command = iconDescription;
        } else {
            command = iconDescription.substring(0, colon);
            attributes = iconDescription.substring(colon + 1);
        }
        
        String cathegory = DEFAULT_CATHEGORY;
        Pattern p = Pattern.compile(".*symbols_([^,]*),?.*");
        Matcher m = p.matcher(attributes);
        
        if (m.find()) {
            cathegory = m.group(1);
        } else {
            Logger.getLogger("global").log(Level.FINE, "latex.IconsStorageImpl: default cathegory for \"" + iconDescription + "\"");
        }
        
        List<String> names = cathegory2Names.get(cathegory);
        
        if (names == null) {
            names = new ArrayList<String>();
            
            cathegory2Names.put(cathegory, names);
        }
        
        names.add(command);
    }
    
    private synchronized void assureLoaded() {
        if (cathegory2Names != null)
            return ;
        
        cathegory2Names = new HashMap<String, List<String>>();
        
        String[] icons = readIconsFile();
        
        for (int cntr = 0; cntr < icons.length; cntr++) {
            addIconDescription(icons[cntr]);
        }
    }
    
    public/*module private*/ static String[] readIconsFile() {
        try {
            InputStream ins = IconsStorageImpl.class.getResourceAsStream("/org/netbeans/modules/latex/ui/symbols/symbols.txt");
            BufferedReader bins = new BufferedReader(new InputStreamReader(ins));
            List<String> icons = new ArrayList<String>();
            String line;
            
            while ((line = bins.readLine()) != null) {
                icons.add(line);
            }
            
            return icons.toArray(new String[0]);
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
            return new String[0];
        }
    }

    public String getCathegoryDisplayName(String catName) {
        try {
            return NbBundle.getBundle(IconsStorageImpl.class).getString("CATLBL_" + catName);
        } catch (MissingResourceException e) {
            Logger.getLogger("global").log(Level.INFO,null, e);
            return catName;
        }
    }
    
    private Map<String, Reference<ChangeableIcon>> expression2Icon;
    
    private final RequestProcessor iconsCreator;
    private final Queue<DelegatingIcon> iconsToCreate;
    
    private class IconCreatorTask implements Runnable {
        public void run() {
            while (true) {
                DelegatingIcon toCreate = null;
                
                synchronized (iconsToCreate) {
                    while (iconsToCreate.empty()) {
                        try {
                            iconsToCreate.wait();
                        } catch (InterruptedException e) {
                            Logger.getLogger("global").log(Level.INFO,null, e);
                        }
                    }
                    
                    toCreate = iconsToCreate.pop();
                }
                
                createOrLoadIcon(toCreate);
            }
        }
    }
    
    private String constructSizeString(DelegatingIcon icon) {
        if (icon.getDesiredSizeX() == (-1) || icon.getDesiredSizeY() == (-1) || !icon.isIcon)
            return null;
        
        return "" + icon.getDesiredSizeX() + "x" + icon.getDesiredSizeY();
    }

    private static class MakeTransparentImage extends RGBImageFilter {

        public int filterRGB(int x, int y, int rgb) {
            int red   = (rgb >>  0) & 0xFF;
            int green = (rgb >>  8) & 0xFF;
            int blue  = (rgb >> 16) & 0xFF;
            int alpha = (rgb >> 24) & 0xFF;

            int redDiff = 0xFF - red;
            int greenDiff = 0xFF - green;
            int blueDiff = 0xFF - blue;

            if (redDiff != greenDiff || greenDiff != blueDiff) {
                System.err.println("not a mono image!");
            }

            return redDiff << 24;
        }

    }

    private void createOrLoadIcon(DelegatingIcon icon) {
        Icon i = null;
        File iconFile = null;
        
        try {
            iconFile = new File(getIconDirectory(), IconsCreator.constructFileName(icon.getText(), constructSizeString(icon)));
            
            if (!iconFile.exists()) {
                IconsCreator creator = IconsCreator.getDefault();
                
                if (icon.isIcon) {
                    if (!creator.createIconForExpression(icon.getText(), constructSizeString(icon))) {
                        iconFile = null;
                    }
                } else {
                    if (!creator.createIconForText(icon.getText())) {
                        iconFile = null;
                    }
                }
            }
            
            if (iconFile != null) {
                Image img = Toolkit.getDefaultToolkit().createImage(iconFile.getAbsolutePath());

                i = new ImageIcon(Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(img.getSource(), new MakeTransparentImage())));
            }
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
            i = null;
        }
        
        if (i != null)
            icon.setIcon(i);

        if (iconFile != null)
            icon.setCacheFileName(iconFile.getAbsolutePath());
    }
    
    private void setWaitingIcon(DelegatingIcon icon) {
        if (IconsCreator.getDefault().isConfigurationUsable()) {
            icon.setIcon(waitIcon);
        } else {
            icon.setIcon(noIcons);
        }
        
        synchronized (iconsToCreate) {
            iconsToCreate.put(icon);
            iconsToCreate.notifyAll();
        }
    }
    
    private ChangeableIcon getIconForExpressionImpl(String expression, int sizeX, int sizeY) {
        DelegatingIcon icon = new DelegatingIcon(expression, sizeX, sizeY, true);
        
        setWaitingIcon(icon);
        
        return icon;
    }

    private ChangeableIcon getIconForTextImpl(String text) {
        DelegatingIcon icon = new DelegatingIcon(text, 0, 0, false);
        
        
        if (IconsCreator.getDefault().isConfigurationUsable()) {
            synchronized (iconsToCreate) {
                iconsToCreate.put(icon);
                iconsToCreate.notifyAll();
            }
        }

        return icon;
    }
    
    public ChangeableIcon getIcon(String expression, int sizeX, int sizeY) {
        ChangeableIcon i = null;
        Reference<ChangeableIcon> sr = expression2Icon.get(expression);
        
        if (sr != null) {
            i = sr.get();
        }
        
        if (i == null) {
            i = getIconForExpressionImpl(expression, sizeX, sizeY);
            expression2Icon.put(expression, new SoftReference<ChangeableIcon>(i));
        }
        
        return i;
    }
    
    public ChangeableIcon getIconForText(String text) {
        ChangeableIcon i = null;
        Reference sr = expression2Icon.get(text);
        
        if (sr != null) {
            i = (ChangeableIcon) sr.get();
        }
        
        if (i == null) {
            i = getIconForTextImpl(text);
            expression2Icon.put(text, new SoftReference<ChangeableIcon>(i));
        }
        
        return i;
    }
    
    void configurationChanged() {
        for (Reference<ChangeableIcon> r : expression2Icon.values()) {
            DelegatingIcon icon = (DelegatingIcon) r.get();
            
            if (icon != null && (icon.delegateTo == waitIcon || icon.delegateTo == noIcons || (icon.getCacheFileName() != null && !new File(icon.getCacheFileName()).exists()))) {
                setWaitingIcon(icon);
            }
        }
    }

    private boolean delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                if (!delete(c))
                    return false;
            }
        }

        return f.delete();
    }

    public boolean clearIconsCache() {
        try {
            if (!delete(getIconDirectory()))
                return false;

            configurationChanged();
            return true;
        } catch (IOException e) {
            Logger.getLogger("global").log(Level.INFO,null, e);
            return false;
        }
    }
    
    private Map/*<DelegatingIcon, ChangeListener or List<ChangeListener>>*/ listeners;
    
    private class DelegatingIcon implements ChangeableIcon {
        
        private Icon   delegateTo;
        private String text;
        private String cacheFileName;
        private int    desiredSizeX;
        private int    desiredSizeY;
        private boolean isIcon;
        private boolean computed;
        
        public DelegatingIcon(Icon delegateTo, String text, int desiredSizeX, int desiredSizeY, boolean isIcon) {
            if (desiredSizeX < 0 || desiredSizeY < 0) {
                throw new IllegalArgumentException("desiredSizeX= " + desiredSizeX + ", desiredSizeY" + desiredSizeY);
            }
            
            this.delegateTo = delegateTo;
            this.text       = text;
            this.desiredSizeX = desiredSizeX;
            this.desiredSizeY = desiredSizeY;
            this.isIcon = isIcon;
        }
        
        public DelegatingIcon(String text, int desiredSizeX, int desiredSizeY, boolean isIcon) {
            this(null, text, desiredSizeX, desiredSizeY, isIcon);
        }
        
        //Only enclosing class should call this:
        private void setIcon(Icon delegateTo) {
            this.delegateTo = delegateTo;
            
            synchronized (this) {
                this.computed = true;
            }
            
            cs.fireChange();
        }
        
        private String getText() {
            return text;
        }

        private String getCacheFileName() {
            return cacheFileName;
        }

        private void setCacheFileName(String fileName) {
            this.cacheFileName = fileName;
        }
        
        private int getDesiredSizeX() {
            return desiredSizeX;
        }
        
        private int getDesiredSizeY() {
            return desiredSizeY;
        }
        
        private ChangeSupport cs = new ChangeSupport(this);
        
        public void removeChangeListener(ChangeListener l) {
            cs.removeChangeListener(l);
        }

        public void addChangeListener(ChangeListener l) {
            cs.addChangeListener(l);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (delegateTo != null) {
                delegateTo.paintIcon(c, g, x, y);
            }
        }
        
        public int getIconWidth() {
            return desiredSizeX <= 0 && delegateTo != null ? delegateTo.getIconWidth() : desiredSizeX;
        }

        public int getIconHeight() {
            return desiredSizeY <= 0 && delegateTo != null ? delegateTo.getIconWidth() : desiredSizeY;
        }

        public synchronized boolean isComputed() {
            return computed;
        }
        
    }

}
