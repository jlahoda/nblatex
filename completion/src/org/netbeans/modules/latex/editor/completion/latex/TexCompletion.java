/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is the LaTeX module.
 * The Initial Developer of the Original Software is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2007.
 * All Rights Reserved.
 *
 * Contributor(s): Jan Lahoda.
 */
package org.netbeans.modules.latex.editor.completion.latex;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import org.netbeans.api.gsf.CancellableTask;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.retouche.source.CompilationController;
import org.netbeans.api.retouche.source.Phase;
import org.netbeans.api.retouche.source.Source;
import org.netbeans.modules.editor.fscompletion.spi.support.FSCompletion;
import org.netbeans.modules.editor.fscompletion.spi.support.FileObjectFilter;
import org.netbeans.modules.latex.editor.completion.latex.TexCompletionItem.BiBRecordCompletionItem;
import org.netbeans.modules.latex.editor.completion.latex.TexCompletionItem.CommandCompletionItem;
import org.netbeans.modules.latex.editor.completion.latex.TexCompletionItem.DocClassCompletionItem;
import org.netbeans.modules.latex.editor.completion.latex.TexCompletionItem.EnvironmentCompletionItem;
import org.netbeans.modules.latex.editor.completion.latex.TexCompletionItem.LabelCompletionItem;
import org.netbeans.modules.latex.editor.completion.latex.TexCompletionItem.ValueCompletionItem;
import org.netbeans.modules.latex.model.IconsStorage;
import org.netbeans.modules.latex.model.LaTeXParserResult;
import org.netbeans.modules.latex.model.LabelInfo;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.modules.latex.model.bibtex.PublicationEntry;
import org.netbeans.modules.latex.model.command.ArgumentContainingNode;
import org.netbeans.modules.latex.model.command.ArgumentNode;
import org.netbeans.modules.latex.model.command.BlockNode;
import org.netbeans.modules.latex.model.command.Command;
import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.CommandPackage;
import org.netbeans.modules.latex.model.command.Environment;
import org.netbeans.modules.latex.model.lexer.TexTokenId;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.modules.latex.model.command.Node;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;


/**
 *
 * @author  Jan Lahoda
 */
public class TexCompletion implements CompletionProvider {
    
    protected static void getCommandsForPrefix(CompletionResultSet resultSet, LaTeXParserResult lpr, Document doc, DataObject od, Position pos, String prefix, int start) throws BadLocationException {
        Object file = od.getPrimaryFile();
        try {
            SourcePosition spos = new SourcePosition(file, Utilities.getDefault().openDocument(file), pos);
            List commandsList = lpr.getCommandUtilities().getCommands(spos);
            
            if (commandsList != null) {
                Iterator commands = commandsList.iterator();
                
                while (commands.hasNext()) {
                    Command comm = (Command) commands.next();
                    String commandName = comm.getCommand();
                    
                    if (commandName.startsWith(prefix)) {
                        if (comm.hasAttribute("end") && comm.getArgumentCount() == 1 && comm.getArgument(0).hasAttribute("#environmentname")) {
                            BlockNode node = findBlockNode(lpr, doc, pos.getOffset());
                            resultSet.addItem(new ValueCompletionItem(start, commandName + "{" + node.getEnvironment().getName() + "}"));
                        } else {
                            boolean isIcon = IconsStorage.getDefault().getAllIconNames().contains(commandName);
                            resultSet.addItem(new CommandCompletionItem(start, comm, isIcon));
                        }
                    }
                }
            }
        } catch (IOException e) {
            ErrorManager.getDefault().notify(e);
        }
    }
    
    private static boolean isCommand(TokenId token) {
        return token == TexTokenId.COMMAND;
    }
    
    private static ArgumentNode lookupArgument(LaTeXParserResult lpr, Document doc, int offset) {
        try {
            Node node = lpr.getCommandUtilities().findNode(doc, offset);
            
            if (node instanceof ArgumentNode) {
                ArgumentNode anode = (ArgumentNode) node;

                Iterator tokens = anode.getDeepNodeTokens().iterator();

                if (   node.getStartingPosition().getOffsetValue() >= offset
                    && tokens.hasNext()
                    && ((Token) tokens.next()).id() == TexTokenId.COMP_BRACKET_LEFT) {
                    return null;
                }
                
                return (ArgumentNode) node;
            }
            
            return null;
        } catch (IOException e) {
            ErrorManager.getDefault().notify(e);
            return null;
        }
    }

