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
package org.netbeans.modules.latex.ui.viewer;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.latex.model.platform.FilePosition;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.Line.ShowOpenType;
import org.openide.text.Line.ShowVisibilityType;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 *
 * @author Jan Lahoda
 */
public class DocumentTopComponent extends TopComponent /*implements KeyListener */{

    private static double sqrt2 = Math.sqrt(2);

    private static double[] zooms = new double[] {
        1.0/4.0,
        1.0/3.0,
        1.0/2.0,
        2.0/3.0,
        3.0/4.0,
        1.0/1.0,
        5.0/4.0,
        3.0/2.0,
        2.0/1.0,
        4.0/1.0,
    };

    private static final RequestProcessor WORKER = new RequestProcessor(DocumentTopComponent.class.getName(), 1, false, false);
    private static final String REFRESH_PREF_KEY = DocumentTopComponent.class.getName() + ".refresh";
    private static final String FOLLOW_PREF_KEY = DocumentTopComponent.class.getName() + ".follow";

    private DocumentComponent viewer;
    private int resolutionIndex;
    private ViewerImpl viewerImpl;
    private final JScrollPane spane;
    private List<DVIPageDescription> desc;
    private FileObject source;
    private PDFFile pdfFile;
    
    private JComboBox pages;
    private JComboBox zoom;
    private JToggleButton rebuildAutomatically;
    private JToggleButton followCaret;

    public DocumentTopComponent(final FileObject source, ViewerImpl viewerImpl) {
        setLayout(new BorderLayout());
        setDisplayName(source.getNameExt());
        resolutionIndex = 4;
	
	this.viewerImpl = viewerImpl;

        spane = new ScrollPaneImpl(viewer = new DocumentComponent());
        
        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);

