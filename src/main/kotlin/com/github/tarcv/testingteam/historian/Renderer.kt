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

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache
import com.github.jknack.handlebars.helper.ConditionalHelpers
import com.github.jknack.handlebars.helper.StringHelpers
import com.github.jknack.handlebars.io.FileTemplateLoader
import com.github.jknack.handlebars.io.TemplateLoader
import java.io.File

fun createRenderer(templateDir: File): Renderer {
    val loader: TemplateLoader = FileTemplateLoader(templateDir).apply {
        suffix = ""
    }
    val cache = ConcurrentMapTemplateCache()
    return Handlebars(loader)
        .with(cache)
        .apply {
            registerEnumHelpers(this, ConditionalHelpers.values())
            registerEnumHelpers(this, StringHelpers.values())
        }
        .let { Renderer(it) }
}

class Renderer(private val handlebars: Handlebars) {
    fun compileStringTemplate(template: String): CompiledTemplate {
        return CompiledTemplate(handlebars.compileInline(template))
    }

    fun compileFileTemplate(relativeFile: File): CompiledTemplate {
        return CompiledTemplate(handlebars.compile(relativeFile.path))
    }

    inner class CompiledTemplate(private val template: Template) {
        fun renderToString(context: Any): String {
            return template.apply(context)
        }

        fun renderToFile(outputFile: File, context: Any) {
            outputFile
                .bufferedWriter(Charsets.UTF_8)
                .use { writer ->
                    template.apply(context, writer)
                }
        }
    }
}

private fun <T> registerEnumHelper(handlebars: Handlebars, helper: T): Handlebars
        where T: Enum<*>, T: Helper<*>
{
    return handlebars.registerHelper(helper.name, helper as Helper<*>)
}

private fun <T> registerEnumHelpers(handlebars: Handlebars, helpers: Array<T>)
        where T: Enum<*>, T: Helper<*>
{
    for (helper in helpers) {
        registerEnumHelper(handlebars, helper)
    }
}
