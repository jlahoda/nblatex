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
package org.netbeans.modules.latex.gui;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.netbeans.modules.latex.model.ParseError;
import org.netbeans.modules.latex.model.Utilities;
import org.netbeans.modules.latex.model.command.SourcePosition;

/**
 *
 * @author Jan Lahoda
 */
public class NodeStorage extends Node implements PropertyChangeListener {
    
    private static final Logger LOG = Logger.getLogger(NodeStorage.class.getName());
    
    private Map objects;
    
    /** Creates a new instance of NodeStorage */
    public NodeStorage() {
        objects = new HashMap();
    }
    
    private transient Collection editors;
    private synchronized Collection getEditors() {
        if (editors == null)
            editors = new HashSet();
        
        return editors;
    }
    
    public void register(JComponent editor) {
        if (getEditors().contains(editor))
            throw new IllegalArgumentException("");
        
        getEditors().add(editor);
    }
    
    public void unregister(JComponent editor) {
        if (!getEditors().contains(editor))
            throw new IllegalArgumentException("");
        
        getEditors().remove(editor);
    }
    
    private void redrawImpl(JComponent editor, int x, int y, int w, int h) {
        editor.repaint(x, y, w, h);
    }
    
    private void redrawImpl(JComponent editor) {
        Rectangle bounds = editor.getBounds();
        
        redrawImpl(editor, (int) bounds.getX(), (int) bounds.getY(), (int) bounds.getWidth(), (int) bounds.getHeight());
    }
    
    public final void redrawAll() {
        Iterator iter = getEditors().iterator();
        
        while (iter.hasNext()) {
            redrawImpl((JComponent) iter.next());
        }
    }

    public final void redrawAll(int x, int y, int w, int h) {
        Iterator iter = getEditors().iterator();
        
        while (iter.hasNext()) {
            redrawImpl((JComponent) iter.next(), x, y, w, h);
        }
    }
    
    public void addObject(Node node) {
        addObject(node, null, null);
    }
    
    public void addObject(Node node, SourcePosition pos, Collection errors) {
        if (getObjectByID(node.getID()) != null) {
            String error = "Node with id=" + node.getID() + " already in the collection. Current node: " + getObjectByID(node.getID()) + ", new node: " + node + ".";
            
            if (errors != null) {
                errors.add(ParseError.create(ParseError.Severity.ERROR, "error.unknown", error, pos));
            } else {
                throw new IllegalArgumentException(error);
            }
        }
        
        objects.put(node.getID(), node);
        node.addPropertyChangeListener(this);
        node.setStorage(this);
    }
    
    public void removeObject(Node node) {
        objects.remove(node.getID());
        node.removePropertyChangeListener(this);
    }
    
    public void removeAllObjects(Collection nodes) {
        Iterator iter = nodes.iterator();
        
        while (iter.hasNext())
            objects.remove(iter.next());
    }
    
    public Collection<Node> getObjects() {
        return Collections.unmodifiableCollection(objects.values());
    }
    
    public Node getObjectByID(String id) {
        return (Node) objects.get(id);
    }
    
    public void draw(Graphics2D g) {
        Iterator iter = getObjects().iterator();
        
        while (iter.hasNext()) {
            ((Node) iter.next()).draw(g);
        }
    }
    
    public Rectangle2D getOuterDimension() {
        Rectangle2D r = null;
        Iterator iter = getObjects().iterator();
        
        while (iter.hasNext()) {
            Rectangle2D current = ((Node) iter.next()).getOuterDimension();
            
            if (r == null) {
                r = new Rectangle2D.Double(current.getX(), current.getY(), current.getWidth(), current.getHeight());
            } else {
                Rectangle2D.union(r, current, r);
            }
        }
        
        if (r == null)
            r = new Rectangle2D.Double();
        
        return r;
    }
    
