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
package org.eclipse.che.api.workspace.server.env.impl.che;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.workspace.server.env.EnvironmentManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceRuntimeImpl;
import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent;

import javax.annotation.PreDestroy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static java.lang.String.format;

/**
 * author Alexander Garagatyi
 */
public class CheEnvironmentManager implements EnvironmentManager {
    private final Map<String, Queue<MachineConfigImpl>> startQueues;
    private final MachineManager                        machineManager;

    public CheEnvironmentManager(MachineManager machineManager) {
        this.machineManager = machineManager;
        this.startQueues = new HashMap<>();
    }

    /**
    * Dev-machine always starts before the other machines.
    * If dev-machine start failed then method will throw appropriate
    * {@link ServerException}.
     * * <p>If {@link #stop} method executed after dev machine is started but
     * another machines haven't been started yet then {@link ConflictException}
     * will be thrown and start process will be interrupted.
     * */

    /**
     * <p>Stops all running machines one by one,
     * non-dev machines first. During the stop of the workspace
     * its runtime is accessible with {@link WorkspaceStatus#STOPPING stopping} status.
     * Workspace may be stopped only if its status is {@link WorkspaceStatus#RUNNING}.
     *
     * <p>If workspace has runtime with dev-machine running
     * and other machines starting then the runtime can still
     * be stopped which will also interrupt starting process.
     *
     * <p>Note that it doesn't provide any events for machines stop,
     * Machine API is responsible for it.
     */




    public void start(String workspaceId, Environment env, boolean recover) {
        // Dev machine goes first in the start queue
        final List<? extends MachineConfig> machineConfigs = env.getMachineConfigs();
        final MachineConfigImpl devCfg = rmFirst(machineConfigs, MachineConfig::isDev);
        machineConfigs.add(0, devCfg);
        startQueues.put(workspace.getId(), new ArrayDeque<>(machineConfigs));
        startQueue(workspace.getId(), activeEnv.getName(), recover);
    }

    public void stop(String workspaceId) {
// remove the workspace from the queue to prevent start
        // of another not started machines(if such exist)
        startQueues.remove(workspaceId);
        destroyRuntime(workspaceId, runtime);
    }

    public void get() {

    }

    public void startMachine() {

    }

    /**
     * Stops workspace by destroying all its machines and removing it from in memory storage.
     */
    // todo can we add machine to running environment? with compose
    //
    private void destroyRuntime(String wsId, WorkspaceRuntimeImpl workspace) throws NotFoundException, ServerException {
        publishEvent(WorkspaceStatusEvent.EventType.STOPPING, wsId, null);
        final List<MachineImpl> machines = new ArrayList<>(workspace.getMachines());
        final MachineImpl devMachine = rmFirst(machines, m -> m.getConfig().isDev());
        // destroying all non-dev machines
        for (MachineImpl machine : machines) {
            try {
                machineManager.destroy(machine.getId(), false);
            } catch (NotFoundException ignore) {
                // it is ok, machine has been already destroyed
            } catch (RuntimeException | MachineException ex) {
                LOG.error(format("Could not destroy machine '%s' of workspace '%s'",
                                 machine.getId(),
                                 machine.getWorkspaceId()),
                          ex);
            }
        }
        // destroying dev-machine
        try {
            machineManager.destroy(devMachine.getId(), false);
            publishEvent(WorkspaceStatusEvent.EventType.STOPPED, wsId, null);
        } catch (NotFoundException ignore) {
            // it is ok, machine has been already destroyed
        } catch (RuntimeException | ServerException ex) {
            publishEvent(WorkspaceStatusEvent.EventType.ERROR, wsId, ex.getLocalizedMessage());
            throw ex;
        } finally {
            removeRuntime(wsId);
        }
    }

