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
package org.eclipse.che.plugin.filetype.ide;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;

@Extension(title = "My Extension")
public class MyExtension {

    @Inject
    private void registerFileType(
            final FileTypeRegistry fileTypeRegistry,
            final @Named("MyFileType") FileType myFileType) {
        fileTypeRegistry.registerFileType(myFileType);
    }
}
