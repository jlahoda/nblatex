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
package org.netbeans.modules.latex.gui.nb;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.modules.latex.gui.AngleEdgeNode;
import org.netbeans.modules.latex.gui.EdgeNode;
import org.netbeans.modules.latex.gui.LineEdgeNode;
import org.netbeans.modules.latex.gui.LineStyle;
import org.netbeans.modules.latex.gui.LoopEdgeNode;
import org.netbeans.modules.latex.gui.Node;
import org.netbeans.modules.latex.gui.NodeStorage;
import org.netbeans.modules.latex.gui.StateNode;
import org.netbeans.modules.latex.gui.VArcEdgeNode;
import org.netbeans.modules.latex.gui.VCurveEdgeNode;
import org.netbeans.modules.latex.gui.VVCurveEdgeNode;
import org.netbeans.modules.latex.model.ParseError;
import org.netbeans.modules.latex.model.ParseError.Severity;
import org.netbeans.modules.latex.model.command.ArgumentNode;

import org.netbeans.modules.latex.model.command.CommandNode;
import org.netbeans.modules.latex.model.command.DefaultTraverseHandler;
import org.netbeans.modules.latex.model.command.SourcePosition;
import org.netbeans.modules.latex.model.command.TextNode;
import org.openide.ErrorManager;

/**
 *
 * @author Jan Lahoda
 */
public final class VauParserImpl {
    
    private static final String VAU_PICTURE_COMMAND = "\\VCDraw";
    
    /** Creates a new instance of VauParser */
    public VauParserImpl() {
    }

    public NodeStorage parse(CommandNode node, Collection<ParseError> errors) {
        NodeStorage storage = new NodeStorage();
        
        parsePicture(node, storage, errors);
        
        return storage;
    }

    private void parsePicture(CommandNode node, NodeStorage storage, Collection<ParseError> errors) {
        VauTraverseHandler vth = new VauTraverseHandler(getCommandHandlers(), storage, errors);
        
        node.traverse(vth);
    }
    
    private static class VauTraverseHandler extends DefaultTraverseHandler {
        private Map commandHandlers;
        private NodeStorage storage;
        Collection<ParseError> errors;
        private Map attributes;
        
        public VauTraverseHandler(Map commandHandlers, NodeStorage storage, Collection<ParseError> errors) {
            this.commandHandlers = commandHandlers;
            this.storage         = storage;
            this.errors          = errors;
            this.attributes      = new HashMap();
        }
        
        @Override
        public boolean commandStart(CommandNode node) {
            if (node.getArgumentCount() != node.getCommand().getArgumentCount()) {
                errors.add(createError("Command " + node.getCommand().getCommand() + " is supposed to have " + node.getCommand().getArgumentCount() + " arguments, but found " + node.getArgumentCount() + ".", node.getStartingPosition()));
            }
           
            String command = node.getCommand().getCommand();
            
            if (VAU_PICTURE_COMMAND.equals(command))
                return true;
            
            if ("\\begin".equals(command) || "\\end".equals(command))
                return true;
            
            CommandParser parser = (CommandParser) commandHandlers.get(command);
            
            if (parser == null) {
                errors.add(createError("Unknown command=" + node.getCommand().getCommand() + ".", node.getStartingPosition()));
                
                return false;
            }
            
            parser.parseCommand(node, storage, errors, attributes);
            
            return false;//The commands in deeper layers are considered to be part of the arguments (ie. state names or edges values), and are not parsed.
        }
    }
    
    private Map<String, CommandParser> commandHandlers;
    private synchronized Map<String, CommandParser> getCommandHandlers() {
        if (commandHandlers == null) {
            commandHandlers = new HashMap<String, CommandParser>();
            
            addCommand(commandHandlers, new StateCommandParser());
            addCommand(commandHandlers, new EdgeBorderParser());
            addCommand(commandHandlers, new EdgeCommandParser());
            addCommand(commandHandlers, new LoopCommandParser());
            addCommand(commandHandlers, new VCurveCommandParser());
            addCommand(commandHandlers, new VArcCommandParser());
            addCommand(commandHandlers, new ArcEdgeCommandParser());
            addCommand(commandHandlers, new VVCurveCommandParser());
            addCommand(commandHandlers, new EdgeLineStyleParser());
        }
        
        return commandHandlers;
    }
    
