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
package org.eclipse.che.api.git;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link GitUrlUtils}.
 *
 * @author Igor Vinokur
 */
public class GitUrlUtilsTest {

    @Test
    public void shouldReturnTrueIfGivenUrlIsSsh() throws Exception {
        assertTrue(GitUrlUtils.isSSH("ssh://user@host.xz:port/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://user@host.xz/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://host.xz:port/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://host.xz/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://user@host.xz/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://host.xz/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://user@host.xz/~user/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://host.xz/~user/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://user@host.xz/~/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("ssh://host.xz/~/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("user@host.xz:/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("user@host.xz:path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("git://host.xz/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("git://host.xz/~user/path/to/repo.git"));
        assertTrue(GitUrlUtils.isSSH("git@vcsProvider.com:user/test.git"));
        assertTrue(GitUrlUtils.isSSH("ssh@vcsProvider.com:user/test.git"));
    }

    @Test
    public void shouldReturnFalseIfGivenUrlIsNotSsh() throws Exception {
        assertFalse(GitUrlUtils.isSSH("http://host.xz/path/to/repo.git"));
        assertFalse(GitUrlUtils.isSSH("https://host.xz/path/to/repo.git"));
    }
}
