/*
 * Copyright 2009 Weaver authors
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

package com.google.testing.threadtester;

/**
 * A task added to a {@link Script}. Each task should provide
 * a concrete implementation of the {@link #execute} method.
 *
 * @param <T> the type of the object-under-test. This is the
 * same type as that of the owning {@link Script} object.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public abstract class ScriptedTask<T> {

  private volatile Script<T> owner;

  /**
   * Executes this task. During execution, operations will typically
   * be carried out on the object-under-test. In addition, control
   * can be transferred to another script by calling {@link #releaseTo}.
   */
  public abstract void execute() throws Exception;

  /**
   * Releases control to another script. Releasing control will
   * pause the Script that owns this task, and allow the other
   * script to continue executing. This method should be invoked
   * during {#link execute}.
   */
  public void releaseTo(Script<T> other) {
    if (owner == null) {
      throw new IllegalStateException("Cannot release until started");
    }
    owner.releaseTo(other);
  }

  /**
   * Sets the script that owns this task.
   */
  void setOwner(Script<T> owner) {
    this.owner = owner;
  }
}
