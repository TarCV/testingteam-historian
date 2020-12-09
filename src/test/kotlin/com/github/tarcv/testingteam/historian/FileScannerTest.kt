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
package com.github.tarcv.testingteam.historian

import org.junit.Test
import java.nio.file.Paths
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.random.Random
import kotlin.test.asserter

class FileScannerTest {
    @Test
    fun test() {
        // ARRANGE:
        val seed = System.currentTimeMillis().also { println(it) }
        val random = Random(seed)

        val results = ConcurrentLinkedQueue<PartialResult>() // avoid interfering with concurrent streams in FileScanner
        val index = AtomicInteger()
        fun extractor(it: PartialResult): List<FullResult> {
            results.add(it)
            val count = index.incrementAndGet()
            val extracted = FullResult(count.toString(), "", "", "", Status.PASSED, it.path)
            return MutableList(count) { extracted }
                .plus(FullResult("", "", "", "", Status.NO_EXECUTION, it.path))
                .shuffled(random)
        }

        val inputPaths = listOf(
            "17/foo/bar/emulators/jkl.xml",
            "17/foo/bar/devices/jkl.xml",
            "17/foobar/devices/jkl.xml",
            "19/foo/bar/devices/jkl.xml"
        )

        // ACT:
        val output = FileScanner(
            pathRegex = Regex("(?<run>[^/]+)/foo/bar/(?<suite>[^/]+)/.*"),
            dataExtractors = listOf(
                { null },
                ::extractor
            )
        ).read(inputPaths)
            .collect(Collectors.toList())

        // ASSERT:
        asserter.assertEquals(null,
            listOf(
                PartialResult("17", "emulators", null, null, null, Paths.get(inputPaths[0]).normalize()),
                PartialResult("17", "devices", null, null, null, Paths.get(inputPaths[1]).normalize()),
                PartialResult("19", "devices", null, null, null, Paths.get(inputPaths[3]).normalize())
            ).sortedBy { it.toString() },
            results.sortedBy { it.toString() }
        )

        output
            .groupBy { it.run }
            .map { it.value }

            .also {
                asserter.assertNotEquals("different lists produces from extractors are kept",
                    it[0].size, it[1].size)
            }
            .also { groups ->
                asserter.assertTrue("NO_EXECUTION results are not kept",
                    groups.all { group ->
                        group.none { it.status == Status.NO_EXECUTION }
                    }
                )
            }
            .map { group ->
                group
                    .shuffled(random)
                    .also {
                        if (it.size > 1) {
                            asserter.assertEquals("got expected results", it[0], it[1])
                        }
                    }
                    .let { group[0] }
            }
            .let { group ->
                group.forEach {
                    asserter.assertEquals(null, "", it.executorPool)
                    asserter.assertEquals(null, "", it.executor)
                    asserter.assertEquals(null, "", it.testName)
                    asserter.assertEquals(null, Status.PASSED, it.status)
                }

                asserter.assertEquals(null,
                    listOf(
                        Paths.get(inputPaths[0]).normalize(),
                        Paths.get(inputPaths[1]).normalize(),
                        Paths.get(inputPaths[3]).normalize()
                    ).sortedBy { it.toString() },
                    group.map { it.path }.sortedBy { it.toString() }
                )
            }
    }
}