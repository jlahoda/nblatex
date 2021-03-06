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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.modules.latex.ui.viewer;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import javax.swing.ImageIcon;
import org.netbeans.modules.latex.ui.IconsCreator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author Jan Lahoda
 */
public class Renderer {

    private static final Renderer INSTANCE = new Renderer();

    public static Renderer getDefault() {
        return INSTANCE;
    }

    /** Creates a new instance of Renderer */
    private Renderer() {
    }

    private File getIconDirectory() {
        File iconDir = new File(new File(new File(new File(System.getProperty("netbeans.user"), "var"), "cache"), "latex"), "viewer");
        
        iconDir.mkdirs();
        
        return iconDir;
    }

    private File getCache(File original, int page, int resolution) {
        String originalName = original.getAbsolutePath().replace(File.separatorChar, '_');
        return new File(getIconDirectory(), originalName + "-" + page + "-r" + resolution + ".png");
    }

    private void createPNG(File original, int page, int resolution) throws IOException, InterruptedException {
        Process gs = Runtime.getRuntime().exec(new String[] {
            "gs",
            "-sDEVICE=png16m",
            "-dBATCH",
            "-dNOPAUSE",
            "-dGraphicsAlphaBits=4",
            "-dTextAlphaBits=4",
            "-dEPSFitPage",
            "-dFirstPage=" + page,
            "-dLastPage=" + page,
            "-sOutputFile=" + getCache(original, page, resolution).getAbsolutePath(),
            "-r" + resolution,
            original.getAbsolutePath()
        });
        
        IconsCreator.waitFor(gs);
    }

    public Image getImage(FileObject source, int page, int resolution) {
        try {
            File sourceFile = FileUtil.toFile(source);
            File cache = getCache(sourceFile, page, resolution);

            if (cache == null || !cache.exists() || cache.lastModified() < sourceFile.lastModified()) {
                createPNG(sourceFile, page, resolution);
            }

            Image img = Toolkit.getDefaultToolkit().createImage(cache.getAbsolutePath());
            
            return new ImageIcon(img).getImage();
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        } catch (InterruptedException e) {
            Exceptions.printStackTrace(e);
        }

        return null;
    }

}