    private static BlockNode findBlockNode(LaTeXParserResult lpr, Document doc, int offset) {
        try {
            Node node = lpr.getCommandUtilities().findNode(doc, offset);

            while (!(node instanceof BlockNode) && node != null) {
                node = node.getParent();
            }
            
            return (BlockNode) node;
        } catch (IOException e) {
            ErrorManager.getDefault().notify(e);
            return null;
        }
    }

    public static String          preprocessList(String prefix) {
        int lastComma = prefix.lastIndexOf(',');
        
        if (lastComma != (-1)) {
            prefix = prefix.substring(lastComma + 1);
        }
        
        return prefix;
    }
    
    private static interface ArgumentCompletionHandler {
        public String[]               getArgumentTags();
        public String                 preprocessPrefix(LaTeXParserResult lpr, ArgumentNode node, String prefix);
        public void                   getCompletionResult(CompletionResultSet set, LaTeXParserResult lpr, ArgumentNode node, String prefix, int start);
    }
    
    private static class RefArgumentCompletionHandler implements ArgumentCompletionHandler {
        
        public String[] getArgumentTags() {
            return new String[] {"#ref"};
        }
        
        public void getCompletionResult(CompletionResultSet set, LaTeXParserResult lpr, ArgumentNode node, String prefix, int start) {
            Collection/*<LabelInfo>*/ labels = Utilities.getDefault().getLabels(lpr);
            
            Iterator             labelsIter = labels.iterator();
            
            while (labelsIter.hasNext()) {
                LabelInfo info = (LabelInfo) labelsIter.next();
                
                if (!info.getLabel().startsWith(prefix))
                    continue;
                
                if (info.getCaption() == null || info.getCaption().length() == 0) {
                    set.addItem(new LabelCompletionItem(start, info.getLabel(), ""));
                } else {
                    set.addItem(new LabelCompletionItem(start, info.getLabel(), info.getCaption()));
                }
            }
        }
        
        public String preprocessPrefix(LaTeXParserResult lpr, ArgumentNode node, String prefix) {
            return prefix;
        }
        
    }
    
    private static class CiteArgumentCompletionHandler implements ArgumentCompletionHandler {
        
        public String[] getArgumentTags() {
            return new String[] {"#cite"};
        }
        
        public void getCompletionResult(CompletionResultSet set, LaTeXParserResult lpr, ArgumentNode node, String prefix, int start) {
            List<? extends PublicationEntry> references = Utilities.getDefault().getAllBibReferences(lpr);
            Iterator            referencesIter = references.iterator();
            
            for (PublicationEntry entry : references) {
                if (entry.getTag().startsWith(prefix)) {
                    set.addItem(new BiBRecordCompletionItem(start, entry));
                }
            }
        }
        
        public String preprocessPrefix(LaTeXParserResult lpr, ArgumentNode node, String prefix) {
            return preprocessList(prefix);
        }
        
    }
    
    private static class DocumentClassArgumentCompletionHandler implements ArgumentCompletionHandler {
        
        public String[] getArgumentTags() {
            return new String[] {"#documentclass", "#package"};
        }
        
        public void getCompletionResult(CompletionResultSet set, LaTeXParserResult lpr, ArgumentNode node, String prefix, int start) {
            Collection names;
            
            if (node.getArgument().hasAttribute("#documentclass")) {
                names = CommandPackage.getKnownDocumentClasses();
            } else {
                names = CommandPackage.getKnownPackages();
            }
            
            for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                String option = (String) iter.next();
                
                set.addItem(new DocClassCompletionItem(start, option));
            }
        }
        