    private void addCommand(Map<String, CommandParser> commandHandlers, CommandParser parser) {
        String[] commands = parser.getSupportedCommandsName();
        
        for (int cntr = 0; cntr < commands.length; cntr++) {
            commandHandlers.put(commands[cntr], parser);
        }
    }
    
    private static int[] parseCoordinates(TextNode node, Collection<ParseError> errors) {
        String coordinatesString = node.getText().toString().trim();
        
        if (   coordinatesString.charAt(0) != '('
            || coordinatesString.charAt(coordinatesString.length() - 1) != ')') {
            errors.add(createError("Incorrect coordinates: " + coordinatesString + ".", node.getStartingPosition()));
            return new int[2];
        }
        
        coordinatesString = coordinatesString.substring(1, coordinatesString.length() - 1);
        
        StringTokenizer commas = new StringTokenizer(coordinatesString, ",");
        
        if (!commas.hasMoreTokens()) {
            errors.add(createError("Incorrect coordinates: " + coordinatesString + ".", node.getStartingPosition()));
            return new int[2];
        }
        
        String xValue = commas.nextToken();
        
        if (!commas.hasMoreTokens()) {
            errors.add(createError("Incorrect coordinates: " + coordinatesString + ".", node.getStartingPosition()));
            return new int[2];
        }
        
        String yValue = commas.nextToken();
        
        if (commas.hasMoreTokens()) {
            errors.add(createError("Incorrect coordinates: " + coordinatesString + ".", node.getStartingPosition()));
            return new int[2];
        }
        
        try {
            return new int[] {
                Integer.parseInt(xValue),
                -Integer.parseInt(yValue)
            };
        } catch (NumberFormatException e) {
          errors.add(createError("Incorrect coordinates: " + coordinatesString + ".", node.getStartingPosition()));
            return new int[2];
        }
    }
    
    private static Map<String, String> parseOptions(TextNode node, Collection<ParseError> errors) {
        String              options   = node.getText().toString();
        StringTokenizer     tokenizer = new StringTokenizer(options, ",");
        Map<String, String> result    = new HashMap<String, String>();
        
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            
            StringTokenizer equals = new StringTokenizer(token, "=");
            
            if (!equals.hasMoreTokens()) {
                errors.add(createError("Incorrect options: " + options + ".", node.getStartingPosition()));
                return Collections.emptyMap();
            }
            
            String name = equals.nextToken();
            
            if (!equals.hasMoreTokens()) {
                errors.add(createError("Incorrect options: " + options + ".", node.getStartingPosition()));
                return Collections.emptyMap();
            }
            
            String value = equals.nextToken();
            
            if (equals.hasMoreTokens()) {
                errors.add(createError("Incorrect options: " + options + ".", node.getStartingPosition()));
                return Collections.emptyMap();
            }
            
            result.put(name, value);
        }
        
