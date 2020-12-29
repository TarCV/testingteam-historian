/*
 *   Copyright TarCV 2021
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
package com.github.tarcv.testingteam.historian.reporters

import com.github.tarcv.testingteam.historian.FullResult
import com.github.tarcv.testingteam.historian.Result
import com.github.tarcv.testingteam.historian.Status
import com.github.tarcv.testingteam.historian.diagnosing
import dev.nohus.autokonfig.types.StringSetting
import java.io.File
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.streams.toList

class PoolComparativeReporter(private val context: ReporterContext) : Reporter {
    private val runLinkTemplate by lazy {
        val template by StringSetting(name = "runLinkTemplate").diagnosing()
        context.renderer.compileStringTemplate(template)
    }
    private val resultLinkTemplate by lazy {
        val template by StringSetting(name = "resultLinkTemplate").diagnosing()
        context.renderer.compileStringTemplate(template)
    }
    private val reportTemplate by lazy {
        context.renderer.compileFileTemplate(File("report.hbs"))
    }

    private fun prepareForReport(list: List<FullResult>): Report {
        val allRuns = getAllRuns(list)
        return list
            .groupBy { it.executorPool }
            .map { poolEntry ->
                val testReports = poolEntry.value
                    .groupBy { it.testName }
                    .map { testEntry ->
                        val testRuns = getAllRuns(testEntry.value)
                        val testResults = testEntry.value.map {
                            ResultReport(
                                it,
                                runLinkTemplate.renderToString(it),
                                resultLinkTemplate.renderToString(it)
                            )
                        }
                        val missingRuns = allRuns - testRuns
                        val blankRuns = missingRuns.map {
                            val result = FullResult(
                                it,
                                poolEntry.key,
                                "",
                                testEntry.key,
                                Status.NO_EXECUTION,
                                Paths.get(".")
                            )
                            ResultReport(
                                result,
                                runLinkTemplate.renderToString(result),
                                ""
                            )
                        }

                        TestReport(
                            testEntry.key,
                            (testResults + blankRuns).sortedWith(Comparator { o1, o2 ->
                                val p1 = o1.run?.split(humanSortSplitter) ?: emptyList()
                                val p2 = o2.run?.split(humanSortSplitter) ?: emptyList()
                                p1.zip(p2).onEach { (r1, r2) ->
                                    val n1 = r1.toIntOrNull()
                                    val n2 = r2.toIntOrNull()
                                    val result = if (n1 != null && n2 != null) {
                                        n1.compareTo(n2)
                                    } else {
                                        r1.compareTo(r2)
                                    }
                                    if (result != 0) {
                                        return@Comparator result
                                    }
                                }
                                p1.size.compareTo(p2.size)
                            })
                        )
                    } // TODO: order: 1st - length of the current failure streak, 2st - failure percentage, 3rd - least number of actual executions

                PoolReport(
                    poolEntry.key ?: "",
                    testReports
                ) // TODO: order as reports above
            }
            .let { Report(it) }
    }

    override fun render(list: Stream<FullResult>, outputFile: File) {
        render(list.toList(), outputFile)
    }

    fun render(list: List<FullResult>, outputFile: File) {
        val context = prepareForReport(list)
        reportTemplate.renderToFile(outputFile, context)
    }

    private fun getAllRuns(list: List<FullResult>) = list
        .map { it.run }
        .toSet()

    companion object {
        private val humanSortSplitter = Regex("""(?=\d)(?<=\D)|(?=\D)(?<=\d)""")
    }

    data class Report(val pools: List<PoolReport>)
    data class PoolReport(
        val pool: String,
        val results: List<TestReport>
    )

    data class TestReport(
        val testName: String,
        val results: List<ResultReport>
    )

    class ResultReport(
        result: FullResult,
        val runLink: String,
        val resultLink: String
    ) : Result by result
}