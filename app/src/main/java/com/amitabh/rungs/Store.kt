package com.amitabh.rungs

import android.util.AtomicFile
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

val RungsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

/** All app data lives in one JSON file. AtomicFile means a crash mid-write can't corrupt it. */
class Store(dir: File) {
    private val file = AtomicFile(File(dir, "rungs.json"))

    fun load(): AppData =
        if (file.baseFile.exists()) RungsJson.decodeFromString(file.readFully().decodeToString()) else AppData()

    fun save(data: AppData) {
        val out = file.startWrite()
        try {
            out.write(RungsJson.encodeToString(data).encodeToByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    companion object {
        fun export(data: AppData, out: OutputStream) = out.write(RungsJson.encodeToString(data).encodeToByteArray())

        /** Throws on a bad file; the caller keeps the current data in that case. */
        fun import(input: InputStream): AppData =
            RungsJson.decodeFromString<AppData>(input.readBytes().decodeToString()).also { d ->
                // Every screen indexes levels by these, so a broken backup must not get in.
                require(Day.entries.all { day -> d.movements.any { it.day == day } }) { "a day has no movements" }
                require(d.movements.all { it.levels.isNotEmpty() }) { "empty ladder" }
                require(d.movements.all { m -> d.progressOf(m).level in m.levels.indices }) { "level out of range" }
            }
    }
}
