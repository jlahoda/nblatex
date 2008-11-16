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
package org.netbeans.modules.latex.loop;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.gsf.api.CompilationInfo;
import org.netbeans.modules.gsf.api.EditHistory;
import org.netbeans.modules.gsf.api.Element;
import org.netbeans.modules.gsf.api.ElementHandle;
import org.netbeans.modules.gsf.api.IncrementalParser;
import org.netbeans.modules.gsf.api.OccurrencesFinder;
import org.netbeans.modules.gsf.api.OffsetRange;
import org.netbeans.modules.gsf.api.ParseEvent;
import org.netbeans.modules.gsf.api.Parser.Job;
import org.netbeans.modules.gsf.api.ParserFile;
import org.netbeans.modules.gsf.api.ParserResult;
import org.netbeans.modules.gsf.api.ParserResult.UpdateState;
import org.netbeans.modules.gsf.api.PositionManager;
import org.netbeans.modules.gsf.api.SemanticAnalyzer;
import org.netbeans.modules.gsf.api.SourceFileReader;
import org.netbeans.modules.gsf.api.TranslatedSource;
import org.netbeans.modules.latex.editor.TexLanguage;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.ParseError;
import org.netbeans.modules.latex.model.command.DocumentNode;
import org.netbeans.modules.latex.model.command.LaTeXSourceFactory;
import org.netbeans.modules.latex.model.command.impl.CommandUtilitiesImpl;
import org.netbeans.modules.latex.model.command.parser.CommandParser;
import org.netbeans.modules.latex.model.lexer.TexTokenId;
import org.netbeans.modules.latex.model.structural.StructuralElement;
import org.netbeans.modules.latex.model.structural.parser.StructuralParserImpl;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Jan Lahoda
 */
public class LaTeXGSFParser implements IncrementalParser {

    private static final boolean INCREMENTAL_REPARSE = !Boolean.getBoolean("latex.no.incremental.reparse");
    private static final Logger  LOG                 = Logger.getLogger(LaTeXGSFParser.class.getName());
    private static final Map<FileObject, StructuralParserImpl> file2Root = new WeakHashMap<FileObject, StructuralParserImpl>();
    
    public LaTeXGSFParser() {
    }

    public void parseFiles(Job job) {
        assert job.files.size() == 1;
        
        ParserFile parseFile = job.files.get(0);
        LaTeXParserResult r = parseFile(parseFile);

        if (r != null) {
            job.listener.finished(new ParseEvent(ParseEvent.Kind.PARSE, parseFile, r));
        }
    }

    public PositionManager getPositionManager() {
        return new PositionManager() {
//            public boolean isTranslatingSource() {
//                return false;
//            }
//
//            public int getLexicalOffset(ParserResult result, int astOffset) {
//                return astOffset;
//            }
//
//            public int getAstOffset(ParserResult result, int lexicalOffset) {
//                return lexicalOffset;
//            }
//
            public OffsetRange getOffsetRange(CompilationInfo info, ElementHandle eh) {
                throw new UnsupportedOperationException();
//                assert eh instanceof Node;
//
//                return new OffsetRange(((Node) eh).getStartingPosition().getOffsetValue(), ((Node) eh).getEndingPosition().getOffsetValue());
            }
        };
    }

    public SemanticAnalyzer getSemanticAnalysisTask() {
        return null;
    }

    public OccurrencesFinder getMarkOccurrencesTask(int caretPosition) {
        return null;
    }

    public ElementHandle createHandle(CompilationInfo info, Element element) {
        return null;
    }

    public Element resolveHandle(CompilationInfo info, ElementHandle handle) {
        return null;
    }

    private LaTeXParserResult parseFile(ParserFile parseFile) {
        try {
            List<ParseError> errors = new LinkedList<ParseError>();
            FileObject file = parseFile.getFileObject();
            FileObject main = null;
            for (LaTeXSourceFactory f : Lookup.getDefault().lookupAll(LaTeXSourceFactory.class)) {
                if (f.supports(file)) {
                    main = f.findMainFile(file);
                    if (main != null) {
                        break;
                    }
                }
            }
            assert main != null;
            final DocumentNode dn = reparseImpl(main, errors);
            StructuralParserImpl p = file2Root.get(main);
            if (p == null) {
                file2Root.put(main, p = new StructuralParserImpl());
            }
            StructuralElement structuralRoot = p.parse(dn, errors);
            return new LaTeXParserResult(this, parseFile, main, dn, structuralRoot, new CommandUtilitiesImpl(dn), errors);
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
            return null;
        }
    }

