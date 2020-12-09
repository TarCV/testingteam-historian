/*
 *   Copyright TarCV 2020
 *   This file is part of TestingTeam-Historian.
 *
 *   TestingTeam-Historian is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   TestingTeam-Historian is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with TestingTeam-Historian.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.tarcv.testingteam.historian.extractors

import com.github.tarcv.testingteam.historian.FullResult
import com.github.tarcv.testingteam.historian.PartialResult
import com.github.tarcv.testingteam.historian.Status
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.serialization.*

object XUnit: ResultExtractor {
    override fun invoke(result: PartialResult): List<FullResult> {
        val xml = XML {
            unknownChildHandler = object : UnknownChildHandler {
                override fun invoke(input: XmlReader, inputKind: InputKind, name: QName?, candidates: Collection<Any>) {
                    // anything unknown is allowed, but print a warning when it's really unknown
                    @Suppress("RedundantCompanionReference")
                    val expectedUnknowns = input.localName.let {
                        when {
                            it == "testsuites" -> TestSuites.Companion.ignoredFields
                            it == "testsuite" -> TestSuites.TestSuite.Companion.ignoredFields
                            it == "testcase" -> TestSuites.TestSuite.TestCase.Companion.ignoredFields
                            it.contains("failure") || it.contains("error") || it.contains("skipped") ->
                                TestSuites.TestSuite.TestCase.Exception.Companion.ignoredFields
                            else -> emptySet()
                        }
                    }
                    if (name?.localPart !in expectedUnknowns) {
                        System.err.println("Unknown $inputKind $name in ${input.localName} (expected: $candidates)")
                    }
                }
            }
        }
        val reportFile = result.path.toFile()
        if (!reportFile.isFile) {
            return emptyList()
        }

        val content = reportFile.readText()
        val xunit: TestSuites = try {
            xml.decodeFromString(content)
        } catch (e: XmlException) {
            TestSuites(listOf(xml.decodeFromString(content)))
        }
        return xunit.testsuites
            .flatMap { suite -> suite.testCases.map { Pair(suite, it) } }
            .map { (suite, testCase) ->
                val errors = testCase.error.size + testCase.flakyError.size + testCase.rerunError.size
                val failures = testCase.failure.size + testCase.flakyFailure.size + testCase.rerunFailure.size
                val skips = testCase.skipped.size
                val totalFailureCountProperty = suite.properties
                    .firstOrNull { it.name.equals("totalFailureCount", ignoreCase = true) }
                    ?.value
                    ?.toIntOrNull() ?: 0
                val status = when {
                    errors > 0 -> Status.ERRORED
                    failures > 0 -> Status.FAILED
                    skips > 0 -> Status.NO_EXECUTION
                    totalFailureCountProperty > 0 -> Status.PASSED_WITH_WARNING
                    else -> Status.PASSED
                }
                FullResult(
                    run = requireNotNull(result.run),
                    executorPool = result.executorPool,
                    executor = result.executor ?: suite.hostname,
                    testName = "${testCase.classname}#${testCase.name}",
                    status = status,
                    path = result.path
                )
            }
    }

    @Serializable
    @XmlSerialName("testsuites", namespace = "", prefix = "")
    data class TestSuites(
        @XmlChildrenName("testsuite", namespace = "", prefix = "")
        val testsuites: List<TestSuite>
    ) {
        companion object {
            val ignoredFields = setOf("name", "time", "tests", "failures", "errors")
        }

        @Serializable
        @XmlSerialName("testsuite", namespace = "", prefix = "")
        data class TestSuite(
            val hostname: String,

            @XmlSerialName("properties", namespace = "", prefix = "")
            @XmlChildrenName("property", namespace = "", prefix = "")
            val properties: List<PropertyPair>,

            val testCases: List<TestCase>
        ) {
            companion object {
                val ignoredFields = setOf(
                    "name", "tests", "failures", "errors", "skipped",
                    "time", "timestamp", "hostname", "package", "id",
                    "system-out", "system-err", "group", "file", "log",
                    "url", "version"
                )
            }

            @Serializable
            data class PropertyPair(
                val name: String,
                val value: String
            )

            @Serializable
            @XmlSerialName("testcase", namespace = "", prefix = "")
            data class TestCase(
                val name: String,
                val classname: String,

                @XmlSerialName("failure", namespace = "", prefix = "")
                val failure: List<Exception>,

                @XmlSerialName("error", namespace = "", prefix = "")
                val error: List<Exception>,

                @XmlSerialName("skipped", namespace = "", prefix = "")
                val skipped: List<Exception>,

                @XmlSerialName("rerunFailure", namespace = "", prefix = "")
                val rerunFailure: List<Exception>,

                @XmlSerialName("rerunError", namespace = "", prefix = "")
                val rerunError: List<Exception>,

                @XmlSerialName("flakyFailure", namespace = "", prefix = "")
                val flakyFailure: List<Exception>,

                @XmlSerialName("flakyError", namespace = "", prefix = "")
                val flakyError: List<Exception>,
            ) {
                companion object {
                    val ignoredFields = setOf("time", "group", "system-out", "system-err")
                }

                @Serializable
                data class Exception(
                    @XmlValue(true)
                    val content: String?
                ) {
                    companion object {
                        val ignoredFields = setOf("message", "type")
                    }
                }
            }
        }
    }
}