        return result;
    }
    
    private static CharSequence getFullTextForArgument(ArgumentNode an) {
        CharSequence seq = an.getFullText();
        
        if ("[{".indexOf(seq.charAt(0)) != (-1)) {
            return seq.subSequence(1, seq.length() - ("}]".indexOf(seq.charAt(seq.length() - 1)) != (-1) ? 1 : 0));
        }
        
        return seq;
    }
    
    private static interface CommandParser {
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes);
        public String[] getSupportedCommandsName();
    }
    
    private static boolean edgeBorder = false;
    
    private class EdgeBorderParser implements CommandParser {
        /*Without arguments!!!*/
        
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\EdgeBorder",
                "\\EdgeBorderOff",
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            if (command.getCommand().getCommand().length() > 12)
                edgeBorder = false;
            else
                edgeBorder = true;
        }
        
    }
    
    private class StateCommandParser implements CommandParser {
        
        private int currentStateSize = 2;
        private boolean hidden = false;
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\SmallState",
                "\\MediumState",
                "\\LargeState",
                "\\VSState",
                "\\State",
                "\\FinalState",
                "\\Initial",
                "\\HideState",
                "\\ShowState",
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            boolean isFinal = false;
            String commandString = command.getCommand().getCommand();
            
            if ("\\HideState".equals(commandString)) {
                hidden = true;
                return ;
            }

            if ("\\ShowState".equals(commandString)) {
                hidden = false;
                return ;
            }
            
            if ("\\SmallState".equals(commandString)) {
                currentStateSize = 1;
                return ;
            }
            
            if ("\\MediumState".equals(commandString)) {
                currentStateSize = 2;
                return ;
            }
            
            if ("\\LargeState".equals(commandString)) {
                currentStateSize = 3;
                return ;
            }
            
            if ("\\Initial".equals(commandString)) {
                String id = command.getArgument(1).getText().toString();
                Node   node = storage.getObjectByID(id);
                
                if (node instanceof StateNode) {
                    ((StateNode) node).setInitialState(true);
                } else {
                  errors.add(createError("Node " + node + " is not a state.", command.getStartingPosition()));
                }
                
                return ;
            }

            if ("\\Final".equals(commandString)) {
                String id = command.getArgument(1).getText().toString();
                Node   node = storage.getObjectByID(id);
                
                if (node instanceof StateNode) {
                    ((StateNode) node).setFinalState(true);
                } else {
                    errors.add(createError("Node " + node + " is not a state.", command.getStartingPosition()));
                }
                
                return ;
            }
            
            if (commandString.charAt(1) == 'F')
                isFinal = true;
            
            int[] coords = parseCoordinates(command.getArgument(1), errors);
            
            StateNode state = new StateNode(coords[0], coords[1]);

            if (command.getArgument(0).isPresent()) {
                state.setName(getFullTextForArgument(command.getArgument(0)).toString());
                
                if ("\\VSState".equals(commandString)) {
                    errors.add(createError("The name attribute is not supported for Very Small States (I guess).", command.getStartingPosition()));
                }
            }
            
            state.setID(command.getArgument(2).getText().toString());
            state.setFinalState(isFinal);
            
            if ("\\VSState".equals(commandString)) {
                state.setDiameterIndex(0);
            } else {
                state.setDiameterIndex(currentStateSize);
            }
            
            state.setIsHidden(hidden);

            LineStyle lstyle = (LineStyle) attributes.get(EdgeLineStyleParser.CURRENT_STATE_LINE_STYLE);
            
            if (lstyle == null) {
                lstyle = (LineStyle) attributes.get(EdgeLineStyleParser.DEFAULT_STATE_LINE_STYLE);
            }
            
            if (lstyle != null) {
                state.setLineStyle(lstyle);
            }
            
            storage.addObject(state, command.getStartingPosition(), errors);
        }
        
    }
    
    private abstract class AbstractEdgeParser implements CommandParser {
        protected EdgeNode parseEdge(CommandNode command, Class nodeClass, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            String   commandString = command.getCommand().getCommand();
            boolean  isLeft        = commandString.charAt(commandString.length() - 1) == 'L';
            Node     leftState     = storage.getObjectByID(command.getArgument(command.getArgumentCount() - 3).getText().toString());
            Node     rightState    = storage.getObjectByID(command.getArgument(command.getArgumentCount() - 2).getText().toString());
            
            if (!(leftState instanceof StateNode)) {
                errors.add(createError("Node " + leftState + " is not a state.", command.getStartingPosition()));
                
                return null;
            }
            
            if (!(rightState instanceof StateNode)) {
                errors.add(createError("Node " + rightState + " is not a state.", command.getStartingPosition()));
                
                return null;
            }
            
            EdgeNode node = null;
            
            try {
                Constructor c = nodeClass.getConstructor(new Class[] {StateNode.class, StateNode.class});
                
                node = (EdgeNode) c.newInstance(new Object[] {leftState, rightState});
            } catch (Exception e) {
                errors.add(createError(e.getMessage(), command.getStartingPosition()));
                
                return null;
            }
            
            if (command.getArgument(0).isPresent()) {
                try {
                    String position = command.getArgument(0).getFullText().toString().replaceFirst("\\[(.*)\\]", "$1");
                    double labelPosition = Double.parseDouble(position);
                    
                    node.setLabelPosition(labelPosition);
                } catch (NumberFormatException e) {
                    ErrorManager.getDefault().annotate(e, ErrorManager.INFORMATIONAL, "VauParserImpl.AbstractEdgeParser, trying to parse: " + command.getArgument(0).getFullText().toString() + " for command: " + command.getCommand().getCommand(), null, null, null);
                    Logger.getLogger("global").log(Level.INFO,null, e);
                }
            }
            
            node.setOrientation(isLeft ? EdgeNode.LEFT : EdgeNode.RIGHT);
            node.setName(getFullTextForArgument(command.getArgument(command.getArgumentCount() - 1)).toString());
            node.setBorder(edgeBorder);
            
            LineStyle lstyle = (LineStyle) attributes.get(EdgeLineStyleParser.CURRENT_EDGE_LINE_STYLE);
            
            if (lstyle == null) {
                lstyle = (LineStyle) attributes.get(EdgeLineStyleParser.DEFAULT_EDGE_LINE_STYLE);
            }
            
            if (lstyle != null) {
                node.setLineStyle(lstyle);
            }
            
            storage.addObject(node, command.getStartingPosition(), errors);
            
            return node;
        }
        
    }
    
    private class EdgeCommandParser extends AbstractEdgeParser {
        
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\EdgeR",
                "\\EdgeL",
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            parseEdge(command, LineEdgeNode.class, storage, errors, attributes);
        }
        
    }

    private class LoopCommandParser implements CommandParser {
        
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\LoopN",
                "\\LoopE",
                "\\LoopS",
                "\\LoopW",
                "\\LoopNE",
                "\\LoopNW",
                "\\LoopSE",
                "\\LoopSW",
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            String   commandString = command.getCommand().getCommand();
            int orientation = LoopEdgeNode.NORTH;
            
            if (commandString.charAt(commandString.length() - 2) == 'p') {
                switch (commandString.charAt(commandString.length() - 1)) {
                    case 'N': orientation = LoopEdgeNode.NORTH; break;
                    case 'E': orientation = LoopEdgeNode.EAST; break;
                    case 'S': orientation = LoopEdgeNode.SOUTH; break;
                    case 'W': orientation = LoopEdgeNode.WEST; break;
                    
                    default:
                        errors.add(createError("Loop handler, should never happen.", command.getStartingPosition()));
                }
            } else {
                String directionString = commandString.substring(commandString.length() - 2);
                
                if ("NE".equals(directionString)) {
                    orientation = LoopEdgeNode.NORTHEAST;
                }
                
                if ("SE".equals(directionString)) {
                    orientation = LoopEdgeNode.SOUTHEAST;
                }
                
                if ("NW".equals(directionString)) {
                    orientation = LoopEdgeNode.NORTHWEST;
                }
                
                if ("SW".equals(directionString)) {
                    orientation = LoopEdgeNode.SOUTHWEST;
                }
            }
            
            Node     leftState     = storage.getObjectByID(command.getArgument(1).getText().toString());
            
            if (!(leftState instanceof StateNode)) {
                errors.add(createError("Node " + leftState + " is not a state.", command.getStartingPosition()));
            }
            
            LoopEdgeNode lineEdge  = new LoopEdgeNode((StateNode) leftState);
            
            if (command.getArgument(0).isPresent()) {
                try {
                    String position = command.getArgument(0).getFullText().toString().replaceFirst("\\[(.*)\\]", "$1");
                    double labelPosition = Double.parseDouble(position);
                    
                    lineEdge.setLabelPosition(labelPosition);
                } catch (NumberFormatException e) {
                    ErrorManager.getDefault().annotate(e, ErrorManager.INFORMATIONAL, "VauParserImpl.AbstractEdgeParser, trying to parse: " + command.getArgument(0).getFullText().toString() + " for command: " + command.getCommand().getCommand(), null, null, null);
                    Logger.getLogger("global").log(Level.INFO,null, e);
                }
            }

            lineEdge.setDirection(orientation);
            lineEdge.setName(getFullTextForArgument(command.getArgument(2)).toString());
            
            storage.addObject(lineEdge, command.getStartingPosition(), errors);
        }
        
    }

    private class ArcEdgeCommandParser extends AbstractEdgeParser {
        
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\ArcR",
                "\\ArcL",
                "\\LArcR",
                "\\LArcL",
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            String   commandString = command.getCommand().getCommand();
            boolean  isBig         = commandString.charAt(1) == 'L';
            
            AngleEdgeNode lineEdge  = (AngleEdgeNode) parseEdge(command, AngleEdgeNode.class, storage, errors, attributes);

            if (lineEdge != null)
                lineEdge.setAngle(isBig ? 2 : 1);
        }
        
    }

    private class VArcCommandParser extends AbstractEdgeParser {
        
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\VArcR",
                "\\VArcL",
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            VArcEdgeNode varcEdge  = (VArcEdgeNode) parseEdge(command, VArcEdgeNode.class, storage, errors, attributes);
            
            if (varcEdge == null)
                return ;
            
            Map options = parseOptions(command.getArgument(1), errors);
            
            String angle = (String) options.get("arcangle");
            
            if (angle != null) {
                try {
                    varcEdge.setAngle(Double.parseDouble(angle));
                } catch (NumberFormatException e) {
                    errors.add(createError(e.getMessage(), command.getStartingPosition()));
                }
            }
            
            String curv= (String) options.get("ncurv");
            
            if (curv != null) {
                try {
                    varcEdge.setCurv(Double.parseDouble(curv));
                } catch (NumberFormatException e) {
                }
            }
        }
        
    }

    private class VCurveCommandParser extends AbstractEdgeParser {
        
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\VCurveR",
                "\\VCurveL",
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            VCurveEdgeNode varcEdge = (VCurveEdgeNode) parseEdge(command, VCurveEdgeNode.class, storage, errors, attributes);
            
            if (varcEdge == null)
                return ;
            
            Map options = parseOptions(command.getArgument(1), errors);
            
            String angleA = (String) options.get("angleA");
            
            if (angleA != null) {
                try {
                    varcEdge.setAngleSource(Double.parseDouble(angleA));
                } catch (NumberFormatException e) {
                    errors.add(createError(e.getMessage(), command.getStartingPosition()));
                }
            }

            String angleB = (String) options.get("angleB");
            
            if (angleB != null) {
                try {
                    varcEdge.setAngleTarget(Double.parseDouble(angleB));
                } catch (NumberFormatException e) {
                    errors.add(createError(e.getMessage(), command.getStartingPosition()));
                }
            }
            
            String curv= (String) options.get("ncurv");
            
            if (curv != null) {
                try {
                    varcEdge.setCurv(Double.parseDouble(curv));
                } catch (NumberFormatException e) {
                }
            }
        }
        
    }

    private class VVCurveCommandParser extends AbstractEdgeParser {
        
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\VVCurveR",
                "\\VVCurveL",
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            VVCurveEdgeNode varcEdge = (VVCurveEdgeNode) parseEdge(command, VVCurveEdgeNode.class, storage, errors, attributes);

            if (varcEdge == null)
                return ;
            
            Map options = parseOptions(command.getArgument(1), errors);
            
            String angleA = (String) options.get("angleA");
            
            if (angleA != null) {
                try {
                    varcEdge.setAngleSource(Double.parseDouble(angleA));
                } catch (NumberFormatException e) {
                    errors.add(createError(e.getMessage(), command.getStartingPosition()));
                }
            }
            
            String angleB = (String) options.get("angleB");
            
            if (angleB != null) {
                try {
                    varcEdge.setAngleTarget(Double.parseDouble(angleB));
                } catch (NumberFormatException e) {
                    errors.add(createError(e.getMessage(), command.getStartingPosition()));
                }
            }
            
            String curvA= (String) options.get("curvA");
            
            if (curvA != null) {
                try {
                    varcEdge.setSourceCurv(Double.parseDouble(curvA));
                } catch (NumberFormatException e) {
                }
            }

            String curvB= (String) options.get("curvB");
            
            if (curvB != null) {
                try {
                    varcEdge.setSourceCurv(Double.parseDouble(curvB));
                } catch (NumberFormatException e) {
                }
            }
        }
        
    }
    
    private static class EdgeLineStyleParser implements CommandParser {
        
        public static final String DEFAULT_EDGE_LINE_STYLE = "default-edge-line-style";
        public static final String CURRENT_EDGE_LINE_STYLE = "current-edge-line-style";
        
        public static final String DEFAULT_STATE_LINE_STYLE = "default-state-line-style";
        public static final String CURRENT_STATE_LINE_STYLE = "current-state-line-style";
        
        public String[] getSupportedCommandsName() {
            return new String[] {
                "\\RstEdgeLineStyle",
                "\\ChgEdgeLineStyle",
                "\\SetEdgeLineStyle",
                "\\RstStateLineStyle",
                "\\ChgStateLineStyle",
                "\\SetStateLineStyle"
            };
        }
        
        public void parseCommand(CommandNode command, NodeStorage storage, Collection<ParseError> errors, Map attributes) {
            String cmd = command.getCommand().getCommand();
            
            if ("\\RstEdgeLineStyle".equals(cmd)) {
                attributes.remove(CURRENT_EDGE_LINE_STYLE);
                return ;
            }
            
            if ("\\ChgEdgeLineStyle".equals(cmd)) {
                String style = command.getArgument(0).getText().toString();
                
                attributes.put(CURRENT_EDGE_LINE_STYLE, LineStyle.valueOf(style));
                return ;
            }
            
            if ("\\SetEdgeLineStyle".equals(cmd)) {
                String style = command.getArgument(0).getText().toString();
                LineStyle lstyle = LineStyle.valueOf(style);
                
                attributes.put(CURRENT_EDGE_LINE_STYLE, lstyle);
                attributes.put(DEFAULT_EDGE_LINE_STYLE, lstyle);
                return ;
            }

            if ("\\RstStateLineStyle".equals(cmd)) {
                attributes.remove(CURRENT_STATE_LINE_STYLE);
                return ;
            }
            
            if ("\\ChgStateLineStyle".equals(cmd)) {
                String style = command.getArgument(0).getText().toString();
                
                attributes.put(CURRENT_STATE_LINE_STYLE, LineStyle.valueOf(style));
                return ;
            }
            
            if ("\\SetStateLineStyle".equals(cmd)) {
                String style = command.getArgument(0).getText().toString();
                LineStyle lstyle = LineStyle.valueOf(style);
                
                attributes.put(CURRENT_STATE_LINE_STYLE, lstyle);
                attributes.put(DEFAULT_STATE_LINE_STYLE, lstyle);
                return ;
            }
        }
    }
    
    private static ParseError createError(final String description, final SourcePosition pos) {
        final Document doc = pos.getDocument();
        final ParseError[] result = new ParseError[1];
        
        assert doc != null;
        
        doc.render(new Runnable() {
            public void run() {
                result[0] = ParseError.create(Severity.ERROR, "error.unknown", description, pos);
            }
        });
        
        return result[0];
    }
    
}
