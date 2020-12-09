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

import com.github.tarcv.testingteam.historian.extractors.ResultExtractor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream
import kotlin.reflect.KProperty1

class FileScanner(private val pathRegex: Regex, private val dataExtractors: List<ResultExtractor>) {
    private fun parsePath(basePath: Path, path: Path): PartialResult? {
        val normalizedPath = basePath
            .normalize()
            .relativize(path)

        return normalizedPath
            .toFile().invariantSeparatorsPath
            .let {
                pathRegex.matchEntire(it) ?: return null
            }
            .let { it.groups as MatchNamedGroupCollection }
            .let{ groups ->
                val status = groups.getByName(Result::status)?.let {
                    Status.valueOf(it.value.toUpperCase(Locale.ROOT))
                }

                PartialResult(
                    run = groups.getByName(Result::run)?.value,
                    executorPool = groups.getByName(Result::executorPool)?.value,
                    executor = groups.getByName(Result::executor)?.value,
                    testName = groups.getByName(Result::testName)?.value,
                    status = status,
                    path = path
                )
            }
    }

    private fun MatchNamedGroupCollection.getByName(name: KProperty1<Result, *>): MatchGroup? = try {
        this[name.name]
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun extractResults(result: PartialResult): Stream<FullResult> {
        for (it in dataExtractors) {
            val fullResult = it(result)
            if (fullResult != null) {
                return fullResult
                    .stream()
                    .filter { it.status != Status.NO_EXECUTION }
            }
        }
        throw RuntimeException("Not data extractor found for ${result.path}")
    }

    private fun read(basePath: Path, paths: Stream<Path>): Stream<FullResult> {
        return paths
            .map { this.parsePath(basePath, it) }
            .filterNonNull()
            .flatMap(this::extractResults)
    }

    internal fun read(paths: Collection<String>): Stream<FullResult> {
        return paths.parallelStream()
            .map { Paths.get(it) }
            .let { read(Paths.get("."), it) }
    }

    fun read(workingDir: File): Stream<FullResult> {
        val basePath = workingDir.toPath()
        return read(basePath, Files.walk(basePath))
    }
}

fun <T> Stream<T?>.filterNonNull(): Stream<T> {
    return this
        .filter { it != null }
        .map {
            @Suppress("UNCHECKED_CAST")
            it as T
        }
}

interface Result {
    val run: String?
    val executorPool: String?
    val executor: String?
    val testName: String?
    val status: Status?
    val path: Path
}

data class PartialResult(
    override val run: String?,
    override val executorPool: String?,
    override val executor: String?,
    override val testName: String?,
    override val status: Status?, // TODO

    override val path: Path
) : Result

data class FullResult(
    override val run: String,
    override val executorPool: String?,
    override val executor: String,
    override val testName: String,
    override val status: Status,

    override val path: Path
) : Result

enum class Status {
    PASSED,
    PASSED_WITH_WARNING,
    FAILED,
    ERRORED,
    NO_EXECUTION
}