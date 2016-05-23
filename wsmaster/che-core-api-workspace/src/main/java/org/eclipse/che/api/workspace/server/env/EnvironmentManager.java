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
package org.eclipse.che.api.workspace.server.env;

import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.workspace.server.env.spi.EnvironmentImplManager;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * author Alexander Garagatyi
 */
public class EnvironmentManager {
    private final Map<String, EnvironmentImplManager> envManagers;

    @Inject
    public EnvironmentManager(Set<EnvironmentImplManager> envManagers) {
        HashMap<String, EnvironmentImplManager> managersMap = new HashMap<>();
        for (EnvironmentImplManager envManager : envManagers) {
            for (String envType : envManager.getSupportedTypes()) {
                if (managersMap.containsKey(envType)) {
                    throw new RuntimeException("EnvironmentImplManagers [" +
                                               envManager.getClass() +
                                               ", " +
                                               managersMap.get(envType).getClass() +
                                               "] are mapped to the same environment type " +
                                               envType);
                }
                managersMap.put(envType, envManager);
            }
        }
        this.envManagers = Collections.unmodifiableMap(managersMap);
    }

    public Set<String> getSupportedTypes() {
        return envManagers.keySet();
    }

    public void start(String workspaceId, Environment env, boolean recover) {

    }

    public void stop(String workspaceId) {

    }

    public List<Machine> get(String workspaceId) {

    }

    public void startMachine() {

    }
}
