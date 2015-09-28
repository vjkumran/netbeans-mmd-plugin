/*
 * Copyright 2015 Igor Maznitsa.
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
package com.igormaznitsa.nbmindmap.nb.swing;

import com.igormaznitsa.mindmap.model.ExtraTopic;
import com.igormaznitsa.mindmap.model.MindMap;
import com.igormaznitsa.mindmap.model.Topic;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public final class MindMapTreePanel extends javax.swing.JPanel {

  private static final long serialVersionUID = 2652308291444091807L;

  private final MindMapTreeCellRenderer cellRenderer = new MindMapTreeCellRenderer();

  public MindMapTreePanel(final MindMap map, final ExtraTopic selectedTopicUid, final boolean expandAll, final ActionListener listener) {
    initComponents();
    this.treeMindMap.setCellRenderer(this.cellRenderer);
    this.treeMindMap.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    if (map != null) {
      this.treeMindMap.setModel(map);
      if (selectedTopicUid != null) {
        final Topic topic = map.findTopicForLink(selectedTopicUid);
        if (topic != null) {
          this.treeMindMap.setSelectionPath(new TreePath(topic.getPath()));
        }
      }
    }

    this.treeMindMap.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(final MouseEvent e) {
        if (!e.isPopupTrigger() && e.getClickCount() > 1) {
          if (listener != null) {
            listener.actionPerformed(new ActionEvent(this, 0, "doubleClick"));
          }
        }
      }
    });

    
    this.setPreferredSize(new Dimension(450, 400));
    
    if (expandAll){
      expandAll();
    }
  }

  public void expandAll(){
    for (int i = 0; i < this.treeMindMap.getRowCount(); i++) {
      this.treeMindMap.expandRow(i);
    }
  }
  
  public void collapseAll(){
    for (int i = 0; i < this.treeMindMap.getRowCount(); i++) {
      this.treeMindMap.collapseRow(i);
    }
  }
  
  public JTree getTree() {
    return this.treeMindMap;
  }

  public Topic getSelectedTopic() {
    final TreePath selected = this.treeMindMap.getSelectionPath();
    return selected == null ? null : (Topic) selected.getLastPathComponent();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    treeScrollPane = new javax.swing.JScrollPane();
    treeMindMap = new javax.swing.JTree();
    toolBar = new javax.swing.JToolBar();
    buttonExpandAll = new javax.swing.JButton();
    buttonCollapseAll = new javax.swing.JButton();
    buttonUnselect = new javax.swing.JButton();

    setLayout(new java.awt.BorderLayout());

    treeScrollPane.setViewportView(treeMindMap);

    add(treeScrollPane, java.awt.BorderLayout.CENTER);

    toolBar.setFloatable(false);
    toolBar.setRollover(true);

    buttonExpandAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/nbmindmap/icons/toggle_expand16.png"))); // NOI18N
    java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/igormaznitsa/nbmindmap/i18n/Bundle"); // NOI18N
    buttonExpandAll.setText(bundle.getString("MindMapTreePanel.buttonExpandAll.text")); // NOI18N
    buttonExpandAll.setFocusable(false);
    buttonExpandAll.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonExpandAll.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonExpandAll.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonExpandAllActionPerformed(evt);
      }
    });
    toolBar.add(buttonExpandAll);

    buttonCollapseAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/nbmindmap/icons/toggle16.png"))); // NOI18N
    buttonCollapseAll.setText(bundle.getString("MindMapTreePanel.buttonCollapseAll.text")); // NOI18N
    buttonCollapseAll.setFocusable(false);
    buttonCollapseAll.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonCollapseAll.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonCollapseAll.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonCollapseAllActionPerformed(evt);
      }
    });
    toolBar.add(buttonCollapseAll);

    buttonUnselect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/nbmindmap/icons/select16.png"))); // NOI18N
    buttonUnselect.setText(bundle.getString("MindMapTreePanel.buttonUnselect.text")); // NOI18N
    buttonUnselect.setFocusable(false);
    buttonUnselect.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonUnselect.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    buttonUnselect.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonUnselectActionPerformed(evt);
      }
    });
    toolBar.add(buttonUnselect);

    add(toolBar, java.awt.BorderLayout.PAGE_START);
  }// </editor-fold>//GEN-END:initComponents

  private void buttonUnselectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUnselectActionPerformed
    this.treeMindMap.setSelectionPath(null);
  }//GEN-LAST:event_buttonUnselectActionPerformed

  private void buttonExpandAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonExpandAllActionPerformed
    expandAll();
  }//GEN-LAST:event_buttonExpandAllActionPerformed

  private void buttonCollapseAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCollapseAllActionPerformed
    collapseAll();
  }//GEN-LAST:event_buttonCollapseAllActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonCollapseAll;
  private javax.swing.JButton buttonExpandAll;
  private javax.swing.JButton buttonUnselect;
  private javax.swing.JToolBar toolBar;
  private javax.swing.JTree treeMindMap;
  private javax.swing.JScrollPane treeScrollPane;
  // End of variables declaration//GEN-END:variables
}