        public String preprocessPrefix(LaTeXParserResult lpr, ArgumentNode node, String prefix) {
            if (node.getArgument().hasAttribute("#package"))
                return preprocessList(prefix);
            else
                return prefix;
        }
        
    }
    
    private static class DocumentClassOptionsArgumentCompletionHandler implements ArgumentCompletionHandler {
        
        public String[] getArgumentTags() {
            return new String[] {"#documentclassoptions", "#packageoptions"};
        }
        
        public void getCompletionResult(CompletionResultSet set, LaTeXParserResult lpr, ArgumentNode node, String prefix, int start) {
            ArgumentContainingNode container = node.getCommand();
            
            if (!(container instanceof CommandNode))
                return;
            
            CommandNode cnode = (CommandNode) container;
            String      name  = null;
            
            if (cnode.getArgumentCount() != 2)
                return ;
            
            name = cnode.getArgument(1).getText().toString();
            
            Collection[] options;
            
            options = new Collection[] {
                CommandPackage.getCommandPackageForName(name).getOptions().keySet(),
            };
            
            for (int cntr = 0; cntr < options.length; cntr++) {
                Iterator            referencesIter = options[cntr].iterator();
                
                while (referencesIter.hasNext()) {
                    String option = (String) referencesIter.next();
                    
                    set.addItem(new ValueCompletionItem(start, option));
                }
            }
        }
        
        public String preprocessPrefix(LaTeXParserResult lpr, ArgumentNode node, String prefix) {
            return preprocessList(prefix);
        }
        
    }

        private static class InputArgumentCompletionHandler implements ArgumentCompletionHandler {

            public String[] getArgumentTags() {
                return new String[] {"#include"};
            }

            public void getCompletionResult(CompletionResultSet set, LaTeXParserResult lpr, ArgumentNode node, String prefix, int start) {
                try {
                    set.addAllItems(FSCompletion.completion(null, lpr.getMainFile(), prefix, start, TEX_FILTER));
                    if (prefix.length() == 0)
                        set.addItem(new TexCompletionItem.NewFileCompletionItem(start, lpr.getMainFile()));
                } catch (IOException e) {
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                }
            }

            public String preprocessPrefix(LaTeXParserResult lpr, ArgumentNode node, String prefix) {
                return prefix;
            }

        }

    private static final TexFileFilter TEX_FILTER = new TexFileFilter("text/x-tex");

    private static class TexFileFilter implements FileObjectFilter {

        private String mimeType;

        public TexFileFilter(String mimeType) {
            this.mimeType = mimeType;
        }

        public boolean accept(FileObject file) {
            if (file.isFolder())
                return true;

            return mimeType.equals(FileUtil.getMIMEType(file));
        }

    }

    private static class EnvironmentArgumentCompletionHandler implements ArgumentCompletionHandler {
        
        public String[] getArgumentTags() {
            return new String[] {"#environmentname"};
        }
        
        public void getCompletionResult(CompletionResultSet set, LaTeXParserResult lpr, ArgumentNode node, String prefix, int start) {
            try {
                ArgumentContainingNode container = node.getCommand();
                
                if (!(container instanceof CommandNode))
                    return;
                
                CommandNode cnode = (CommandNode) container;
                List environments = lpr.getCommandUtilities().getEnvironments(cnode.getStartingPosition());
                Iterator            environmentsIter = environments.iterator();
                
                while (environmentsIter.hasNext()) {
                    Environment env = (Environment) environmentsIter.next();
                    
                    if (env.getName().startsWith(prefix))
                        set.addItem(new EnvironmentCompletionItem(start, env));
                }
            } catch (IOException e) {
                ErrorManager.getDefault().notify(e);
            }
        }
        
        public String preprocessPrefix(LaTeXParserResult lpr, ArgumentNode node, String prefix) {
            return prefix;
        }
        
    }
    
    private static ArgumentCompletionHandler[] handlers = new ArgumentCompletionHandler[] {
        new RefArgumentCompletionHandler(),
        new CiteArgumentCompletionHandler(),
        new DocumentClassOptionsArgumentCompletionHandler(),
        new InputArgumentCompletionHandler(),
        new EnvironmentArgumentCompletionHandler(),
        new DocumentClassArgumentCompletionHandler(),
    };
    
    private static boolean isArgumentCurlyBracket(ArgumentNode anode) {
        CharSequence text = anode.getFullText();

        return text.length() > 0 && text.charAt(0) == '{';
    }
    
    
    private static void getSpecialCommandArguments(CompletionResultSet set, LaTeXParserResult lpr, Document doc, int offset, int start) throws BadLocationException {
        ArgumentNode argument = lookupArgument(lpr, doc, offset);
        
        if (argument != null) {
            ArgumentContainingNode container = argument.getCommand();
            
            if (!(container instanceof CommandNode))
                return;
            
            CommandNode cnode = (CommandNode) container;
            String commandString = cnode.getCommand().getCommand();
            int    prefixLength = offset - argument.getStartingPosition().getOffsetValue();
            CharSequence argumentContent = argument.getText(); //jl: to be honest, I do not understand the stuff about argumentContent. getText returns only plain text, not commands!
            
            if (prefixLength < 0)
                return ;

            if (prefixLength == 1 && isArgumentCurlyBracket(argument)) {
                start++;
            }

            String ccPrefix = argumentContent.length() >= (prefixLength - 1) && prefixLength > 0 ? argumentContent.subSequence(0, prefixLength - 1).toString() : "";
            
            for (int cntr = 0; cntr < handlers.length; cntr++) {
                String[] attributes = handlers[cntr].getArgumentTags();
                
                for (int atr_cntr = 0; atr_cntr < attributes.length; atr_cntr++) {
                    if (argument.getArgument().hasAttribute(attributes[atr_cntr])) {
                        ccPrefix = handlers[cntr].preprocessPrefix(lpr, argument, ccPrefix);
                        
                        handlers[cntr].getCompletionResult(set, lpr, argument, ccPrefix, start);
                    }
                }
            }
            
            Command.Param param = argument.getArgument();
            
            for (Iterator i = param.getValues().iterator(); i.hasNext(); ) {
                String name = (String) i.next();
                
                if (name.startsWith(ccPrefix)) {
                    set.addItem(new ValueCompletionItem(start, name));
                }
            }
        }
    }
    
    private static boolean isArgument(LaTeXParserResult lpr, Document doc, int index) {
        return lookupArgument(lpr, doc, index) != null;
    }
    
    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (queryType == COMPLETION_QUERY_TYPE) {
            return new AsyncCompletionTask(new Query(), component);
        }
        
        return null;
    }
    
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return 0;
    }
    
    private static class Query extends AsyncCompletionQuery {
        
        protected void query(final CompletionResultSet resultSet, final Document doc, final int caretOffset) {
            final DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty); //TODO: this won't work in SA
            Source source = Source.forDocument(doc);
            try {
                source.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {}
                    public void run(CompilationController parameter) throws Exception {
                        parameter.toPhase(Phase.RESOLVED);
                        
                        LaTeXParserResult lpr = (LaTeXParserResult) parameter.getParserResult();
                                
                        int type;
                        
                        Token token = getToken(doc, caretOffset);
                        
                        String caption      = null;
                        int start = getStartingOffset(doc, caretOffset);
                        
                        if (isCommand(token.id())) {
                            Position pos = doc.createPosition(caretOffset);
                            String prefix = token.text().subSequence(0, caretOffset - start + 1).toString();
                            
                            getCommandsForPrefix(resultSet, lpr, doc, od, pos, prefix, start);
                        }
                        
                        if (isArgument(lpr, doc, caretOffset)) {
                            getSpecialCommandArguments(resultSet, lpr, doc, caretOffset, start);
                        }
                    }
                }, true);
                
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            } finally {
                resultSet.finish();
            }
        }
        
        protected void filter(CompletionResultSet resultSet) {
            super.filter(resultSet);
        }
        
    }


    public static final Token<TexTokenId> getToken(Document doc, int offset) throws ClassCastException {
        TokenSequence<TexTokenId> ts = (TokenSequence<TexTokenId>) TokenHierarchy.get(doc).tokenSequence();
        
        ts.move(offset > 0 ? offset - 1 : 0); //TODO: -1??/
        
        if (ts.moveNext())
            return ts.token();
        else
            return null;
    }
    
    public static final int getStartingOffset(Document doc, int offset) {
        TokenHierarchy h = TokenHierarchy.get(doc);
        TokenSequence ts = h.tokenSequence();
        
        ts.move(offset);
        ts.moveNext();
        
        Token orig = ts.token();
        int newOffset = ts.offset();
        
        if (getToken(doc, newOffset) == orig)
            return newOffset;
        
        if (getToken(doc, newOffset + 1) == orig)
            return newOffset + 1;
        
        throw new IllegalStateException("");
    }
}
