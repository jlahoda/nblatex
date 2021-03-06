/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2009.
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
package org.netbeans.modules.latex;


import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import junit.framework.Assert;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.lexer.InputAttributes;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.LanguagePath;
import org.netbeans.api.lexer.Token;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.latex.lexer.BiBTeXTokenId;
import org.netbeans.modules.latex.lexer.TexTokenId;
import org.netbeans.modules.latex.lexer.impl.TexLanguage;
import org.netbeans.modules.latex.lexer.impl.BiBTeXLanguage;
import org.netbeans.modules.latex.loop.LaTeXGSFParser;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.modules.latex.model.impl.NBUtilities;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.netbeans.spi.lexer.LanguageEmbedding;
import org.netbeans.spi.lexer.LanguageProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.LocalFileSystem;
import org.openide.filesystems.MultiFileSystem;
import org.openide.filesystems.Repository;
import org.openide.filesystems.XMLFileSystem;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.xml.sax.SAXException;

/**Inspired by org.netbeans.api.project.TestUtil.
 *
 * @author Jan Lahoda
 */
public class UnitUtilities extends ProxyLookup {
    
    public static UnitUtilities DEFAULT_LOOKUP = null;
    
    public UnitUtilities() {
        Assert.assertNull(DEFAULT_LOOKUP);
        DEFAULT_LOOKUP = this;
    }
    
    /**
     * Set the global default lookup with some fixed instances including META-INF/services/*.
     */
    public static void setLookup(Object[] instances, ClassLoader cl) {
        DEFAULT_LOOKUP.setLookups(new Lookup[] {
            Lookups.fixed(instances),
            Lookups.metaInfServices(cl),
            Lookups.singleton(cl),
        });
    }
    
    public static void prepareTest(String[] additionalLayers, Object[] additionalLookupContent) throws IOException, SAXException, PropertyVetoException {
        URL[] layers = new URL[additionalLayers.length + 1];
        
        layers[0] = Utilities.class.getResource("/org/netbeans/modules/latex/model/resources/mf-layer.xml");
	
	Assert.assertNotNull(layers[0]);
        
        for (int cntr = 0; cntr < additionalLayers.length; cntr++) {
            layers[cntr + 1] = Utilities.class.getResource(additionalLayers[cntr]);
	    Assert.assertNotNull(additionalLayers[cntr], layers[cntr + 1]);
        }
        
        XMLFileSystem system = new XMLFileSystem();
        system.setXmlUrls(layers);
        FileSystem fs = new MFSImpl(new FileSystem[] {FileUtil.createMemoryFileSystem(), system});
        
        Repository repository = new Repository(fs);
        Object[] lookupContent = new Object[additionalLookupContent.length + 4];
        
        System.arraycopy(additionalLookupContent, 0, lookupContent, 0, additionalLookupContent.length);
        
        lookupContent[additionalLookupContent.length] = repository;
        lookupContent[additionalLookupContent.length + 1] = new ModelUtilities();
        lookupContent[additionalLookupContent.length + 2] = new MimeDataProviderImpl();
        lookupContent[additionalLookupContent.length + 3] = new LanguageProviderImpl();
        
        DEFAULT_LOOKUP.setLookup(lookupContent, Utilities.class.getClassLoader());
    }
    
    private static final class MFSImpl extends MultiFileSystem {
        public MFSImpl(FileSystem[] fs) {
            super(fs);
            try {
                setSystemName("A");
            } catch (PropertyVetoException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
    static {
        UnitUtilities.class.getClassLoader().setDefaultAssertionStatus(true);
        System.setProperty("org.openide.util.Lookup", UnitUtilities.class.getName());
        Assert.assertEquals(UnitUtilities.class, Lookup.getDefault().getClass());
    }
    
    public static void initLookup() {
        //currently nothing.
    }
    
    /**Copied from org.netbeans.api.project.
     * Create a scratch directory for tests.
     * Will be in /tmp or whatever, and will be empty.
     * If you just need a java.io.File use clearWorkDir + getWorkDir.
     */
    public static FileObject makeScratchDir(NbTestCase test) throws IOException {
        test.clearWorkDir();
        File root = test.getWorkDir();
        assert root.isDirectory() && root.list().length == 0;
        FileObject fo = FileUtil.toFileObject(root);
        if (fo != null) {
            // Presumably using masterfs.
            return fo;
        } else {
            // For the benefit of those not using masterfs.
            LocalFileSystem lfs = new LocalFileSystem();
            try {
                lfs.setRootDirectory(root);
            } catch (PropertyVetoException e) {
                assert false : e;
            }
            Repository.getDefault().addFileSystem(lfs);
            return lfs.getRoot();
        }
    }
    
    private static class ModelUtilities extends NBUtilities {
        
        public Object getFile(Document doc) {
            return ((DataObject) doc.getProperty(Document.StreamDescriptionProperty)).getPrimaryFile();
        }
        
        private Map/*<File,Document>*/ file2Document = null;
        
        public Document openDocument(Object obj) throws IOException {
            if (file2Document == null) {
                file2Document = new HashMap();
            }
            
            Document doc = (Document) file2Document.get(obj);
            
            if (doc != null)
                return doc;
            
            InputStream  fis = null;
            
            try {
                FileObject   file = (FileObject) obj;
                int          read;
                StringBuffer test = new StringBuffer();
                
                fis = file.getInputStream();
                
                while ((read = fis.read()) != (-1)) {
                    test.append((char) read);
                }
                
                try {
                    doc = new DefaultStyledDocument();//new PlainDocument();
                    
                    doc.insertString(0, test.toString(), null);
                    doc.putProperty(Document.StreamDescriptionProperty,  DataObject.find((FileObject) obj));
                    
                    if ("tex".equals(file.getExt())) {
                        doc.putProperty("mimeType", "text/x-tex");
                        doc.putProperty(Language.class, TexTokenId.language());
                    }
                    
                    if ("bib".equals(file.getExt())) {
                        doc.putProperty("mimeType", "text/x-bibtex");
                        doc.putProperty(Language.class, BiBTeXTokenId.language());
                    }
                    
                    file2Document.put(obj, doc);
                    return doc;
                } catch (BadLocationException e) {
                    System.err.println("Should !never! happen:");
                    e.printStackTrace();
                    
                    return null;
                }
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        Exceptions.printStackTrace(e);
                    }
                }
            }
        }
        
    }

    private static final class MimeDataProviderImpl implements MimeDataProvider {

        public Lookup getLookup(MimePath mimePath) {
            if ("text/x-tex".equals(mimePath.getPath())) {
                return Lookups.fixed(new LaTeXGSFParser.FactoryImpl());
            }
            return Lookup.EMPTY;
        }
        
    }

    private static final class LanguageProviderImpl extends LanguageProvider {

        @Override
        public Language<?> findLanguage(String mimeType) {
            if ("text/x-tex".equals(mimeType))
                return TexTokenId.language();
            if ("text/x-bibtex".equals(mimeType))
                return BiBTeXTokenId.language();

            return null;
        }

        @Override
        public LanguageEmbedding<?> findLanguageEmbedding(Token<?> token, LanguagePath languagePath, InputAttributes inputAttributes) {
            return null;
        }
        
    }
}
