/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.parts.ide;

import com.google.inject.Inject;

import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.plugin.parts.ide.helloworldview.HelloWorldViewAction;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_MAIN_MENU;

@Extension(title = "Hello World View Extension")
public class MyExtension {

    @Inject
    public MyExtension(ActionManager actionManager, HelloWorldViewAction action){
        actionManager.registerAction("helloWorldViewAction",action);
        DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(GROUP_MAIN_MENU);

        DefaultActionGroup sampleActionGroup = new DefaultActionGroup("Sample Action", true, actionManager);
        sampleActionGroup.add(action);


        mainMenu.add(sampleActionGroup);
    }
}
