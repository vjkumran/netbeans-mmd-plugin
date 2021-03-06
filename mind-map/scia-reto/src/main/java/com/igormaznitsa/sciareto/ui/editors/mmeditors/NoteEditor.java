/*
 * Copyright 2016 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.sciareto.ui.editors.mmeditors;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.text.Utilities;
import javax.swing.undo.UndoManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import com.igormaznitsa.mindmap.model.logger.Logger;
import com.igormaznitsa.mindmap.model.logger.LoggerFactory;
import com.igormaznitsa.sciareto.preferences.PreferencesManager;
import com.igormaznitsa.sciareto.preferences.SpecificKeys;
import com.igormaznitsa.sciareto.ui.DialogProviderManager;
import com.igormaznitsa.sciareto.ui.UiUtils;
import com.igormaznitsa.sciareto.ui.editors.TextEditor;

public final class NoteEditor extends javax.swing.JPanel {

  private static final long serialVersionUID = -1715683034655322518L;

  private static final Logger LOGGER = LoggerFactory.getLogger(NoteEditor.class);

  private final UndoManager undoManager = new UndoManager(){
    private static final long serialVersionUID = -239961738072597268L;
    @Override
    public void undoableEditHappened(@Nonnull final UndoableEditEvent e) {
      super.undoableEditHappened(e); 
      updateRedoUndoState();
    }
    
  };
  
  private enum Wrapping {

    NONE("none", "off"), //NOI18N
    CHAR_WRAP("char", "char"), //NOI18N
    WORD_WRAP("word", "word"); //NOI18N

    private final String value;
    private final String display;

    private Wrapping(@Nonnull final String val, @Nonnull final String display) {
      this.value = val;
      this.display = display;
    }

    @Nonnull
    public String getValue() {
      return this.value;
    }

    @Nonnull
    public String getDisplay() {
      return this.display;
    }

    @Nonnull
    public Wrapping next() {
      final int index = this.ordinal() + 1;
      if (index >= Wrapping.values().length) {
        return NONE;
      } else {
        return Wrapping.values()[index];
      }
    }

    @Nonnull
    public static Wrapping findFor(@Nonnull final String text) {
      for (final Wrapping w : Wrapping.values()) {
        if (w.value.equalsIgnoreCase(text)) {
          return w;
        }
      }
      return NONE;
    }
  }

  private static final FileFilter TEXT_FILE_FILTER = new FileFilter() {

    @Override
    public boolean accept(final File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".txt"); //NOI18N
    }

    @Override
    public String getDescription() {
      return java.util.ResourceBundle.getBundle("com/igormaznitsa/nbmindmap/i18n/Bundle").getString("PlainTextEditor.fileFilter.description");
    }
  };

  private Wrapping wrapping;

  private static boolean isWhitespaceOrControl(final char c) {
    return Character.isISOControl(c) || Character.isWhitespace(c);
  }

  private void updateRedoUndoState(){
    this.buttonUndo.setEnabled(this.undoManager.canUndo());
    this.buttonRedo.setEnabled(this.undoManager.canRedo());
  }

  private void doUndo(){
    if (this.undoManager.canUndo()){
      this.undoManager.undo();
    }
    updateRedoUndoState();
  }
  
  private void doRedo(){
    if (this.undoManager.canRedo()){
      this.undoManager.redo();
    }
    updateRedoUndoState();
  }
  
  public NoteEditor(@Nonnull final String text) {
    initComponents();

    this.buttonRedo.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK), "do-redo"); //NOI18N
    this.buttonRedo.getActionMap().put("do-redo", new AbstractAction(){ //NOI18N
      private static final long serialVersionUID = -5644390861803492172L;
      @Override
      public void actionPerformed(@Nonnull final ActionEvent e) {
        doRedo();
      }
    });
    
    this.buttonUndo.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),"do-undo"); //NOI18N
    this.buttonUndo.getActionMap().put("do-undo", new AbstractAction() { //NOI18N
      private static final long serialVersionUID = -5644390861803492172L;
      @Override
      public void actionPerformed(@Nonnull final ActionEvent e) {
        doUndo();
      }
    });
    
    this.editorPane.getActionMap().put(DefaultEditorKit.selectWordAction, new TextAction(DefaultEditorKit.selectWordAction) {
      private static final long serialVersionUID = -6477916799997545798L;
      private final Action start = new TextAction("wordStart") { //NOI18N
        private static final long serialVersionUID = 4377386270269629176L;
        @Override
        public void actionPerformed(@Nonnull final ActionEvent e) {
          final JTextComponent target = getTextComponent(e);
          try {
            if (target != null) {
              int offs = target.getCaretPosition();
              final Document doc = target.getDocument();
              final String text = doc.getText(0, doc.getLength());
              int startOffs = offs;
              if (startOffs < text.length()) {
                for (int i = offs; i >= 0; i--) {
                  if (!isWhitespaceOrControl(text.charAt(i))) {
                    startOffs = i;
                  } else {
                    break;
                  }
                }
                target.setCaretPosition(startOffs);
              }
            }
          } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(target);
          }
        }
      };
      private final Action end = new TextAction("wordEnd") { //NOI18N
        private static final long serialVersionUID = 4377386270269629176L;
        @Override
        public void actionPerformed(@Nonnull final ActionEvent e) {
          final JTextComponent target = getTextComponent(e);
          try {
            if (target != null) {
              int offs = target.getCaretPosition();

              final Document doc = target.getDocument();
              final String text = doc.getText(0, doc.getLength());
              int endOffs = offs;
              for (int i = offs; i < text.length(); i++) {
                endOffs = i;
                if (isWhitespaceOrControl(text.charAt(i))) {
                  break;
                }
              }
              if (endOffs < text.length() && !isWhitespaceOrControl(text.charAt(endOffs))) {
                endOffs++;
              }
              target.moveCaretPosition(endOffs);
            }
          } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(target);
          }
        }
      };

      @Override
      public void actionPerformed(@Nonnull final ActionEvent e) {
        this.start.actionPerformed(e);
        this.end.actionPerformed(e);
      }

    });

    this.setPreferredSize(new Dimension(640, 480));
    this.editorPane.setFont(PreferencesManager.getInstance().getFont(PreferencesManager.getInstance().getPreferences(), SpecificKeys.PROPERTY_TEXT_EDITOR_FONT, TextEditor.DEFAULT_FONT));
    this.editorPane.setText(text);
    this.addAncestorListener(new AncestorListener() {
      @Override
      public void ancestorAdded(@Nonnull final AncestorEvent event) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            editorPane.grabFocus();
            updateCaretPos();
          }
        });
      }

      @Override
      public void ancestorRemoved(@Nonnull final AncestorEvent event) {
      }

      @Override
      public void ancestorMoved(@Nonnull final AncestorEvent event) {
      }
    });

    this.editorPane.addCaretListener(new CaretListener() {
      @Override
      public void caretUpdate(@Nonnull final CaretEvent e) {
        updateCaretPos();
      }
    });

    this.wrapping = Wrapping.WORD_WRAP;
    editorPane.setCaretPosition(0);
    updateWrapping();
    
    this.editorPane.getDocument().addUndoableEditListener(this.undoManager);
    updateRedoUndoState();
    
    UiUtils.makeOwningDialogResizable(this);
  }

  private void updateCaretPos() {
    final int pos = this.editorPane.getCaretPosition();
    final int col = getColumn(pos, this.editorPane);
    final int row = getRow(pos, this.editorPane);
    this.labelCursorPos.setText(row + ":" + col); //NOI18N

    final String selectedText = this.editorPane.getSelectedText();
    if (StringUtils.isEmpty(selectedText)) {
      this.buttonCopy.setEnabled(false);
      this.buttonBrowse.setEnabled(false);
    } else {
      this.buttonCopy.setEnabled(true);
      try {
        final URI uri = URI.create(selectedText.trim());
        this.buttonBrowse.setEnabled(uri.isAbsolute());
      } catch (Exception ex) {
        this.buttonBrowse.setEnabled(false);
      }
    }
  }

  private static int getRow(final int pos, final JTextComponent editor) {
    int rn = (pos == 0) ? 1 : 0;
    try {
      int offs = pos;
      while (offs > 0) {
        offs = Utilities.getRowStart(editor, offs) - 1;
        rn++;
      }
    } catch (BadLocationException e) {
      LOGGER.error("Bad location", e); //NOI18N
    }
    return rn;
  }

  private static int getColumn(final int pos, final JTextComponent editor) {
    try {
      return pos - Utilities.getRowStart(editor, pos) + 1;
    } catch (BadLocationException e) {
      LOGGER.error("Bad location", e); //NOI18N
    }
    return -1;
  }

  @Nonnull
  public String getText() {
    return this.editorPane.getText();
  }

  public void dispose() {

  }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
   * Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jToolBar1 = new javax.swing.JToolBar();
    buttonUndo = new javax.swing.JButton();
    buttonRedo = new javax.swing.JButton();
    buttonImport = new javax.swing.JButton();
    buttonExport = new javax.swing.JButton();
    buttonCopy = new javax.swing.JButton();
    buttonPaste = new javax.swing.JButton();
    buttonBrowse = new javax.swing.JButton();
    buttonClear = new javax.swing.JButton();
    jPanel1 = new javax.swing.JPanel();
    labelCursorPos = new javax.swing.JLabel();
    jSeparator1 = new javax.swing.JSeparator();
    labelWrapMode = new javax.swing.JLabel();
    filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(16, 0), new java.awt.Dimension(16, 0), new java.awt.Dimension(16, 32767));
    jScrollPane2 = new javax.swing.JScrollPane();
    editorPane = new javax.swing.JTextArea();

    setLayout(new java.awt.BorderLayout());

    jToolBar1.setFloatable(false);
    jToolBar1.setRollover(true);

    buttonUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/undo.png"))); // NOI18N
    buttonUndo.setMnemonic('u');
    buttonUndo.setText("Undo");
    buttonUndo.setFocusable(false);
    buttonUndo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonUndo.setNextFocusableComponent(buttonRedo);
    buttonUndo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonUndo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonUndoActionPerformed(evt);
      }
    });
    jToolBar1.add(buttonUndo);

    buttonRedo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/redo.png"))); // NOI18N
    buttonRedo.setMnemonic('r');
    buttonRedo.setText("Redo");
    buttonRedo.setFocusable(false);
    buttonRedo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonRedo.setNextFocusableComponent(buttonImport);
    buttonRedo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonRedo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonRedoActionPerformed(evt);
      }
    });
    jToolBar1.add(buttonRedo);

    buttonImport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/disk16.png"))); // NOI18N
    buttonImport.setMnemonic('i');
    buttonImport.setText("Import");
    buttonImport.setToolTipText("Import text content from UTF8 encoded text file");
    buttonImport.setFocusable(false);
    buttonImport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonImport.setNextFocusableComponent(buttonExport);
    buttonImport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonImport.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonImportActionPerformed(evt);
      }
    });
    jToolBar1.add(buttonImport);

    buttonExport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/file_save16.png"))); // NOI18N
    buttonExport.setMnemonic('e');
    buttonExport.setText("Export");
    buttonExport.setToolTipText("Export text content into UTF8 encoded file");
    buttonExport.setFocusable(false);
    buttonExport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonExport.setNextFocusableComponent(buttonCopy);
    buttonExport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonExport.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonExportActionPerformed(evt);
      }
    });
    jToolBar1.add(buttonExport);

    buttonCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/page_copy16.png"))); // NOI18N
    buttonCopy.setMnemonic('c');
    buttonCopy.setText("Copy");
    buttonCopy.setToolTipText("Copy selected text content into clipboard");
    buttonCopy.setFocusable(false);
    buttonCopy.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonCopy.setNextFocusableComponent(buttonPaste);
    buttonCopy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonCopy.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonCopyActionPerformed(evt);
      }
    });
    jToolBar1.add(buttonCopy);

    buttonPaste.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/paste_plain16.png"))); // NOI18N
    buttonPaste.setMnemonic('p');
    buttonPaste.setText("Paste");
    buttonPaste.setToolTipText("Paste text content from clipboard into current position");
    buttonPaste.setFocusable(false);
    buttonPaste.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonPaste.setNextFocusableComponent(buttonBrowse);
    buttonPaste.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonPaste.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonPasteActionPerformed(evt);
      }
    });
    jToolBar1.add(buttonPaste);

    buttonBrowse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/link16.png"))); // NOI18N
    buttonBrowse.setMnemonic('b');
    buttonBrowse.setText("Browse");
    buttonBrowse.setToolTipText("Open selected link in browser");
    buttonBrowse.setFocusable(false);
    buttonBrowse.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonBrowse.setNextFocusableComponent(buttonClear);
    buttonBrowse.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonBrowse.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonBrowseActionPerformed(evt);
      }
    });
    jToolBar1.add(buttonBrowse);

    buttonClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cross16.png"))); // NOI18N
    buttonClear.setMnemonic('a');
    buttonClear.setText("Clear All");
    buttonClear.setToolTipText("Clear all text content");
    buttonClear.setFocusable(false);
    buttonClear.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonClear.setNextFocusableComponent(editorPane);
    buttonClear.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonClear.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonClearActionPerformed(evt);
      }
    });
    jToolBar1.add(buttonClear);

    add(jToolBar1, java.awt.BorderLayout.NORTH);

    jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

    labelCursorPos.setText("...:...");
    labelCursorPos.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        labelCursorPosMouseClicked(evt);
      }
    });
    jPanel1.add(labelCursorPos);

    jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
    jSeparator1.setPreferredSize(new java.awt.Dimension(8, 16));
    jPanel1.add(jSeparator1);

    labelWrapMode.setText("...");
    labelWrapMode.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    labelWrapMode.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        labelWrapModeMouseClicked(evt);
      }
    });
    jPanel1.add(labelWrapMode);
    jPanel1.add(filler1);

    add(jPanel1, java.awt.BorderLayout.PAGE_END);

    editorPane.setColumns(20);
    editorPane.setRows(5);
    editorPane.setNextFocusableComponent(buttonUndo);
    jScrollPane2.setViewportView(editorPane);

    add(jScrollPane2, java.awt.BorderLayout.CENTER);
  }// </editor-fold>//GEN-END:initComponents

  private void labelCursorPosMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelCursorPosMouseClicked

  }//GEN-LAST:event_labelCursorPosMouseClicked

  private void labelWrapModeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelWrapModeMouseClicked
    this.wrapping = this.wrapping.next();
    updateWrapping();
  }//GEN-LAST:event_labelWrapModeMouseClicked

  private void updateWrapping() {
    this.editorPane.setWrapStyleWord(this.wrapping != Wrapping.CHAR_WRAP);
    this.editorPane.setLineWrap(this.wrapping != Wrapping.NONE);
    updateBottomPanel();
  }

  private void buttonImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonImportActionPerformed
    final File toOpen = DialogProviderManager.getInstance().getDialogProvider().msgOpenFileDialog("note-editor", UiUtils.BUNDLE.getString("PlainTextEditor.buttonLoadActionPerformed.title"), null, true, TEXT_FILE_FILTER, "Open"); //NOI18N
    if (toOpen != null) {
      try {
        final String text = FileUtils.readFileToString(toOpen, "UTF-8"); //NOI18N
        this.editorPane.setText(text);
      } catch (Exception ex) {
        LOGGER.error("Error during text file loading", ex); //NOI18N
        DialogProviderManager.getInstance().getDialogProvider().msgError(UiUtils.BUNDLE.getString("PlainTextEditor.buttonLoadActionPerformed.msgError"));
      }
    }

  }//GEN-LAST:event_buttonImportActionPerformed

  private void buttonExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonExportActionPerformed
    final File toSave = DialogProviderManager.getInstance().getDialogProvider().msgSaveFileDialog("note-editor", UiUtils.BUNDLE.getString("PlainTextEditor.buttonSaveActionPerformed.saveTitle"), null, true, TEXT_FILE_FILTER, "Save"); //NOI18N
    if (toSave != null) {
      try {
        final String text = getText();
        FileUtils.writeStringToFile(toSave, text, "UTF-8"); //NOI18N
      } catch (Exception ex) {
        LOGGER.error("Error during text file saving", ex); //NOI18N
        DialogProviderManager.getInstance().getDialogProvider().msgError(UiUtils.BUNDLE.getString("PlainTextEditor.buttonSaveActionPerformed.msgError"));
      }
    }

  }//GEN-LAST:event_buttonExportActionPerformed

  private void buttonCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCopyActionPerformed
    StringSelection stringSelection = new StringSelection(this.editorPane.getSelectedText());
    final Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
    clpbrd.setContents(stringSelection, null);
  }//GEN-LAST:event_buttonCopyActionPerformed

  private void buttonPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPasteActionPerformed
    try {
      this.editorPane.replaceSelection((String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    } catch (UnsupportedFlavorException ex) {
      // no text data in clipboard
    } catch (IOException ex) {
      LOGGER.error("Error during paste from clipboard", ex); //NOI18N
    }
  }//GEN-LAST:event_buttonPasteActionPerformed

  private void buttonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearActionPerformed
    this.editorPane.setText(""); //NOI18N
  }//GEN-LAST:event_buttonClearActionPerformed

  private void buttonBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBrowseActionPerformed
    final String selectedText = this.editorPane.getSelectedText().trim();
    try {
      UiUtils.browseURI(URI.create(selectedText), false);
    } catch (Exception ex) {
      LOGGER.error("Can't open link : " + selectedText); //NOI18N
      DialogProviderManager.getInstance().getDialogProvider().msgError("Can't browse link : " + selectedText);
    }
  }//GEN-LAST:event_buttonBrowseActionPerformed

  private void buttonUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUndoActionPerformed
    doUndo();
  }//GEN-LAST:event_buttonUndoActionPerformed

  private void buttonRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRedoActionPerformed
    doRedo();
  }//GEN-LAST:event_buttonRedoActionPerformed

  private void updateBottomPanel() {
    this.labelWrapMode.setText("Wrap: " + this.wrapping.getDisplay());
  }


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonBrowse;
  private javax.swing.JButton buttonClear;
  private javax.swing.JButton buttonCopy;
  private javax.swing.JButton buttonExport;
  private javax.swing.JButton buttonImport;
  private javax.swing.JButton buttonPaste;
  private javax.swing.JButton buttonRedo;
  private javax.swing.JButton buttonUndo;
  private javax.swing.JTextArea editorPane;
  private javax.swing.Box.Filler filler1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JSeparator jSeparator1;
  private javax.swing.JToolBar jToolBar1;
  private javax.swing.JLabel labelCursorPos;
  private javax.swing.JLabel labelWrapMode;
  // End of variables declaration//GEN-END:variables
}