    private void startQueue(String wsId, String envName, boolean recover) throws ServerException,
                                                                                 NotFoundException,
                                                                                 ConflictException {
        publishEvent(WorkspaceStatusEvent.EventType.STARTING, wsId, null);
        MachineConfigImpl config = getPeekConfig(wsId);
        while (config != null) {
            startMachine(config, wsId, envName, recover);
            config = getPeekConfig(wsId);
        }

        // Clean up the start queue when all the machines successfully started
        rwLock.writeLock().lock();
        try {
            final Queue<MachineConfigImpl> queue = startQueues.get(wsId);
            if (queue != null && queue.isEmpty()) {
                startQueues.remove(wsId);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private MachineConfigImpl getPeekConfig(String wsId) throws ConflictException, ServerException {
        // Trying to get machine to start. If queue doesn't exist then workspace
        // start was interrupted either by the stop method, or by the cleanup
        rwLock.readLock().lock();
        try {
            ensurePreDestroyIsNotExecuted();
            final Queue<MachineConfigImpl> queue = startQueues.get(wsId);
            if (queue == null) {
                throw new ConflictException(format("Workspace '%s' start interrupted. " +
                                                   "Workspace was stopped before all its machines were started",
                                                   wsId));
            }
            return queue.peek();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void startMachine(MachineConfigImpl config,
                              String wsId,
                              String envName,
                              boolean recover) throws ServerException,
                                                      NotFoundException,
                                                      ConflictException {
        // Trying to start machine from the given configuration
        MachineImpl machine = null;
        try {
            machine = createMachine(config, wsId, envName, recover);
        } catch (RuntimeException | MachineException | NotFoundException | SnapshotException | ConflictException ex) {
            if (config.isDev()) {
                publishEvent(WorkspaceStatusEvent.EventType.ERROR, wsId, ex.getLocalizedMessage());
                cleanupStartResources(wsId);
                throw ex;
            }
            LOG.error(format("Error while creating non-dev machine '%s' in workspace '%s', environment '%s'",
                             config.getName(),
                             wsId,
                             envName),
                      ex);
        }

        // Machine destroying is an expensive operation which must be
        // performed outside of the lock, this section checks if
        // the workspace wasn't stopped while it is starting and sets
        // polled flag to true if the workspace wasn't stopped plus
        // polls the proceeded machine configuration from the queue
        boolean queuePolled = false;
        rwLock.readLock().lock();
        try {
            ensurePreDestroyIsNotExecuted();
            final Queue<MachineConfigImpl> queue = startQueues.get(wsId);
            if (queue != null) {
                queue.poll();
                queuePolled = true;
                if (machine != null) {
                    final WorkspaceRuntimeImpl runtime = descriptors.get(wsId).getRuntime();
                    if (config.isDev()) {
                        runtime.setDevMachine(machine);
                        publishEvent(WorkspaceStatusEvent.EventType.RUNNING, wsId, null);
                    }
                    runtime.getMachines().add(machine);
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }

        // If machine config is not polled from the queue
        // then stop method was executed and the machine which
        // has been just created must be destroyed
        if (!queuePolled) {
            if (machine != null) {
                machineManager.destroy(machine.getId(), false);
            }
            throw new ConflictException(format("Workspace '%s' start interrupted. " +
                                               "Workspace was stopped before all its machines were started",
                                               wsId));
        }
    }

    /**
     * Creates or recovers machine based on machine config.
     */
    private MachineImpl createMachine(MachineConfig machine,
                                      String workspaceId,
                                      String envName,
                                      boolean recover) throws MachineException,
                                                              SnapshotException,
                                                              NotFoundException,
                                                              ConflictException {
        try {
            if (recover) {
                return machineManager.recoverMachine(machine, workspaceId, envName);
            } else {
                return machineManager.createMachineSync(machine, workspaceId, envName);
            }
        } catch (BadRequestException brEx) {
            // TODO fix this in machineManager
            throw new IllegalArgumentException(brEx.getLocalizedMessage(), brEx);
        }
    }

    /**
     * Removes all descriptors from the in-memory storage, while
     * {@link MachineManager#cleanup()} is responsible for machines destroying.
     */
    @PreDestroy
    @VisibleForTesting
    void cleanup() {
        isPreDestroyInvoked = true;
        rwLock.writeLock().lock();
        try {
            startQueues.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @VisibleForTesting
    void cleanupStartResources(String workspaceId) {
        rwLock.writeLock().lock();
        try {
            descriptors.remove(workspaceId);
            startQueues.remove(workspaceId);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private <T> T rmFirst(List<? extends T> elements, Predicate<T> predicate) {
        T element = null;
        for (final Iterator<? extends T> it = elements.iterator(); it.hasNext() && element == null; ) {
            final T next = it.next();
            if (predicate.apply(next)) {
                element = next;
                it.remove();
            }
        }
        return element;
    }
}
