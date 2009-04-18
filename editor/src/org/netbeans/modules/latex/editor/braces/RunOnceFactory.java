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
package org.netbeans.modules.latex.editor.braces;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.latex.model.hacks.RegisterParsingTaskFactory;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Lahoda
 */
@RegisterParsingTaskFactory(mimeType="text/x-tex")
public class RunOnceFactory extends TaskFactory {

    private static final Logger LOG = Logger.getLogger(RunOnceFactory.class.getName());

    static {
        LOG.setLevel(Level.ALL);
    }

    private static RunOnceFactory INSTANCE;

    private List<Pair<FileObject, ParserResultTask<?>>> work = new LinkedList<Pair<FileObject, ParserResultTask<?>>>();
    private FileObject currentFile;
    private ParserResultTask<?> task;

    public RunOnceFactory() {
        INSTANCE = this;
    }

    @Override
    public synchronized Collection<ParserResultTask<?>> create(Snapshot s) {
        final ParserResultTask task = this.task;
        if (task == null) return Collections.emptyList();
        return Collections.<ParserResultTask<?>>singleton(new ParserResultTask<Parser.Result>() {
            public void cancel() {
                task.cancel();
            }

            @Override
            public void run(Result result, SchedulerEvent event) {
                task.run(result, event);
                next();
            }

            @Override
            public int getPriority() {
                return task.getPriority();
            }

            @Override
            public Class<? extends Scheduler> getSchedulerClass() {
                return OneTimeScheduler.class;
            }
        });
    }

    private synchronized void addImpl(FileObject file, ParserResultTask<?> task) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "addImpl({0}, {1})", new Object[] {FileUtil.getFileDisplayName(file), task.getClass().getName()});
        }

        work.add(new Pair<FileObject, ParserResultTask<?>>(file, task));

        if (currentFile == null)
            next();
    }

    private synchronized void next() {
        LOG.fine("next, phase 1");

        if (currentFile != null) {
            currentFile = null;
            task = null;
        }

        LOG.fine("next, phase 1 done");

        if (work.isEmpty())
            return ;

        LOG.fine("next, phase 2");

        Pair<FileObject, ParserResultTask<?>> p = work.remove(0);

        currentFile = p.getA();
        task = p.getB();

        if (SCHEDULER != null) {
            SCHEDULER.schedule(currentFile);
        }

        LOG.fine("next, phase 2 done");
    }

    public static void add(FileObject file, ParserResultTask<?> task) {
        if (INSTANCE == null)
            return ;

        INSTANCE.addImpl(file, task);
    }

    private static class Pair<A, B> {
        private A a;
        private B b;

        public Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }

        public A getA() {
            return a;
        }

        public B getB() {
            return b;
        }

    }

    private static OneTimeScheduler SCHEDULER;

    @ServiceProvider(service=Scheduler.class)
    public static final class OneTimeScheduler extends Scheduler {

        public OneTimeScheduler() {
            SCHEDULER = this;
        }

        @Override
        protected SchedulerEvent createSchedulerEvent(SourceModificationEvent event) {
            return new SchedulerEvent(event.getModifiedSource()) {};
        }

        public void schedule(FileObject file) {
            Source s = Source.create(file);
            
            schedule(s, new SchedulerEvent(s) {});
        }
        
    }
}
