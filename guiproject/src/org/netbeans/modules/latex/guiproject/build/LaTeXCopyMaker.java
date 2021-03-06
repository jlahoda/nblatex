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
 * The Original Software is the LaTeX module.
 * The Initial Developer of the Original Software is Jan Lahoda.
 * Portions created by Jan Lahoda_ are Copyright (C) 2002-2007.
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
package org.netbeans.modules.latex.guiproject.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Document;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.windows.OutputWriter;

/** This class may not use System.err! (it is redirected by the ant!)
 *
 * @author Jan Lahoda
 */
/*package private*/ class LaTeXCopyMaker extends Thread {
    
    final OutputWriter out;
    final BufferedReader is;
    /** while set to false at streams that writes to the OutputWindow it must be
     * true for a stream that reads from the window.
     */
    final boolean autoflush;
    private boolean done = false;
    private Stack<String> currentFile;
    private int lineNumber;
    private Map<Document, List<ErrorDescription>> errors;
    private final Pattern pattern;
    private volatile boolean containsPattern;
    
    private final File baseDir;
    
    LaTeXCopyMaker(File baseDir, InputStream is, OutputWriter out) {
        this(baseDir, is, out, null);
    }

    LaTeXCopyMaker(File baseDir, InputStream is, OutputWriter out, Pattern pattern) {
        this.baseDir = baseDir;
        this.out = out;
        this.is = new BufferedReader(new InputStreamReader(/*new TypingInputStream*/(is)));
        this.pattern = pattern;
        autoflush = true;
        currentFile = new Stack<String>();
        lineNumber = 0;
        errors = new HashMap<Document, List<ErrorDescription>>();
    }

    private FileObject getCurrentFile() {
        String fileName = currentFile.peek();
        File file = FileUtil.normalizeFile(new File(baseDir, fileName));

        return FileUtil.toFileObject(file);
    }

    public Map<Document, List<ErrorDescription>> getErrors() {
        return errors;
    }

    public boolean containsPattern() {
        return containsPattern;
    }
    
    private class ErrorPattern {
        public Pattern pattern;
        public int     skip;
        public ErrorPattern(Pattern pattern, int skip) {
            this.pattern = pattern;
            this.skip    = skip;
        }
        
        public boolean process(String line) throws IOException {
            Matcher m = pattern.matcher(line);

            if (m.find() && !currentFile.isEmpty()) {
//                project.log("Pattern: \"" + pattern.pattern() + "\" found on line \"" + line + "\"", Project.MSG_DEBUG);
                
                String lineNum = m.group(1);
                try {
                    int lineNumber = Integer.parseInt(lineNum);
                    FileObject file = getCurrentFile();

//                    os.println(new File(baseDir, (String) currentFile.peek()).getAbsolutePath() + ":" + lineNumber + ":" + line);
                    if (file != null) {
                        out.println(line, new OutputListenerImpl(file, lineNumber, line), true);
                    } else {
                        out.println(line);
                    }
                    if (autoflush) out.flush();
                    
                    for (int cntr = 0; cntr < skip; cntr++) {
                        out.println(is.readLine());
                        lineNumber++;
                        
                        if (autoflush) out.flush();
                    }
                    
                    return true;
                } catch (NumberFormatException e) {
//                    project.log("NumberFormatException: " + e.getMessage(), Project.MSG_VERBOSE);
                }
            }
            
            return false;
        }
    }
    
    private static Pattern pageDef = Pattern.compile("^\\[[0-9]+\\]", Pattern.MULTILINE);
    
    private class OverfullErrorPattern extends ErrorPattern {
        public OverfullErrorPattern(Pattern pattern) {
            super(pattern, (-1));
        }
        
        public boolean process(String line) throws IOException {
            Matcher m = pattern.matcher(line);
            
            if (m.find() && !currentFile.isEmpty()) {
                String startLineNum = m.group(5);
                String endLineNum = m.group(6);
                try {
                    int startLineNumber = Integer.parseInt(startLineNum);
                    int endLineNumber   = Integer.parseInt(endLineNum);
                    FileObject file = getCurrentFile();
                    
//                    os.println(new File(baseDir, (String) currentFile.peek()).getAbsolutePath() + ":" + startLineNumber + ":0:" + endLineNumber  + ":0:" + line);
                    if (file != null) {
                        DataObject d = DataObject.find(file);
                        EditorCookie ec = (EditorCookie) d.getLookup().lookup(EditorCookie.class);
                        Document doc = ec.openDocument();
                        ErrorDescription err = ErrorDescriptionFactory.createErrorDescription(Severity.VERIFIER, line, doc, startLineNumber);
                        List<ErrorDescription> errorDescriptions = errors.get(doc);
                        
                        if (errorDescriptions == null) {
                            errors.put(doc, errorDescriptions = new ArrayList<ErrorDescription>());
                        }
                        
                        errorDescriptions.add(err);
                    }
                    
                    out.println(line);
                    if (autoflush) out.flush();
                    
                    while (!"".equals(line = is.readLine()) && line != null && !pageDef.matcher(line).find() && line.charAt(0) == ')') { //??sufficient
                        out.println(line);
                        lineNumber++;

                        if (autoflush) out.flush();
                    }
                    
                    lineNumber++;

                    out.println(line);
                    
                    if (autoflush) out.flush();
                    
                    return true;
                } catch (NumberFormatException e) {
                    //ignored...
                }
            }
            
            return false;
        }
    }
    
    private final ErrorPattern[] patterns = new ErrorPattern[] {
        new ErrorPattern(Pattern.compile("LaTeX Warning: [^\n]* on input line ([0123456789]*)"), 0),
        new ErrorPattern(Pattern.compile("l\\.([0123456789]*)"), 1),
        new OverfullErrorPattern(Pattern.compile("((Over)|(Under))full \\\\hbox (.*) in paragraph at lines ([0123456789]*)--([0123456789]*)")),
    };
    
    /* Makes copy. */
    public void run() {
        try {
            String line;
            
            MAIN_LOOP: while ((line = is.readLine()) != null) {
                lineNumber++;

                if (pattern != null && pattern.matcher(line).find()) {
                    containsPattern = true;
                }
                
                for (int pNumber = 0; pNumber < patterns.length; pNumber++) {
                    if (patterns[pNumber].process(line)) {
                        continue MAIN_LOOP;
                    }
                }
                
                for (int cntr = 0; cntr < line.length(); cntr++) {
                    switch (line.charAt(cntr)) {
                        case '(':
                            String file = readFileName(line, cntr + 1);
                            
//                            project.log("adding file: " + file, Project.MSG_DEBUG);
                            
                            currentFile.push(file);
                            break;
                        case ')':
                            if (!currentFile.isEmpty())
                                currentFile.pop();
//                                project.log("removing file: " + currentFile.pop().toString(), Project.MSG_DEBUG);
                            
                            break;
                    }
                }
                
                out.println(line);
                
                if (autoflush) out.flush();
            }
        } catch (IOException ex) {
        } finally {
            out.flush();
        }
    }
    
//    public void interrupt() {
//        super.interrupt();
//        done = true;
//    }
    
    private static String readFileName(String line, int cntr) {
        StringBuffer sb = new StringBuffer();
        String fileSpecChars = "/\\._-";
        
        while (   (cntr < line.length())
        && (Character.isLetterOrDigit(line.charAt(cntr)) || fileSpecChars.indexOf(line.charAt(cntr)) != (-1))){
            sb.append(line.charAt(cntr));
            cntr++;
        }
        
        return sb.toString();
    }
    
    private static class TypingInputStream extends InputStream {
        
        private InputStream ins;
        
        public TypingInputStream(InputStream ins) {
            this.ins = ins;
        }
        
        public int read() throws IOException {
            int r = ins.read();
            
            System.err.print((char) r);
            return r;
        }
        
    }
    
} // end of CopyMaker
