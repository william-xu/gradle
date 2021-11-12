/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.tooling.internal.provider;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.execution.internal.FileChangeListener;
import org.gradle.api.execution.internal.TaskInputsListener;
import org.gradle.api.internal.TaskInternal;
import org.gradle.initialization.BuildCancellationToken;
import org.gradle.initialization.ContinuousExecutionGate;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.file.FileHierarchySet;
import org.gradle.internal.filewatch.PendingChangesListener;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.logging.text.StyledTextOutput;
import org.gradle.internal.watch.registry.FileWatcherRegistry;
import org.gradle.internal.watch.registry.impl.Combiners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.gradle.internal.filewatch.DefaultFileSystemChangeWaiterFactory.QUIET_PERIOD_SYSPROP;

public class FileSystemChangeListener implements FileChangeListener, TaskInputsListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemChangeListener.class);

    private final PendingChangesListener pendingChangesListener;
    private final BuildCancellationToken cancellationToken;
    private final ContinuousExecutionGate continuousExecutionGate;
    private final BlockingQueue<String> pendingChanges = new LinkedBlockingQueue<>(1);
    private final FileEventCollector fileEventCollector = new FileEventCollector();
    private final long quietPeriod;
    private volatile FileHierarchySet inputs = FileHierarchySet.empty();
    private volatile long lastChangeAt = monotonicClockMillis();

    public FileSystemChangeListener(PendingChangesListener pendingChangesListener, BuildCancellationToken cancellationToken, ContinuousExecutionGate continuousExecutionGate) {
        this.pendingChangesListener = pendingChangesListener;
        this.cancellationToken = cancellationToken;
        this.continuousExecutionGate = continuousExecutionGate;
        this.quietPeriod = Long.getLong(QUIET_PERIOD_SYSPROP, 250L);
    }

    public boolean hasAnyInputs() {
        return inputs != FileHierarchySet.empty();
    }

    void wait(Runnable notifier) {
        Runnable cancellationHandler = () -> pendingChanges.offer("Build cancelled");
        if (cancellationToken.isCancellationRequested()) {
            return;
        }
        try {
            cancellationToken.addCallback(cancellationHandler);
            notifier.run();
            String pendingChange = pendingChanges.take();
            LOGGER.info("Received pending change: {}", pendingChange);
            while (!cancellationToken.isCancellationRequested()) {
                long now = monotonicClockMillis();
                long remainingQuietPeriod = quietPeriod - (now - lastChangeAt);
                if (remainingQuietPeriod <= 0) {
                    break;
                }
                pendingChanges.poll(remainingQuietPeriod, TimeUnit.MILLISECONDS);
            }
            if (!cancellationToken.isCancellationRequested()) {
                continuousExecutionGate.waitForOpen();
            }
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        } finally {
            cancellationToken.removeCallback(cancellationHandler);
        }
    }

    public void reportChanges(StyledTextOutput logger) {
        fileEventCollector.reportChanges(logger);
    }

    @Override
    public void handleChange(FileWatcherRegistry.Type type, Path path) {
        String absolutePath = path.toString();
        lastChangeAt = monotonicClockMillis();
        if (inputs.contains(absolutePath)) {
            // got a change, store it
            fileEventCollector.onChange(type, path);
            if (pendingChanges.offer(absolutePath)) {
                pendingChangesListener.onPendingChanges();
            }
        }
    }

    @Override
    public void stopWatchingAfterError() {
        fileEventCollector.errorWhenWatching();
        if (pendingChanges.offer("Error watching files")) {
            pendingChangesListener.onPendingChanges();
        }
    }

    @Override
    public synchronized void onExecute(TaskInternal task, ImmutableMap<String, CurrentFileCollectionFingerprint> fingerprints) {
        this.inputs = fingerprints.values().stream()
            .flatMap(fingerprint -> fingerprint.getRootHashes().keySet().stream())
            .reduce(inputs, FileHierarchySet::plus, Combiners.nonCombining());
    }

    private static long monotonicClockMillis() {
        return System.nanoTime() / 1000000L;
    }

    private static class FileEventCollector {
        private static final int SHOW_INDIVIDUAL_CHANGES_LIMIT = 3;
        private final Map<Path, FileWatcherRegistry.Type> aggregatedEvents = new LinkedHashMap<>();
        private int moreChangesCount;
        private boolean errorWhenWatching;


        public void onChange(FileWatcherRegistry.Type type, Path path) {
            FileWatcherRegistry.Type existingEvent = aggregatedEvents.get(path);
            if (existingEvent == type ||
                (existingEvent == FileWatcherRegistry.Type.CREATED && type == FileWatcherRegistry.Type.MODIFIED)) {
                return;
            }

            if (existingEvent != null || aggregatedEvents.size() < SHOW_INDIVIDUAL_CHANGES_LIMIT) {
                aggregatedEvents.put(path, type);
            } else {
                moreChangesCount++;
            }
        }

        public void errorWhenWatching() {
            errorWhenWatching = true;
        }

        public void reportChanges(StyledTextOutput logger) {
            for (Map.Entry<Path, FileWatcherRegistry.Type> entry : aggregatedEvents.entrySet()) {
                FileWatcherRegistry.Type type = entry.getValue();
                Path path = entry.getKey();
                showIndividualChange(logger, path, type);
            }
            if (moreChangesCount > 0) {
                logOutput(logger, "and some more changes");
            }
            if (errorWhenWatching) {
                logOutput(logger, "Error when watching files - triggering a rebuild");
            }
        }

        private void showIndividualChange(StyledTextOutput logger, Path path, FileWatcherRegistry.Type changeType) {
            String changeDescription;
            switch (changeType) {
                case CREATED:
                    changeDescription = "new " + (Files.isDirectory(path) ? "directory" : "file");
                    break;
                case REMOVED:
                    changeDescription = "deleted";
                    break;
                case MODIFIED:
                default:
                    changeDescription = "modified";
            }
            logOutput(logger, "%s: %s", changeDescription, path.toString());
        }

        private void logOutput(StyledTextOutput logger, String message, Object... objects) {
            logger.formatln(message, objects);
        }
    }
}
