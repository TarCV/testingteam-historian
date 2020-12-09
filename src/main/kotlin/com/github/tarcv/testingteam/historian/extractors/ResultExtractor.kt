package com.github.tarcv.testingteam.historian.extractors

import com.github.tarcv.testingteam.historian.FullResult
import com.github.tarcv.testingteam.historian.PartialResult

interface ResultExtractor {
    operator fun invoke(result: PartialResult): List<FullResult>?
}