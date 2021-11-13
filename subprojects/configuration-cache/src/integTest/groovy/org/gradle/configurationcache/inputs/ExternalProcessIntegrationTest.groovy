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

package org.gradle.configurationcache.inputs

import org.gradle.configurationcache.AbstractConfigurationCacheIntegrationTest
import org.gradle.test.fixtures.file.TestFile

class ExternalProcessIntegrationTest extends AbstractConfigurationCacheIntegrationTest {

    def setup() {
        writeExecutableScript(testDirectory)
        // Write a second copy of the script in the different directory for tests that use it as current dir.
        writeExecutableScript(testDirectory.createDir(pwd))
    }

    def "#title is intercepted in groovy build script"(VarInitializer varInitializer) {
        given:
        buildFile("""
        import org.codehaus.groovy.runtime.ProcessGroovyMethods
        import static org.codehaus.groovy.runtime.ProcessGroovyMethods.execute

        ${varInitializer.groovy}
        def process = $processCreator
        process.waitForProcessOutput(System.out, System.err)
        """)

        when:
        configurationCacheFails(":help")

        then:
        failure.assertOutputContains("PWD=${testDirectory.file(expectedPwdSuffix).path};\nFOOBAR=$expectedEnvVar;")
        problems.assertFailureHasProblems(failure) {
            withProblem("Build file 'build.gradle': external process started 'sh $executableScript'")
        }

        where:
        varInitializer     | processCreator                                                                        | expectedPwdSuffix | expectedEnvVar
        fromString()       | "command.execute()"                                                                   | ""                | ""
        fromGroovyString() | "command.execute()"                                                                   | ""                | ""
        fromStringArray()  | "command.execute()"                                                                   | ""                | ""
        fromStringList()   | "command.execute()"                                                                   | ""                | ""
        fromObjectList()   | "command.execute()"                                                                   | ""                | ""
        fromString()       | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromGroovyString() | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromStringArray()  | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromStringList()   | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromObjectList()   | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromString()       | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        fromGroovyString() | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        fromStringArray()  | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        fromStringList()   | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        fromObjectList()   | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        // Null argument handling
        fromString()       | "command.execute(null, null)"                                                         | ""                | ""
        fromString()       | "command.execute(['FOOBAR=foobar'], null)"                                            | ""                | "foobar"
        fromString()       | "command.execute(new String[] {'FOOBAR=foobar'}, null)"                               | ""                | "foobar"
        fromString()       | "command.execute(null, file('$pwd'))"                                                 | pwd               | ""
        // Typed nulls
        fromString()       | "command.execute((String[]) null, null)"                                              | ""                | ""
        fromString()       | "command.execute(null, (File) null)"                                                  | ""                | ""
        fromString()       | "command.execute((String[]) null, (File) null)"                                       | ""                | ""
        // type-wrapped arguments
        fromString()       | "command.execute((String[]) ['FOOBAR=foobar'], null)"                                 | ""                | "foobar"
        fromString()       | "command.execute((List) ['FOOBAR=foobar'], null)"                                     | ""                | "foobar"
        fromString()       | "command.execute(['FOOBAR=foobar'] as String[], null)"                                | ""                | "foobar"
        fromString()       | "command.execute(['FOOBAR=foobar'] as List, null)"                                    | ""                | "foobar"
        // null-safe call
        fromGroovyString() | "command?.execute()"                                                                  | ""                | ""

        // Direct ProcessGroovyMethods calls
        fromString()       | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromGroovyString() | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromStringArray()  | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromStringList()   | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromObjectList()   | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromString()       | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromGroovyString() | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromStringArray()  | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromStringList()   | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromObjectList()   | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        fromGroovyString() | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        fromStringArray()  | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        fromStringList()   | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        fromObjectList()   | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        // Null argument handling
        fromString()       | "ProcessGroovyMethods.execute(command, null, null)"                                   | ""                | ""
        fromString()       | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], null)"                      | ""                | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, null)"         | ""                | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, null, file('$pwd'))"                           | pwd               | ""
        // Typed nulls
        fromString()       | "ProcessGroovyMethods.execute(command, (String[]) null, null)"                        | ""                | ""
        fromString()       | "ProcessGroovyMethods.execute(command, null, (File) null)"                            | ""                | ""
        fromString()       | "ProcessGroovyMethods.execute(command, (String[]) null, (File) null)"                 | ""                | ""
        // type-wrapped arguments
        fromGroovyString() | "ProcessGroovyMethods.execute(command as String)"                                     | ""                | ""
        fromGroovyString() | "ProcessGroovyMethods.execute(command as String, null, null)"                         | ""                | ""
        fromString()       | "ProcessGroovyMethods.execute(command, (String[]) ['FOOBAR=foobar'], null)"           | ""                | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, (List) ['FOOBAR=foobar'], null)"               | ""                | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'] as String[], null)"          | ""                | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'] as List, null)"              | ""                | "foobar"

        // static import calls (are handled differently by the dynamic Groovy's codegen)
        fromString()       | "execute(command)"                                                                    | ""                | ""
        fromGroovyString() | "execute(command)"                                                                    | ""                | ""
        fromStringArray()  | "execute(command)"                                                                    | ""                | ""
        fromStringList()   | "execute(command)"                                                                    | ""                | ""
        fromObjectList()   | "execute(command)"                                                                    | ""                | ""
        fromString()       | "execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"                      | pwd               | "foobar"
        fromGroovyString() | "execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"                      | pwd               | "foobar"
        fromStringArray()  | "execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"                      | pwd               | "foobar"
        fromStringList()   | "execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"                      | pwd               | "foobar"
        fromObjectList()   | "execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"                      | pwd               | "foobar"
        fromString()       | "execute(command, ['FOOBAR=foobar'], file('$pwd'))"                                   | pwd               | "foobar"
        fromGroovyString() | "execute(command, ['FOOBAR=foobar'], file('$pwd'))"                                   | pwd               | "foobar"
        fromStringArray()  | "execute(command, ['FOOBAR=foobar'], file('$pwd'))"                                   | pwd               | "foobar"
        fromStringList()   | "execute(command, ['FOOBAR=foobar'], file('$pwd'))"                                   | pwd               | "foobar"
        fromObjectList()   | "execute(command, ['FOOBAR=foobar'], file('$pwd'))"                                   | pwd               | "foobar"
        // Null argument handling
        fromString()       | "execute(command, null, null)"                                                        | ""                | ""
        fromString()       | "execute(command, ['FOOBAR=foobar'], null)"                                           | ""                | "foobar"
        fromString()       | "execute(command, new String[] {'FOOBAR=foobar'}, null)"                              | ""                | "foobar"
        fromString()       | "execute(command, null, file('$pwd'))"                                                | pwd               | ""
        // Typed nulls
        fromString()       | "execute(command, (String[]) null, null)"                                             | ""                | ""
        fromString()       | "execute(command, null, (File) null)"                                                 | ""                | ""
        fromString()       | "execute(command, (String[]) null, (File) null)"                                      | ""                | ""
        // type-wrapped arguments
        fromGroovyString() | "execute(command as String)"                                                          | ""                | ""
        fromGroovyString() | "execute(command as String, null, null)"                                              | ""                | ""
        fromString()       | "execute(command, (String[]) ['FOOBAR=foobar'], null)"                                | ""                | "foobar"
        fromString()       | "execute(command, (List) ['FOOBAR=foobar'], null)"                                    | ""                | "foobar"
        fromString()       | "execute(command, ['FOOBAR=foobar'] as String[], null)"                               | ""                | "foobar"
        fromString()       | "execute(command, ['FOOBAR=foobar'] as List, null)"                                   | ""                | "foobar"

        // Runtime.exec() overloads
        fromString()       | "Runtime.getRuntime().exec(command)"                                                  | ""                | ""
        fromGroovyString() | "Runtime.getRuntime().exec(command)"                                                  | ""                | ""
        fromStringArray()  | "Runtime.getRuntime().exec(command)"                                                  | ""                | ""
        fromString()       | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"                  | ""                | "foobar"
        fromGroovyString() | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"                  | ""                | "foobar"
        fromStringArray()  | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"                  | ""                | "foobar"
        fromString()       | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"    | pwd               | "foobar"
        fromGroovyString() | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"    | pwd               | "foobar"
        fromStringArray()  | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"    | pwd               | "foobar"
        // Null argument handling
        fromString()       | "Runtime.getRuntime().exec(command, null)"                                            | ""                | ""
        fromString()       | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, null)"            | ""                | "foobar"
        fromString()       | "Runtime.getRuntime().exec(command, null, file('$pwd'))"                              | pwd               | ""
        fromString()       | "Runtime.getRuntime().exec(command, null, null)"                                      | ""                | ""
        // Typed nulls
        fromString()       | "Runtime.getRuntime().exec(command, null as String[])"                                | ""                | ""
        fromString()       | "Runtime.getRuntime().exec(command, null, null as File)"                              | ""                | ""
        // type-wrapped arguments
        fromGroovyString() | "Runtime.getRuntime().exec(command as String)"                                        | ""                | ""
        fromGroovyString() | "Runtime.getRuntime().exec(command as String, null)"                                  | ""                | ""
        fromGroovyString() | "Runtime.getRuntime().exec(command as String, null, null)"                            | ""                | ""
        fromObjectList()   | "Runtime.getRuntime().exec(command as String[])"                                      | ""                | ""
        fromObjectList()   | "Runtime.getRuntime().exec(command as String[], null)"                                | ""                | ""
        fromObjectList()   | "Runtime.getRuntime().exec(command as String[], null, null)"                          | ""                | ""
        // Null-safe calls
        fromString()       | "Runtime.getRuntime()?.exec(command)"                                                 | ""                | ""
        fromString()       | "Runtime.getRuntime()?.exec(command, new String[] {'FOOBAR=foobar'})"                 | ""                | "foobar"
        fromString()       | "Runtime.getRuntime()?.exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"   | pwd               | "foobar"

        // ProcessBuilder.start()
        fromStringArray()  | "new ProcessBuilder(command).start()"                                                 | ""                | ""
        fromStringList()   | "new ProcessBuilder(command).start()"                                                 | ""                | ""
        fromStringArray()  | "new ProcessBuilder(command)?.start()"                                                | ""                | ""

        title = processCreator.replace("command", varInitializer.description)
    }

    def "#title is intercepted in static groovy build script"(VarInitializer varInitializer) {
        given:
        buildFile("""
        import org.codehaus.groovy.runtime.ProcessGroovyMethods

        @groovy.transform.CompileStatic
        void runStuff() {
            ${varInitializer.groovy}
            def process = $processCreator
            process.waitForProcessOutput(System.out, System.err)
        }

        runStuff()
        """)

        when:
        configurationCacheFails(":help")

        then:
        failure.assertOutputContains("PWD=${testDirectory.file(expectedPwdSuffix).path};\nFOOBAR=$expectedEnvVar;")
        problems.assertFailureHasProblems(failure) {
            withProblem("Build file 'build.gradle': external process started 'sh $executableScript'")
        }

        where:
        varInitializer     | processCreator                                                                        | expectedPwdSuffix | expectedEnvVar
        fromString()       | "command.execute()"                                                                   | ""                | ""
        fromGroovyString() | "command.execute()"                                                                   | ""                | ""
        fromStringArray()  | "command.execute()"                                                                   | ""                | ""
        fromStringList()   | "command.execute()"                                                                   | ""                | ""
        fromObjectList()   | "command.execute()"                                                                   | ""                | ""
        fromString()       | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromGroovyString() | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromStringArray()  | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromStringList()   | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromObjectList()   | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"                       | pwd               | "foobar"
        fromString()       | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        fromGroovyString() | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        fromStringArray()  | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        fromStringList()   | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        fromObjectList()   | "command.execute(['FOOBAR=foobar'], file('$pwd'))"                                    | pwd               | "foobar"
        // Null argument handling
        fromString()       | "command.execute((List) null, null)"                                                  | ""                | ""
        fromString()       | "command.execute((String[]) null, null)"                                              | ""                | ""
        fromString()       | "command.execute((List) null, file('$pwd'))"                                          | pwd               | ""
        fromString()       | "command.execute((String[]) null, file('$pwd'))"                                      | pwd               | ""
        fromString()       | "command.execute(['FOOBAR=foobar'], null)"                                            | ""                | "foobar"
        fromString()       | "command.execute(new String[] {'FOOBAR=foobar'}, null)"                               | ""                | "foobar"

        // Direct ProcessGroovyMethods calls
        fromString()       | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromGroovyString() | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromStringArray()  | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromStringList()   | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromObjectList()   | "ProcessGroovyMethods.execute(command)"                                               | ""                | ""
        fromString()       | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromGroovyString() | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromStringArray()  | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromStringList()   | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromObjectList()   | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))" | pwd               | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        fromGroovyString() | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        fromStringArray()  | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        fromStringList()   | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        fromObjectList()   | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"              | pwd               | "foobar"
        // Null argument handling
        fromString()       | "ProcessGroovyMethods.execute(command, (List) null, null)"                            | ""                | ""
        fromString()       | "ProcessGroovyMethods.execute(command, (String[]) null, null)"                        | ""                | ""
        fromString()       | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], null)"                      | ""                | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, null)"         | ""                | "foobar"
        fromString()       | "ProcessGroovyMethods.execute(command, (List) null, file('$pwd'))"                    | pwd               | ""
        fromString()       | "ProcessGroovyMethods.execute(command, (String[]) null, file('$pwd'))"                | pwd               | ""

        // Runtime.exec() overloads
        fromString()       | "Runtime.getRuntime().exec(command)"                                                  | ""                | ""
        fromStringArray()  | "Runtime.getRuntime().exec(command)"                                                  | ""                | ""
        fromString()       | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"                  | ""                | "foobar"
        fromStringArray()  | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"                  | ""                | "foobar"
        fromString()       | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"    | pwd               | "foobar"
        fromStringArray()  | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"    | pwd               | "foobar"
        // Null argument handling
        fromString()       | "Runtime.getRuntime().exec(command, null)"                                            | ""                | ""
        fromString()       | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, null)"            | ""                | "foobar"
        fromString()       | "Runtime.getRuntime().exec(command, null, file('$pwd'))"                              | pwd               | ""
        fromString()       | "Runtime.getRuntime().exec(command, null, null)"                                      | ""                | ""

        // ProcessBuilder.start()
        fromStringArray()  | "new ProcessBuilder(command).start()"                                                 | ""                | ""
        fromStringList()   | "new ProcessBuilder(command).start()"                                                 | ""                | ""

        title = processCreator.replace("command", varInitializer.description)
    }

    def "#title is intercepted in kotlin build script"(VarInitializer varInitializer) {
        given:
        buildKotlinFile("""
        import java.io.OutputStream
        import org.codehaus.groovy.runtime.ProcessGroovyMethods

        ${varInitializer.kotlin}
        val process = $processCreator
        ProcessGroovyMethods.waitForProcessOutput(process, System.out as OutputStream, System.err as OutputStream)
        """)

        when:
        configurationCacheFails(":help")

        then:
        failure.assertOutputContains("PWD=${testDirectory.file(expectedPwdSuffix).path};\nFOOBAR=$expectedEnvVar;")
        problems.assertFailureHasProblems(failure) {
            withProblem("Build file 'build.gradle.kts': external process started 'sh $executableScript'")
        }

        where:
        varInitializer    | processCreator                                                                      | expectedPwdSuffix | expectedEnvVar
        // Direct ProcessGroovyMethods calls
        fromString()      | "ProcessGroovyMethods.execute(command)"                                             | ""                | ""
        fromStringArray() | "ProcessGroovyMethods.execute(command)"                                             | ""                | ""
        fromStringList()  | "ProcessGroovyMethods.execute(command)"                                             | ""                | ""
        fromObjectList()  | "ProcessGroovyMethods.execute(command)"                                             | ""                | ""
        fromString()      | "ProcessGroovyMethods.execute(command, arrayOf(\"FOOBAR=foobar\"), file(\"$pwd\"))" | pwd               | "foobar"
        fromStringArray() | "ProcessGroovyMethods.execute(command, arrayOf(\"FOOBAR=foobar\"), file(\"$pwd\"))" | pwd               | "foobar"
        fromStringList()  | "ProcessGroovyMethods.execute(command, arrayOf(\"FOOBAR=foobar\"), file(\"$pwd\"))" | pwd               | "foobar"
        fromObjectList()  | "ProcessGroovyMethods.execute(command, arrayOf(\"FOOBAR=foobar\"), file(\"$pwd\"))" | pwd               | "foobar"
        fromString()      | "ProcessGroovyMethods.execute(command, listOf(\"FOOBAR=foobar\"), file(\"$pwd\"))"  | pwd               | "foobar"
        fromStringArray() | "ProcessGroovyMethods.execute(command, listOf(\"FOOBAR=foobar\"), file(\"$pwd\"))"  | pwd               | "foobar"
        fromStringList()  | "ProcessGroovyMethods.execute(command, listOf(\"FOOBAR=foobar\"), file(\"$pwd\"))"  | pwd               | "foobar"
        fromObjectList()  | "ProcessGroovyMethods.execute(command, listOf(\"FOOBAR=foobar\"), file(\"$pwd\"))"  | pwd               | "foobar"
        // Null argument handling
        fromString()      | "ProcessGroovyMethods.execute(command, null as List<*>?, null)"                     | ""                | ""
        fromString()      | "ProcessGroovyMethods.execute(command, null as Array<String>?, null)"               | ""                | ""
        fromString()      | "ProcessGroovyMethods.execute(command, listOf(\"FOOBAR=foobar\"), null)"            | ""                | "foobar"
        fromString()      | "ProcessGroovyMethods.execute(command, arrayOf(\"FOOBAR=foobar\"), null)"           | ""                | "foobar"
        fromString()      | "ProcessGroovyMethods.execute(command, null as List<*>?, file(\"$pwd\"))"           | pwd               | ""
        fromString()      | "ProcessGroovyMethods.execute(command, null as Array<String>?, file(\"$pwd\"))"     | pwd               | ""

        // Runtime.exec() overloads
        fromString()      | "Runtime.getRuntime().exec(command)"                                                | ""                | ""
        fromStringArray() | "Runtime.getRuntime().exec(command)"                                                | ""                | ""
        fromString()      | "Runtime.getRuntime().exec(command, arrayOf(\"FOOBAR=foobar\"))"                    | ""                | "foobar"
        fromStringArray() | "Runtime.getRuntime().exec(command, arrayOf(\"FOOBAR=foobar\"))"                    | ""                | "foobar"
        fromString()      | "Runtime.getRuntime().exec(command, arrayOf(\"FOOBAR=foobar\"), file(\"$pwd\"))"    | pwd               | "foobar"
        fromStringArray() | "Runtime.getRuntime().exec(command, arrayOf(\"FOOBAR=foobar\"), file(\"$pwd\"))"    | pwd               | "foobar"
        // Null argument handling
        fromString()      | "Runtime.getRuntime().exec(command, null)"                                          | ""                | ""
        fromString()      | "Runtime.getRuntime().exec(command, arrayOf(\"FOOBAR=foobar\"), null)"              | ""                | "foobar"
        fromString()      | "Runtime.getRuntime().exec(command, null, file(\"$pwd\"))"                          | pwd               | ""
        fromString()      | "Runtime.getRuntime().exec(command, null, null)"                                    | ""                | ""

        // ProcessBuilder.start()
        fromStringArray() | "ProcessBuilder(*command).start()"                                                  | ""                | ""
        fromStringList()  | "ProcessBuilder(command).start()"                                                   | ""                | ""

        title = processCreator.replace("command", varInitializer.description)
    }

    def "#title is intercepted in java build code"(VarInitializer varInitializer) {
        given:
        file("buildSrc/src/main/java/SneakyPlugin.java") << """
        import org.gradle.api.*;
        import java.io.*;
        import java.util.*;
        import org.codehaus.groovy.runtime.ProcessGroovyMethods;

        public class SneakyPlugin implements Plugin<Project> {
            @Override
            public void apply(Project project) {
                try {
                    ${varInitializer.java}
                    Process process = $processCreator;
                    ProcessGroovyMethods.waitForProcessOutput(process, (OutputStream) System.out, (OutputStream) System.err);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        """
        buildFile("""
            apply plugin: SneakyPlugin
        """)

        when:
        configurationCacheFails(":help")

        then:
        failure.assertOutputContains("PWD=${testDirectory.file(expectedPwdSuffix).path};\nFOOBAR=$expectedEnvVar;")
        problems.assertFailureHasProblems(failure) {
            withProblem("Plugin class 'SneakyPlugin': external process started 'sh $executableScript'")
        }

        where:
        varInitializer    | processCreator                                                                                    | expectedPwdSuffix | expectedEnvVar
        // Direct ProcessGroovyMethods calls
        fromString()      | "ProcessGroovyMethods.execute(command)"                                                           | ""                | ""
        fromStringArray() | "ProcessGroovyMethods.execute(command)"                                                           | ""                | ""
        fromStringList()  | "ProcessGroovyMethods.execute(command)"                                                           | ""                | ""
        fromObjectList()  | "ProcessGroovyMethods.execute(command)"                                                           | ""                | ""
        fromString()      | "ProcessGroovyMethods.execute(command, new String[] {\"FOOBAR=foobar\"}, project.file(\"$pwd\"))" | pwd               | "foobar"
        fromStringArray() | "ProcessGroovyMethods.execute(command, new String[] {\"FOOBAR=foobar\"}, project.file(\"$pwd\"))" | pwd               | "foobar"
        fromStringList()  | "ProcessGroovyMethods.execute(command, new String[] {\"FOOBAR=foobar\"}, project.file(\"$pwd\"))" | pwd               | "foobar"
        fromObjectList()  | "ProcessGroovyMethods.execute(command, new String[] {\"FOOBAR=foobar\"}, project.file(\"$pwd\"))" | pwd               | "foobar"
        fromString()      | "ProcessGroovyMethods.execute(command, Arrays.asList(\"FOOBAR=foobar\"), project.file(\"$pwd\"))" | pwd               | "foobar"
        fromStringArray() | "ProcessGroovyMethods.execute(command, Arrays.asList(\"FOOBAR=foobar\"), project.file(\"$pwd\"))" | pwd               | "foobar"
        fromStringList()  | "ProcessGroovyMethods.execute(command, Arrays.asList(\"FOOBAR=foobar\"), project.file(\"$pwd\"))" | pwd               | "foobar"
        fromObjectList()  | "ProcessGroovyMethods.execute(command, Arrays.asList(\"FOOBAR=foobar\"), project.file(\"$pwd\"))" | pwd               | "foobar"
        // Null argument handling
        fromString()      | "ProcessGroovyMethods.execute(command, (List) null, null)"                                        | ""                | ""
        fromString()      | "ProcessGroovyMethods.execute(command, (String[]) null, null)"                                    | ""                | ""
        fromString()      | "ProcessGroovyMethods.execute(command, Arrays.asList(\"FOOBAR=foobar\"), null)"                   | ""                | "foobar"
        fromString()      | "ProcessGroovyMethods.execute(command, new String[] {\"FOOBAR=foobar\"}, null)"                   | ""                | "foobar"
        fromString()      | "ProcessGroovyMethods.execute(command, (List) null, project.file(\"$pwd\"))"                      | pwd               | ""
        fromString()      | "ProcessGroovyMethods.execute(command, (String[]) null, project.file(\"$pwd\"))"                  | pwd               | ""

        // Runtime.exec() overloads
        fromString()      | "Runtime.getRuntime().exec(command)"                                                              | ""                | ""
        fromStringArray() | "Runtime.getRuntime().exec(command)"                                                              | ""                | ""
        fromString()      | "Runtime.getRuntime().exec(command, new String[] {\"FOOBAR=foobar\"})"                            | ""                | "foobar"
        fromStringArray() | "Runtime.getRuntime().exec(command, new String[] {\"FOOBAR=foobar\"})"                            | ""                | "foobar"
        fromString()      | "Runtime.getRuntime().exec(command, new String[] {\"FOOBAR=foobar\"}, project.file(\"$pwd\"))"    | pwd               | "foobar"
        fromStringArray() | "Runtime.getRuntime().exec(command, new String[] {\"FOOBAR=foobar\"}, project.file(\"$pwd\"))"    | pwd               | "foobar"
        // Null argument handling
        fromString()      | "Runtime.getRuntime().exec(command, null)"                                                        | ""                | ""
        fromString()      | "Runtime.getRuntime().exec(command, new String[] {\"FOOBAR=foobar\"}, null)"                      | ""                | "foobar"
        fromString()      | "Runtime.getRuntime().exec(command, null, project.file(\"$pwd\"))"                                | pwd               | ""
        fromString()      | "Runtime.getRuntime().exec(command, null, null)"                                                  | ""                | ""

        // ProcessBuilder.start()
        fromStringArray() | "new ProcessBuilder(command).start()"                                                             | ""                | ""
        fromStringList()  | "new ProcessBuilder(command).start()"                                                             | ""                | ""

        title = processCreator.replace("command", varInitializer.description)
    }

    def "running #title in task is not a problem for dynamic build script"() {
        def configurationCache = newConfigurationCacheFixture()
        given:
        buildFile("""
        import org.codehaus.groovy.runtime.ProcessGroovyMethods
        import static org.codehaus.groovy.runtime.ProcessGroovyMethods.execute

        tasks.register("runCommand") {
            doLast {
                ${varInitializer.groovy}
                def process = $processCreator
                process.waitForProcessOutput(System.out, System.err)
            }
        }
        """)

        when:
        configurationCacheRun(":runCommand")

        then:
        configurationCache.assertStateStored()

        where:
        varInitializer    | processCreator
        fromString()      | "command.execute()"
        fromString()      | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"
        fromString()      | "command.execute(['FOOBAR=foobar'], file('$pwd'))"

        // Direct ProcessGroovyMethods calls
        fromString()      | "ProcessGroovyMethods.execute(command)"
        fromString()      | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"
        fromString()      | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"

        // static import calls (are handled differently by the dynamic Groovy's codegen)
        fromString()      | "execute(command)"
        fromString()      | "execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"
        fromString()      | "execute(command, ['FOOBAR=foobar'], file('$pwd'))"

        // Runtime.exec() overloads
        fromString()      | "Runtime.getRuntime().exec(command)"
        fromStringArray() | "Runtime.getRuntime().exec(command)"
        fromString()      | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"
        fromStringArray() | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"
        fromString()      | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"
        fromStringArray() | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"

        // ProcessBuilder.start()
        fromStringArray() | "new ProcessBuilder(command).start()"
        fromStringList()  | "new ProcessBuilder(command).start()"

        title = processCreator.replace("command", varInitializer.description)
    }

    def "running #title in task is not a problem for static build script"() {
        def configurationCache = newConfigurationCacheFixture()
        given:
        buildFile("""
        import org.codehaus.groovy.runtime.ProcessGroovyMethods
        import static org.codehaus.groovy.runtime.ProcessGroovyMethods.execute

        @groovy.transform.CompileStatic
        def taskAction() {
            ${varInitializer.groovy}
            def process = $processCreator
            process.waitForProcessOutput(System.out, System.err)
        }

        tasks.register("runCommand") {
            doLast {
                taskAction()
            }
        }
        """)

        when:
        configurationCacheRun(":runCommand")

        then:
        configurationCache.assertStateStored()

        where:
        varInitializer    | processCreator
        fromString()      | "command.execute()"
        fromString()      | "command.execute(new String[] {'FOOBAR=foobar'}, file('$pwd'))"
        fromString()      | "command.execute(['FOOBAR=foobar'], file('$pwd'))"

        // Direct ProcessGroovyMethods calls
        fromString()      | "ProcessGroovyMethods.execute(command)"
        fromString()      | "ProcessGroovyMethods.execute(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"
        fromString()      | "ProcessGroovyMethods.execute(command, ['FOOBAR=foobar'], file('$pwd'))"

        // Runtime.exec() overloads
        fromString()      | "Runtime.getRuntime().exec(command)"
        fromStringArray() | "Runtime.getRuntime().exec(command)"
        fromString()      | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"
        fromStringArray() | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'})"
        fromString()      | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"
        fromStringArray() | "Runtime.getRuntime().exec(command, new String[] {'FOOBAR=foobar'}, file('$pwd'))"

        // ProcessBuilder.start()
        fromStringArray() | "new ProcessBuilder(command).start()"
        fromStringList()  | "new ProcessBuilder(command).start()"

        title = processCreator.replace("command", varInitializer.description)
    }

    private static String getExecutableScript() {
        return "exec.sh"
    }

    private static String getPwd() {
        return "tmp"
    }

    private static void writeExecutableScript(TestFile root) {
        TestFile scriptFile = root.file(executableScript)
        scriptFile << '''
            echo PWD=$PWD\\;
            echo FOOBAR=$FOOBAR\\;
        '''
    }

    private abstract static class VarInitializer {
        final String description

        VarInitializer(String description) {
            this.description = description
        }

        String getGroovy() {
            throw new UnsupportedOperationException()
        }

        String getJava() {
            throw new UnsupportedOperationException()
        }

        String getKotlin() {
            throw new UnsupportedOperationException()
        }

        @Override
        String toString() {
            return description
        }
    }

    static VarInitializer fromString() {
        return new VarInitializer("String") {
            @Override
            String getGroovy() {
                return """String command = 'sh $executableScript'"""
            }

            @Override
            String getJava() {
                return """String command = "sh $executableScript";"""
            }

            @Override
            String getKotlin() {
                return """val command = "sh $executableScript" """
            }
        }
    }

    static VarInitializer fromGroovyString() {
        return new VarInitializer("GString") {
            @Override
            String getGroovy() {
                return """
                        String rawCommand = 'sh $executableScript'
                        def command = "\${rawCommand.toString()}"
                    """
            }
        }
    }

    static VarInitializer fromStringArray() {
        return new VarInitializer("String[]") {
            @Override
            String getGroovy() {
                return """String[] command = ['sh', '$executableScript']"""
            }

            @Override
            String getJava() {
                return """String[] command = new String[] {"sh", "$executableScript"};"""
            }

            @Override
            String getKotlin() {
                return """val command = arrayOf("sh", "$executableScript") """
            }
        }
    }

    static VarInitializer fromStringList() {
        return new VarInitializer("List<String>") {
            @Override
            String getGroovy() {
                return """
                        def command = ['sh', '$executableScript']
                    """
            }

            @Override
            String getJava() {
                return """
                    List<String> command = Arrays.asList("sh", "$executableScript");
                """
            }

            @Override
            String getKotlin() {
                return """
                    val command = listOf("sh", "$executableScript")
                """
            }
        }
    }

    static VarInitializer fromObjectList() {
        return new VarInitializer("List<Object>") {
            @Override
            String getGroovy() {
                return """
                    def rawScript = '$executableScript'
                    def command = ['sh', "\${rawScript.toString()}"]
                    """
            }

            @Override
            String getJava() {
                return """
                    Object rawScript = new Object() {
                        public String toString() {
                            return "$executableScript";
                        }
                    };
                    List<Object> command = Arrays.<Object>asList("sh", rawScript);
                    """
            }

            @Override
            String getKotlin() {
                return """
                    val rawScript = object : Any() {
                        override fun toString(): String = "$executableScript"
                    }
                    val command = listOf<Any>("sh", rawScript)
                    """
            }
        }
    }
}
