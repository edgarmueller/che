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
package org.eclipse.che.plugin.sampleactions.ide.action;

import com.google.inject.Inject;

import org.eclipse.che.ide.newresource.AbstractNewResourceAction;
import org.eclipse.che.plugin.sampleactions.ide.SampleActionsResources;

/**
 * Simple action that creates an empty file with an "my" extension.
 */
public class CreateMyFileAction extends AbstractNewResourceAction {


    /**
     * Creates new action.
     */
    @Inject
    public CreateMyFileAction(SampleActionsResources resources) {
        super("Create my file", "Create a new file", resources.icon());
    }

    @Override
    protected String getExtension() {
        return "my";
    }
}
