/*
 * Copyright 2015 Shekhar Jha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jhash.oimadmin.ui.component;

import com.jhash.oimadmin.Config;
import com.jhash.oimadmin.events.EventListenerRegistrar;
import com.jhash.oimadmin.ui.DisplayArea;
import com.jhash.oimadmin.ui.componentTree.UIComponentTree;
import com.jhash.oimadmin.ui.menu.MenuHandler;

public interface ParentComponent<T extends ParentComponent> extends EventListenerRegistrar<T> {

    Config.Configuration getConfiguration();

    UIComponentTree getUIComponentTree();

    DisplayArea getDisplayArea();

    MenuHandler getMenuHandler();

}
