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
package org.eclipse.che.plugin.parts.ide.helloworldview;

import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.base.BaseView;

@Singleton
public class HelloWorldViewImpl extends BaseView<HelloWorldView.ActionDelegate> implements HelloWorldView {

    @Inject
    public HelloWorldViewImpl(PartStackUIResources resources){
        super(resources);
        Label label = new Label("Hello World");
        setContentWidget(label);
    }


}
