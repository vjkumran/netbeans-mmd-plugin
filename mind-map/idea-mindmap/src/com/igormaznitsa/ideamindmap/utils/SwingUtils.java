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
package com.igormaznitsa.ideamindmap.utils;

import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;

public final class SwingUtils {
  private SwingUtils() {
  }

  public static void assertSwingThread() {
    if (!SwingUtilities.isEventDispatchThread())
      throw new Error("Must be Swing event dispatching thread, but detected '" + Thread.currentThread().getName() + '\'');
  }

  public static void safeSwing(@NotNull final Runnable runnable) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    }
    else {
      SwingUtilities.invokeLater(runnable);
    }
  }
}