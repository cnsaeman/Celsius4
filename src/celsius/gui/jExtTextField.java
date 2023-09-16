package celsius.gui;

import java.awt.event.ActionEvent;
import static java.awt.event.ActionEvent.SHIFT_MASK;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.Serializable;
import javax.swing.AbstractAction;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 *
 * @author cnsaeman
 */
public class jExtTextField extends JTextField implements Serializable, FocusListener {

    private final java.awt.Color lightgray=new java.awt.Color(204,204,204);
    public final UndoManager undoManager;
    private String defaultText;

    public jExtTextField() {
        super();
        addFocusListener(this);
        undoManager = new UndoManager();
        getDocument().addUndoableEditListener(undoManager);
        getActionMap().put("Undo", new AbstractAction("Undo") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } catch (CannotUndoException e) {
                }
            }
        });
        getActionMap().put("Redo", new AbstractAction("Redo") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                } catch (CannotRedoException e) {
                }
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,ActionEvent.CTRL_MASK+SHIFT_MASK), "Redo");
    }

    public String getDefaultText() {
        return defaultText;
    }

    public void setDefaultText(String value) {
        defaultText=value;
        resetText();
    }

    public void focusGained(FocusEvent e) {
        if (getText().equals(defaultText)) {
            setForeground(java.awt.Color.BLACK);
            super.setText("");
        }
    }

    public void focusLost(FocusEvent e) {
        if (getText().equals("")) {
            resetText();
        }
    }

    public void resetText() {
        setForeground(lightgray);
        super.setText(defaultText);
    }

    @Override
    public void setText(String s) {
        super.setText(s);
        setForeground(java.awt.Color.BLACK);
    }

}