        rebuildAutomatically = new JToggleButton(new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/latex/ui/resources/refresh.png")));
        followCaret = new JToggleButton(new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/latex/ui/resources/caret.png")));
        
        rebuildAutomatically.setToolTipText("Automatically rebuild on save");
        followCaret.setToolTipText("Follow caret");

        rebuildAutomatically.setSelected(NbPreferences.forModule(DocumentTopComponent.class).getBoolean(REFRESH_PREF_KEY, true));

        rebuildAutomatically.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleRebuildAutomatically();
                
                NbPreferences.forModule(DocumentTopComponent.class).putBoolean(REFRESH_PREF_KEY, rebuildAutomatically.isSelected());
            }
        });
        
        followCaret.setSelected(NbPreferences.forModule(DocumentTopComponent.class).getBoolean(FOLLOW_PREF_KEY, true));

        followCaret.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleRebuildAutomatically();
                
                NbPreferences.forModule(DocumentTopComponent.class).putBoolean(FOLLOW_PREF_KEY, followCaret.isSelected());
            }
        });
        
        pages = new JComboBox();

        pages.setRenderer(new DVIPageDescriptionRenderer());
        pages.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DVIPageDescription d = (DVIPageDescription) pages.getSelectedItem();
                
                viewer.setPage(d.getPageNumber());
            }
        });
        
        zoom = new JComboBox();
        
        DefaultComboBoxModel zoomModel = new DefaultComboBoxModel();
        
        zoomModel.addElement(ZoomKind.FIT_WIDTH);
        zoomModel.addElement(ZoomKind.FIT_PAGE);

        for (double zoom : zooms) {
            zoomModel.addElement(zoom);
        }
        
        zoom.setModel(zoomModel);
        zoom.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewer.updateSize();
            }
        });
        zoom.setRenderer(new ZoomRenderer());
        
        toolBar.add(new JLabel("Page:"));
        toolBar.add(pages);
        toolBar.add(new JLabel("Zoom:"));
        toolBar.add(zoom);
        toolBar.add(rebuildAutomatically);
        toolBar.add(followCaret);

        add(toolBar, BorderLayout.PAGE_START);
        add(spane, BorderLayout.CENTER);

        viewer.setEnclosingScrollPane(spane);

        toolBar.addKeyListener(viewer);
        addKeyListener(viewer);
	
        setFile(source);
    }
    
    private void handleRebuildAutomatically() {
        Project p = FileOwnerQuery.getOwner(source);

        if (rebuildAutomatically.isSelected()) {
            ProjectRebuilDer.INSTANCE.registerProject(p, followCaret.isSelected() ? this : null);
        } else {
            ProjectRebuilDer.INSTANCE.unregisterProject(p);
        }
    }
    
    private void setFile(final FileObject source) {
        final FileObject parent = source.getParent();
        final String name = source.getNameExt();
        this.source = source;
        
        final FileChangeListener sourceFileChangeListener = new FileChangeAdapter() {
            public void fileChanged(FileEvent fileEvent) {
                handleDVI(FileUtil.findBrother(source, "dvi"));
                updatePDFFile();
                viewer.showPage();
            }
        };
        
        source.addFileChangeListener(sourceFileChangeListener);
        
        parent.addFileChangeListener(new FileChangeAdapter() {
            public void fileDataCreated(FileEvent fileEvent) {
                if (name.equals(fileEvent.getFile().getNameExt())) {
                    parent.removeFileChangeListener(this);
                    source.removeFileChangeListener(sourceFileChangeListener);
                    setFile(fileEvent.getFile());
                    viewer.showPage();
                }
            }
        });
        
        handleDVI(FileUtil.findBrother(source, "dvi"));
        updatePDFFile();
    }
    
    FileObject getFile() {
        return source;
    }

    private boolean wasInitialized;

    public void setFilePosition(FilePosition scrollTo) {
        if (scrollTo != null) {
            int scrollToPage = findPageForPosition(scrollTo);
            
            if (scrollToPage != (-1)) {
                viewer.setPage(scrollToPage);
                wasInitialized = true;
            }
        }

        if (!wasInitialized) {
            viewer.setPage(1);
            wasInitialized = true;
        }
    }

    public boolean requestFocusInWindow() {
        super.requestFocusInWindow();
        return viewer.requestFocusInWindow();
    }

    public void requestFocus() {
        super.requestFocus();
        viewer.requestFocus();
    }

    @Override
    protected void componentShowing() {
        super.componentShowing();
        handleRebuildAutomatically();
    }

    @Override
    protected void componentHidden() {
        super.componentHidden();
        ProjectRebuilDer.INSTANCE.unregisterProject(FileOwnerQuery.getOwner(source));
    }

    @Override
    protected void componentClosed() {
        super.componentClosed();
        viewerImpl.componentClosed(this);
    }

    private void handleDVI(FileObject dvi) {
        if (dvi == null) {
            desc = Collections.emptyList();
            return ;
        }

        try {
            File dviFile = FileUtil.toFile(dvi);
            desc = new DVIParser().parse(dviFile);
        } catch (IOException ex) {
            Logger.getLogger("global").log(Level.INFO,null, ex);
            desc = Collections.emptyList();
        }
        
        //update the pages combo:
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        
        for (DVIPageDescription d : desc) {
            model.addElement(d);
        }
        
        pages.setModel(model);
    }

    private int findPageForPosition(FilePosition position) {
        int closestDifference = Integer.MAX_VALUE;
        DVIPageDescription closestPage = null;
        for (DVIPageDescription d : desc) {
            for (FilePosition p : d.getSourcePositions()) {
                if (p.getFile() == position.getFile() && p.getLine() <= position.getLine()) {
                    int dviLine = p.getLine();
                    int givenLine = position.getLine();

                    if (dviLine <= givenLine && closestDifference > (givenLine - dviLine)) {
                        closestDifference = givenLine - dviLine;
                        closestPage = d;
                    }
                }
            }
        }

        if (closestPage != null) {
            return closestPage.getPageNumber();
        } else {
            return -1;
        }
    }
    
    private void updatePDFFile() {
        try {
            File sourceFile = FileUtil.toFile(source);
            ByteBuffer pdfContent;

            if (sourceFile != null) {
                pdfContent = new FileInputStream(sourceFile).getChannel().map(MapMode.READ_ONLY, 0, sourceFile.length());
            } else {
                pdfContent = ByteBuffer.wrap(source.asBytes());
            }
            pdfFile = new PDFFile(pdfContent);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }
    
    public Object readResolve() {
        return this;
    }
    
    private static class DVIPageDescriptionRenderer extends DefaultListCellRenderer {
        
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof DVIPageDescription) {
                return super.getListCellRendererComponent(list, ((DVIPageDescription) value).getPageNumber(), index, isSelected, cellHasFocus);
            }
            
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }

    }

    private class DocumentComponent extends JComponent implements KeyListener, MouseListener, MouseMotionListener {

        private int page;
        private PDFPage currentPage;
        private JScrollPane spane;
        
        public DocumentComponent() {
            this.page = 1;
            
            setFocusable(true);
            setRequestFocusEnabled(true);
            setVerifyInputWhenFocusTarget(false);
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        
        private void setPage(int page) {
            if (page < 1)
                page = 1;
            
            if (desc.size() != 0 && desc.size() < page)
                page = desc.size();
            
            this.page = page;
            
            if (pages.getModel().getSize() >= page)
                pages.setSelectedIndex(page - 1);
            
            showPage();
        }
        
        protected void paintComponent(Graphics g) {
            if (currentPage == null) return ;
            
            Dimension targetSize = getPreferredSize();
            Rectangle targetBounds = new Rectangle(0, 0, targetSize.width, targetSize.height);
            PDFRenderer pdfRenderer = new PDFRenderer(currentPage, (Graphics2D) g, targetBounds, null, Color.WHITE);

            pdfRenderer.run();
        }
        
        private void showPage() {
            final Cursor original = getCursor();

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    final PDFPage currPage = pdfFile.getPage(page);
                    
                    try {
                        currPage.waitForFinish();
                    } catch (InterruptedException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            currentPage = currPage;
                            updateSize();

                            if (getCursor() == Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)) {
                                setCursor(original);
                            }
                        }
                    });
                }
            });
        }

        private void updateSize() {
            Dimension targetSize;
            JViewport viewport = spane.getViewport();

            if (currentPage == null) return;

            Object selectedZoom = zoom.getSelectedItem();

            if (selectedZoom == ZoomKind.FIT_WIDTH) {
                targetSize = new Dimension(viewport.getWidth(), (int) (viewport.getWidth() / currentPage.getAspectRatio()));
            } else if (selectedZoom == ZoomKind.FIT_PAGE) {
                int computedWidth = (int) (viewport.getHeight() * currentPage.getAspectRatio());
                if (computedWidth <= viewport.getWidth()) {
                    targetSize = new Dimension(computedWidth, viewport.getHeight());
                } else {
                    targetSize = new Dimension(viewport.getWidth(), (int) (viewport.getWidth() / currentPage.getAspectRatio()));
                }
            } else if (selectedZoom instanceof Double) {
                double zoom = (double) (Double) selectedZoom;

                targetSize = new Dimension((int) (currentPage.getWidth() * zoom), (int) (currentPage.getHeight() * zoom));
            } else {
                throw new IllegalStateException();
            }

            setPreferredSize(targetSize);
            invalidate();
            repaint();
        }

        private void setEnclosingScrollPane(JScrollPane spane) {
             this.spane = spane;
             spane.addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    updateSize();
                }
             });
        }

        public void keyTyped(KeyEvent e) {
        }
        
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_PAGE_DOWN:
                    setPage(page + 1);
                    break;
                case KeyEvent.VK_PAGE_UP:
                    setPage(page - 1);
                    break;
            }
        }
        
        public void keyReleased(KeyEvent e) {
        }

        private Cursor beforeDragging;
        private Point dragStart;
        private Rectangle startRectangle;

        public void mouseDragged(MouseEvent e) {
            if (beforeDragging == null) {
                beforeDragging = getCursor();
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                dragStart = e.getPoint();
                startRectangle = spane.getViewport().getViewRect();
            }

            Point p = new Point(dragStart);

            p.translate((int) -e.getPoint().getX(), (int) -e.getPoint().getY());

            startRectangle.translate((int) p.getX(), (int) p.getY());
            
            int xCorrection = startRectangle.getX() < 0 ? (int) -startRectangle.getX() : 0;
            int yCorrection = startRectangle.getY() < 0 ? (int) -startRectangle.getY() : 0;
            
            startRectangle.translate(xCorrection, yCorrection);
            
            dragStart.translate(xCorrection, yCorrection);
            
            scrollRectToVisible(startRectangle);
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1 && e.getModifiersEx() == MouseEvent.CTRL_DOWN_MASK && e.getButton() == MouseEvent.BUTTON1) {
                for (DVIPageDescription d : desc) {
                    if (d.getPageNumber() == page) {
                        List<FilePosition> p = d.getSourcePositions();

                        if (!p.isEmpty())
                            open(p.get(0));
                    }
                }
            }
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            if (getCursor() == Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)) {
                setCursor(beforeDragging);
                beforeDragging = null;
            }
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        private void open(FilePosition e) {
            try {
                DataObject d = DataObject.find(e.getFile());
                LineCookie lc = (LineCookie) d.getLookup().lookup(LineCookie.class);
                
                lc.getLineSet().getCurrent(e.getLine() - 1).show(ShowOpenType.OPEN, ShowVisibilityType.FOCUS);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

    }
    
    private enum ZoomKind {
        FIT_WIDTH,
        FIT_PAGE;
    }

    private static final class ZoomRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String toRender;

            if (value == ZoomKind.FIT_WIDTH) {
                toRender = "Fit Width";
            } else if (value == ZoomKind.FIT_PAGE) {
                toRender = "Fit Page";
            } else if (value instanceof Double) {
                toRender = String.format("%d%%", ((int) ((Double) value * 100)));
            } else {
                toRender = String.valueOf(value);
            }

            return super.getListCellRendererComponent(list, toRender, index, isSelected, cellHasFocus);
        }
    }

    private final class ScrollPaneImpl extends JScrollPane {
        ScrollPaneImpl(Component view) {
            super(view);
        }

        @Override
        protected JViewport createViewport() {
            return new ViewportImpl();
        }
    }

    private final class ViewportImpl extends JViewport {
        @Override
        public void reshape(int x, int y, int w, int h) {
            super.reshape(x, y, w, h);
            viewer.updateSize();
        }
    }
}
