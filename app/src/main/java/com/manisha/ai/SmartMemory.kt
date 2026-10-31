package com.manisha.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object SmartMemory {

    private const val PREFS_NAME = "manisha_memory"
    private const val MEMORY_KEY = "saved_memories"

    data class Memory(
        val id: String,
        val text: String
    )

    private fun getPreferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun getMemories(context: Context): List<Memory> {
        val json = getPreferences(context)
            .getString(MEMORY_KEY, "[]") ?: "[]"

        return try {
            val array = JSONArray(json)
            val memories = mutableListOf<Memory>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                memories.add(
                    Memory(
                        id = item.optString("id"),
                        text = item.optString("text")
                    )
                )
            }

            memories
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addMemory(context: Context, text: String): Boolean {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return false

        val memories = getMemories(context).toMutableList()

        if (memories.any {
                it.text.equals(cleanText, ignoreCase = true)
            }) {
            return false
        }

        memories.add(
            Memory(
                id = UUID.randomUUID().toString(),
                text = cleanText
            )
        )

        return saveMemories(context, memories)
    }

    fun updateMemory(
        context: Context,
        id: String,
        newText: String
    ): Boolean {
        val cleanText = newText.trim()
        if (cleanText.isEmpty()) return false

        val memories = getMemories(context).toMutableList()
        val index = memories.indexOfFirst { it.id == id }

        if (index == -1) return false

        memories[index] = Memory(id, cleanText)
        return saveMemories(context, memories)
    }

    fun deleteMemory(context: Context, id: String): Boolean {
        val memories = getMemories(context)
            .filterNot { it.id == id }

        return saveMemories(context, memories)
    }

    fun clearAll(context: Context): Boolean {
        return saveMemories(context, emptyList())
    }

    private fun saveMemories(
        context: Context,
        memories: List<Memory>
    ): Boolean {
        val array = JSONArray()

        memories.forEach { memory ->
            array.put(
                JSONObject()
                    .put("id", memory.id)
                    .put("text", memory.text)
            )
        }

        return getPreferences(context)
            .edit()
            .putString(MEMORY_KEY, array.toString())
            .commit()
    }
}
