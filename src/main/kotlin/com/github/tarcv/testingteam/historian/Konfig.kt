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

import dev.nohus.autokonfig.AutoKonfig
import dev.nohus.autokonfig.getKeySource
import dev.nohus.autokonfig.types.SettingType
import dev.nohus.autokonfig.types.StringSettingType
import dev.nohus.autokonfig.withMap
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private val mainKonfig = AutoKonfig.withMap(emptyMap())
    .also {
        assert(it === AutoKonfig.withMap(emptyMap()))
    }

fun DirectorySetting(default: File? = null): DiagnosingSettingProvider<File> {
    return mainKonfig.customSetting(SettingType { value ->
        File(StringSettingType.transform(value))
            .also {
                if (!it.isDirectory) {
                    throw IllegalArgumentException("$value is not a directory")
                }
            }
    }, default = default)
}

fun FileSetting(default: File? = null): DiagnosingSettingProvider<File> {
    return mainKonfig.customSetting(
        SettingType { File(StringSettingType.transform(it)) },
        default = default
    )
}

fun RegexSetting(default: Regex? = null): DiagnosingSettingProvider<Regex> {
    return mainKonfig.customSetting(SettingType { value ->
        Regex(StringSettingType.transform(value))
    }, default = default)
}

inline fun <reified T> InstanceSetting(defaultPackage: String, ctorArguments: Array<Any>, default: T? = null)
: DiagnosingSettingProvider<T> {
    return InstanceSetting(T::class.java, defaultPackage, ctorArguments, default)
}
fun <T> InstanceSetting(type: Class<T>, defaultPackage: String, ctorArguments: Array<Any>, default: T? = null)
: DiagnosingSettingProvider<T> {
    return mainKonfig.customSetting(instanceSettingType(type, defaultPackage, ctorArguments), default = default)
}

inline fun <reified T> instanceSettingType(defaultPackage: String, ctorArguments: Array<Any>): SettingType<T> {
    return instanceSettingType(T::class.java, defaultPackage, ctorArguments)
}
fun <T> instanceSettingType(type: Class<T>, defaultPackage: String, ctorArguments: Array<Any>): SettingType<T> {
    return SettingType { value ->
        val className = StringSettingType.transform(value)
        val extractorClass = if (className.contains('.')) {
            Class.forName(className)
        } else {
            Class.forName("$defaultPackage.$className")
        }
        if (!type.isAssignableFrom(extractorClass)) {
            throw IllegalArgumentException("${extractorClass.name} is not a ${type.name}")
        }

        val instance = if (ctorArguments.isEmpty()) {
            extractorClass.kotlin.objectInstance ?: extractorClass.newInstance()
        } else {
            val argTypes = ctorArguments.map { it.javaClass }
            extractorClass.constructors
                .singleOrNull { constructor ->
                    constructor.parameterTypes.let {
                        (it.size == argTypes.size) and
                            it.zip(argTypes)
                                .all { (parameterType, argumentType) ->
                                    parameterType.isAssignableFrom(argumentType)
                                }
                    }
                }
                .let {
                    it ?: throw IllegalArgumentException("${extractorClass.name} has no compatible constructors")
                }
                .newInstance(*ctorArguments)
        }

        @Suppress("UNCHECKED_CAST") // isAssignableFrom above ensures this cast is correct
        instance as T
    }
}

class DiagnosingSettingProvider<T>(private val wrapped: AutoKonfig.SettingProvider<T>) {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, T> {
        return try {
            wrapped.provideDelegate(thisRef, prop)
        } catch (e: Exception) {
            if (e.javaClass.name.startsWith("dev.nohus.autokonfig")) {
                throw e
            }
            val name = prop.name
            throw RuntimeException("Error reading key \"$name\" - $e (${AutoKonfig.getKeySource(name)})", e)
        }
    }
}

fun <T> AutoKonfig.SettingProvider<T>.diagnosing(): DiagnosingSettingProvider<T> {
    return DiagnosingSettingProvider(this)
}

fun <T> AutoKonfig.customSetting(type: SettingType<T>, default: T? = null): DiagnosingSettingProvider<T> {
    return SettingProvider({ SettingDelegate(type, it, default) }, default != null, null, null)
        .diagnosing()
}