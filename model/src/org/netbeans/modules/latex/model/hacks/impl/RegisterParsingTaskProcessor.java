/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 *
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.latex.model.hacks.impl;

import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.openide.filesystems.annotations.LayerGeneratingProcessor;
import org.openide.filesystems.annotations.LayerGenerationException;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Lahoda
 */
@SupportedAnnotationTypes("org.netbeans.modules.latex.model.hacks.RegisterParsingTaskFactory")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@ServiceProvider(service=Processor.class)
public class RegisterParsingTaskProcessor extends LayerGeneratingProcessor {

    @Override
    protected boolean handleProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) throws LayerGenerationException {
        TypeElement te = processingEnv.getElementUtils().getTypeElement("org.netbeans.modules.latex.model.hacks.RegisterParsingTaskFactory");

        for (Element el : roundEnv.getElementsAnnotatedWith(te)) {
            for (AnnotationMirror am : el.getAnnotationMirrors()) {
                if (!te.equals(am.getAnnotationType().asElement())) {
                    continue;
                }

                String mimeType = null;
                int    position = Integer.MAX_VALUE;
                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : am.getElementValues().entrySet()) {
                    if (e.getKey().getSimpleName().contentEquals("mimeType")) {
                        mimeType = (String) e.getValue().getValue();
                        continue;
                    }
                    if (e.getKey().getSimpleName().contentEquals("position")) {
                        position = (Integer) e.getValue().getValue();
                        continue;
                    }
                }

                if (mimeType != null) {
                    if (mimeType.length() != 0) mimeType = "/" + mimeType;
                    layer(el).instanceFile("Editors" + mimeType, null, null).position(position).write();
                }
            }
        }

        return true;
    }

}