    private DocumentNode reparseImpl(FileObject main, List<ParseError> errors) throws IOException {
//        try {
            long start = System.currentTimeMillis();
            
            Collection newDocuments = new HashSet();
            
            long startParsing = System.currentTimeMillis();
            
            DocumentNode document = new CommandParser().parse(main, newDocuments, errors);
            
            long endParsing = System.currentTimeMillis();
            
            //XXX:
//            synchronized (REPARSE_IMPL_LOCK) {
//                DocumentNode oldDocument         = document;
//                Set          oldDocumentHardRefs = getDocumentsHardRefs();
//                
//                Collection<ErrorDescription> errors = new ArrayList();
//                
//                if (documents == null)
//                    documents = new HashSet();
//                else
//                    documents.clear();
//                
//                Iterator it = newDocuments.iterator();
//                
//                while (it.hasNext()) {
//                    documents.add(new WeakReference(it.next()));
//                }
//                
//                fireNodesRemoved(Collections.singletonList(oldDocument));
//                fireNodesAdded(Collections.singletonList(document));
//                
//                Set          newDocumentHardRefs = getDocumentsHardRefs();
//                
//                resolveListeners(oldDocumentHardRefs, newDocumentHardRefs);
//                
//                setErrors(document, errors);
//                
//                synchronized (DOCUMENT_VERSION_LOCK) {
//                    documentVersion++;
//                }
//            }
            
            long end = System.currentTimeMillis();
            
            Logger.getLogger("TIMER").log(Level.FINE, "LaTeX Parse", new Object[] {main, (endParsing - startParsing)});
            Logger.getLogger("TIMER").log(Level.FINE, "LaTeX Parse Complete", new Object[] {main, (end - start)});
            
//        } finally {
//            pcs.firePropertyChange("parsing", Boolean.TRUE, Boolean.FALSE);
//            if (isUpToDate())
//                setReparseStateDebug(REPARSE_DEBUG_ALL_VALID);
//        }
        
        return document;
    }

    public ParserResult parse(ParserFile file, SourceFileReader reader, TranslatedSource translatedSource, EditHistory history, ParserResult previousResult) {
        if (previousResult == null || previousResult.getInfo() == null || !INCREMENTAL_REPARSE) {
            return parseFile(file);
        }
        
        try {
            long start = System.currentTimeMillis();
            boolean fullReparse =    checkFullReparse(previousResult.getInfo().getText(), history.getStart(), history.getOriginalEnd())
                                  || checkFullReparse(reader.read(file),                  history.getStart(), history.getEditedEnd());

            if (!fullReparse) {
                previousResult.setUpdateState(UpdateState.NO_SEMANTIC_CHANGE);
                long end = System.currentTimeMillis();
                Logger.getLogger("TIMER").log(Level.FINE, "LaTeX Incremental Reparse", new Object[]{file.getFileObject(), (end - start)});
                return previousResult;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        LOG.log(Level.INFO, "Full Reparse");

        previousResult.setUpdateState(UpdateState.FAILED);
        
        return parseFile(file);
    }

    private boolean checkFullReparse(CharSequence text, int start, int end) {
        TokenHierarchy h = TokenHierarchy.create(text, TexLanguage.description());
        TokenSequence ts = h.tokenSequence();
        
        ts.move(start);
        while (ts.moveNext() && ts.offset() < end) {
            if (!DO_NOT_PARSE_TOKENS.contains(ts.token().id())) {
                return true;
            }
        }

        return false;
    }

    private static final Set<TexTokenId> DO_NOT_PARSE_TOKENS = EnumSet.of(TexTokenId.COMMENT, TexTokenId.WHITESPACE, TexTokenId.WORD);
    
}
