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
import org.gradle.internal.concurrent.Stoppable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultExceptionCollector implements ExceptionCollector, Stoppable {

    private final List<Exception> exceptions = new CopyOnWriteArrayList<>();

    @Override
    public void addException(Exception e) {
        exceptions.add(e);
    }

    @Override
    public List<Exception> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

    @Override
    public void stop() {
        exceptions.clear();
    }

    @Override
    public <T> Action<T> decorate(Action<T> action) {
        return new ExceptionCollectingAction<>(action);
    }

    @Override
    public <T> Closure<T> decorate(Closure<T> closure) {
        return new ExceptionCollectingClosure<>(closure);
    }

    private class ExceptionCollectingAction<T> implements Action<T> {

        private final Action<T> delegate;

        private ExceptionCollectingAction(Action<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(final T arg) {
            try {
                delegate.execute(arg);
            } catch (Exception e) {
                DefaultExceptionCollector.this.addException(e);
            }
        }
    }

    private class ExceptionCollectingClosure<T> extends Closure<T> {

        private final Closure<T> delegate;

        public ExceptionCollectingClosure(Closure<T> delegate) {
            super(delegate.getOwner(), delegate.getThisObject());
            this.delegate = delegate;
        }

        @SuppressWarnings("unused")
        public void doCall(final Object... args) {
            try {
                int numClosureArgs = delegate.getMaximumNumberOfParameters();
                Object[] finalArgs = numClosureArgs < args.length ? Arrays.copyOf(args, numClosureArgs) : args;
                delegate.call(finalArgs);
            } catch (Exception e) {
                DefaultExceptionCollector.this.addException(e);
            }
        }

        @Override
        public void setDelegate(Object delegateObject) {
            delegate.setDelegate(delegateObject);
        }

        @Override
        public void setResolveStrategy(int resolveStrategy) {
            delegate.setResolveStrategy(resolveStrategy);
        }

        @Override
        public int getMaximumNumberOfParameters() {
            return delegate.getMaximumNumberOfParameters();
        }
    }
}
