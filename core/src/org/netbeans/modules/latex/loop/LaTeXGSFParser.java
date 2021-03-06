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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
import javax.swing.event.ChangeListener;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.ParseError;
import org.netbeans.modules.latex.model.command.DocumentNode;
import org.netbeans.modules.latex.model.command.LaTeXSourceFactory;
import org.netbeans.modules.latex.model.command.impl.CommandUtilitiesImpl;
import org.netbeans.modules.latex.model.command.parser.CommandParser;
import org.netbeans.modules.latex.model.hacks.RegisterParsingTaskFactory;
import org.netbeans.modules.latex.lexer.TexTokenId;
import org.netbeans.modules.latex.model.structural.StructuralElement;
import org.netbeans.modules.latex.model.structural.parser.StructuralParserImpl;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserFactory;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Jan Lahoda
 */
public class LaTeXGSFParser extends Parser {

    private static final boolean INCREMENTAL_REPARSE = !Boolean.getBoolean("latex.no.incremental.reparse");
    private static final Logger  LOG                 = Logger.getLogger(LaTeXGSFParser.class.getName());
    private static final Map<FileObject, StructuralParserImpl> file2Root = new WeakHashMap<FileObject, StructuralParserImpl>();
    
    public LaTeXGSFParser() {
    }

    private LaTeXParserResult parseFile(Snapshot sh) {
        try {
            List<ParseError> errors = new LinkedList<ParseError>();
            FileObject file = sh.getSource().getFileObject();
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
            return new LaTeXParserResult(sh, main, dn, structuralRoot, new CommandUtilitiesImpl(dn), errors);
        } catch (IOException e) {
            Exceptions.printStackTrace(e);
            return null;
        }
    }

    private DocumentNode reparseImpl(FileObject main, List<ParseError> errors) throws IOException {
        long start = System.currentTimeMillis();

        Collection newDocuments = new HashSet();

        long startParsing = System.currentTimeMillis();

        DocumentNode document = new CommandParser().parse(main, newDocuments, errors);

        long endParsing = System.currentTimeMillis();

        long end = System.currentTimeMillis();

        Logger.getLogger("TIMER").log(Level.FINE, "LaTeX Parse", new Object[] {main, (endParsing - startParsing)});
        Logger.getLogger("TIMER").log(Level.FINE, "LaTeX Parse Complete", new Object[] {main, (end - start)});
        
        return document;
    }

//    public ParserResult parse(ParserFile file, SourceFileReader reader, TranslatedSource translatedSource, EditHistory history, ParserResult previousResult) {
//        if (previousResult == null || previousResult.getInfo() == null || history.getStart() == (-1) || !INCREMENTAL_REPARSE) {
//            return parseFile(file);
//        }
//
//        try {
//            long start = System.currentTimeMillis();
//            boolean fullReparse =    checkFullReparse(previousResult.getInfo().getText(), history.getStart(), history.getOriginalEnd())
//                                  || checkFullReparse(reader.read(file),                  history.getStart(), history.getEditedEnd());
//
//            if (!fullReparse) {
//                previousResult.setUpdateState(UpdateState.NO_SEMANTIC_CHANGE);
//                TokenHierarchy h = TokenHierarchy.create(reader.read(file), TexLanguage.description());
//
//                ((NBDocumentNodeImpl) ((LaTeXParserResult) previousResult).getDocument()).addUsedFile(file.getFileObject(), h);
//
//                long end = System.currentTimeMillis();
//                Logger.getLogger("TIMER").log(Level.FINE, "LaTeX Incremental Reparse", new Object[]{file.getFileObject(), (end - start)});
//                return previousResult;
//            }
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
//        }
//
//        LOG.log(Level.INFO, "Full Reparse");
//
//        previousResult.setUpdateState(UpdateState.FAILED);
//
//        return parseFile(file);
//    }

    private boolean checkFullReparse(CharSequence text, int start, int end) {
        TokenHierarchy h = TokenHierarchy.create(text, TexTokenId.language());
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

    private LaTeXParserResult lpr;
    
    @Override
    public void cancel() {}

    @Override
    public void addChangeListener(ChangeListener changeListener) {}

    @Override
    public void removeChangeListener(ChangeListener changeListener) {}

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
        lpr = parseFile(snapshot);
    }

    @Override
    public Result getResult(Task task) throws ParseException {
        return new LaTeXParserResult(lpr.getSnapshot(), lpr.getMainFile(), lpr.getDocument(), lpr.getStructuralRoot(), lpr.getCommandUtilities(), lpr.getErrors());
    }

    @RegisterParsingTaskFactory(mimeType="text/x-tex")
    public static final class FactoryImpl extends ParserFactory {

        @Override
        public Parser createParser(Collection<Snapshot> snapshots) {
            return new LaTeXGSFParser();
        }
        
    }
}
