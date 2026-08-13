package com.guitartuner.tuning

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 调弦持久化：按乐器记录「当前选中的调弦」，并保存用户自定义调弦（全局共享）。
 */
object TuningStore {

    private const val PREFS = "tuner_store"
    private const val KEY_CUSTOM = "custom_tunings"
    private const val KEY_INSTRUMENT = "current_instrument"

    // ---- 乐器 ----

    fun currentInstrument(context: Context): Instrument {
        val name = prefs(context).getString(KEY_INSTRUMENT, Instrument.GUITAR.name)
            ?: Instrument.GUITAR.name
        return Instrument.values().firstOrNull { it.name == name } ?: Instrument.GUITAR
    }

    fun setCurrentInstrument(context: Context, instrument: Instrument) {
        prefs(context).edit().putString(KEY_INSTRUMENT, instrument.name).apply()
    }

    // ---- 调弦 ----

    fun allTunings(context: Context, instrument: Instrument): List<Tuning> =
        TuningLibrary.presetsFor(instrument) + loadCustom(context)

    fun currentTuning(context: Context): Tuning = currentTuningFor(context, currentInstrument(context))

    fun currentTuningFor(context: Context, instrument: Instrument): Tuning {
        val default = TuningLibrary.defaultFor(instrument)
        val name = prefs(context).getString(tuningKey(instrument), default.name) ?: default.name
        return allTunings(context, instrument).firstOrNull { it.name == name } ?: default
    }

    fun setCurrentTuning(context: Context, name: String) {
        val instrument = currentInstrument(context)
        prefs(context).edit().putString(tuningKey(instrument), name).apply()
    }

    private fun tuningKey(instrument: Instrument) = "current_tuning_${instrument.name}"

    // ---- 自定义调弦（全局共享）----

    fun loadCustom(context: Context): List<Tuning> {
        val json = prefs(context).getString(KEY_CUSTOM, null) ?: return emptyList()
        return try {
            parseCustom(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addOrUpdateCustom(context: Context, tuning: Tuning) {
        val list = loadCustom(context).toMutableList()
        val idx = list.indexOfFirst { it.name == tuning.name }
        val saved = tuning.copy(isCustom = true)
        if (idx >= 0) list[idx] = saved else list.add(saved)
        saveCustom(context, list)
    }

    fun deleteCustom(context: Context, name: String) {
        saveCustom(context, loadCustom(context).filterNot { it.name == name })
        val instrument = currentInstrument(context)
        if (prefs(context).getString(tuningKey(instrument), null) == name) {
            prefs(context).edit()
                .putString(tuningKey(instrument), TuningLibrary.defaultFor(instrument).name)
                .apply()
        }
    }

    private fun saveCustom(context: Context, list: List<Tuning>) {
        val arr = JSONArray()
        for (t in list) {
            val o = JSONObject()
            o.put("name", t.name)
            val notes = JSONArray()
            for (s in t.strings) notes.put(s.note.fullName)
            o.put("notes", notes)
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_CUSTOM, arr.toString()).apply()
    }

    private fun parseCustom(json: String): List<Tuning> {
        val arr = JSONArray(json)
        val list = mutableListOf<Tuning>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.getString("name")
            val notesArr = o.getJSONArray("notes")
            val strings = mutableListOf<GuitarString>()
            for (j in 0 until notesArr.length()) {
                strings.add(GuitarString(j + 1, Note.parse(notesArr.getString(j))))
            }
            list.add(Tuning(name, strings, isCustom = true))
        }
        return list
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