    public void normalise() {
        Rectangle2D rec = getOuterDimension();
        int moveX = 0;
        int moveY = 0;
        
        if (rec.getX() < 0) {
            moveX = 1 - (int) (rec.getX() / UIProperties.getGridSize().getWidth());
        }
        
        if (rec.getY() < 0) {
            moveY = 1 - (int) (rec.getY() / UIProperties.getGridSize().getHeight());
        }
        
        Iterator it = objects.values().iterator();
        
        while (it.hasNext()) {
            Node n = (Node) it.next();
            
            if (n instanceof PositionNode) {
                PositionNode ps = (PositionNode) n;
                ps.setX(ps.getX() + moveX);
                ps.setY(ps.getY() + moveY);
            }
        }
        
        Iterator editor = editors.iterator();
        
        rec = getOuterDimension();
        
        while (editor.hasNext()) {
            ((JComponent) editor.next()).setSize((int) (rec.getWidth() + rec.getX()), (int) (rec.getHeight() + rec.getY()));
        }
    }
    
    public Node findNearestPoint(Point p) {
        double nearest = Double.POSITIVE_INFINITY;
        Node node    = null;
        Iterator iter  = getObjects().iterator();
        
        while (iter.hasNext()) {
            Node n = (Node) iter.next();
            
            double dist = n.distance(p);
            
            if (dist < nearest) {
                node = n;
                LOG.log(Level.FINE, "dist = {0}", dist);
                LOG.log(Level.FINE, "nearest = {0}", nearest);
                nearest = dist;
            }
        }
        
        return node;
    }
    
    public void outputVaucansonSource(PrintWriter out) {
        Rectangle2D r = getOuterDimension();
        
        out.println("\\VCDraw{");
        out.print("\\begin{VCPicture}{(");
        out.print((int) (r.getX() / UIProperties.getGridSize().getWidth()));
        out.print(",");
        out.print((int) (-r.getY() / UIProperties.getGridSize().getHeight()));
        out.print(")(");
        out.print((int) ((r.getWidth() + r.getX())/ UIProperties.getGridSize().getWidth()));
        out.print(",");
        out.print((int) (-(r.getHeight() + r.getY())/ UIProperties.getGridSize().getHeight()));
        out.println(")}");
        
        Iterator iter = getObjects().iterator();
        
        while (iter.hasNext()) {
            Object obj = iter.next();
            
            if (obj instanceof StateNode) {
                ((StateNode) obj).outputVaucansonSource(out);
            }
        }

        iter = getObjects().iterator();
        
        while (iter.hasNext()) {
            Object obj = iter.next();
            
            if (!(obj instanceof StateNode)) {
                ((Node) obj).outputVaucansonSource(out);
            }
        }
        
        out.println("\\end{VCPicture}");
        out.println("}");
    }    
    
    public double distance(Point p) {
        return Double.POSITIVE_INFINITY;
    }
    
    public void save(OutputStream out) throws IOException {
//        XMLEncoder enc = null;
//        
//        try {
//            enc = new XMLEncoder(new BufferedOutputStream(out));
//            
//            enc.writeObject(this);
//        } finally {
//            if (enc != null) {
//                enc.close();
//            }
//        }
        ObjectOutputStream enc = null;
        
        try {
            enc = new ObjectOutputStream(new BufferedOutputStream(out));
            
            enc.writeObject(this);
        } finally {
            if (enc != null) {
                try {
                    enc.close();
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        }

    }
    
    public void remove() {
        //empty
    }

    public void propertyChange(PropertyChangeEvent evt) {
        firePropertyChange(evt);
    }
    
    public boolean equals(NodeStorage storage) {
        if (!objects.keySet().equals(storage.objects.keySet()))
            return false;
        
        for (Iterator i = objects.keySet().iterator(); i.hasNext(); ) {
            String key  = (String) i.next();
            Node first  = (Node) objects.get(key);
            Node second = (Node) storage.objects.get(key);
            
            if (!first.equalsNode(second))
                return false;
        }
        
        return true;
    }
}
