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
package com.github.tarcv.testingteam.historian

import com.github.tarcv.testingteam.historian.extractors.ResultExtractor
import com.github.tarcv.testingteam.historian.reporters.Reporter
import com.github.tarcv.testingteam.historian.reporters.ReporterContext
import dev.nohus.autokonfig.AutoKonfig
import dev.nohus.autokonfig.types.ListSetting
import dev.nohus.autokonfig.withCommandLineArguments
import java.io.File
import java.nio.file.Files

object Historian {
    @JvmStatic
    fun main(args: Array<String>) {
        AutoKonfig.withCommandLineArguments(args)

        val inputReportsDir by DirectorySetting()
        val inputReportsRegex by RegexSetting(default = Regex(".*"))

        val outputFile by FileSetting()
        val templateDir by DirectorySetting(File("html"))
        val renderer = createRenderer(templateDir)

        val extractors by ListSetting(
            type = instanceSettingType<ResultExtractor>(
                "com.github.tarcv.testingteam.historian.extractors", emptyArray())
        ).diagnosing()
        val reporter by InstanceSetting<Reporter>(
            "com.github.tarcv.testingteam.historian.reporters",
            arrayOf(ReporterContext(renderer))
        )

        val results = FileScanner(inputReportsRegex, extractors)
            .read(inputReportsDir)

        outputFile.toPath().parent?.let {
            Files.createDirectories(it)
        }
        reporter.render(results, outputFile)
    }
}