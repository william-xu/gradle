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

package org.gradle.internal.service.scopes;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.internal.service.ServiceRegistration;

import java.util.Collections;
import java.util.List;

public class ToolingServices extends AbstractPluginServiceRegistry {

    private static final ExceptionCollector NOOP_COLLECTOR = new ExceptionCollector() {

        @Override
        public List<Exception> getExceptions() {
            return Collections.emptyList();
        }

        @Override
        public void addException(Exception e) {
        }

        @Override
        public <T> Action<T> decorate(Action<T> action) {
            return action;
        }

        @Override
        public <T> Closure<T> decorate(Closure<T> closure) {
            return closure;
        }

        @Override
        public void stop() {
        }
    };

    @Override
    public void registerBuildServices(ServiceRegistration registration) {
        registration.addProvider(new ExceptionServices());
    }

    static class ExceptionServices {
        public ExceptionCollector createExceptionCollector() {
            String classpathMode = System.getProperty("org.gradle.kotlin.dsl.provider.mode");
            boolean expressionsSuppressed = classpathMode != null && classpathMode.contains("classpath");
            return expressionsSuppressed ? new DefaultExceptionCollector() : NOOP_COLLECTOR;
        }
    }
}
