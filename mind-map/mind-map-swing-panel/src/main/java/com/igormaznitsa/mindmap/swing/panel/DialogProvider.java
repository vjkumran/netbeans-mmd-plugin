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
package com.igormaznitsa.mindmap.swing.panel;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;

public interface DialogProvider {
  void msgError(@Nonnull String text);
  void msgInfo(@Nonnull String text);
  void msgWarn(@Nonnull String text);
  boolean msgConfirmOkCancel(@Nonnull String title, @Nonnull String question);
  boolean msgOkCancel(@Nonnull String title, @Nonnull JComponent component);
  boolean msgConfirmYesNo(@Nonnull String title, @Nonnull String question);
  @Nullable
  Boolean msgConfirmYesNoCancel(@Nonnull String title, @Nonnull final String question);
  @Nullable
  File msgSaveFileDialog(@Nonnull String id, @Nonnull String title, @Nullable File defaultFolder, boolean filesOnly, @Nonnull FileFilter fileFilter, @Nonnull String approveButtonText);
  @Nullable
  File msgOpenFileDialog(@Nonnull String id, @Nonnull String title, @Nullable File defaultFolder, boolean filesOnly, @Nonnull FileFilter fileFilter, @Nonnull String approveButtonText);
}
