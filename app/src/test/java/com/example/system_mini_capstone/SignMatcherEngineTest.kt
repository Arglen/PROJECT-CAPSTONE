package com.example.system_mini_capstone

import com.example.system_mini_capstone.admin.studio.SignSampleDto
import com.example.system_mini_capstone.ml.SignMatcherEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Method

class SignMatcherEngineTest {

    @Test
    fun testMatcherLogic() = runBlocking {
        // Use reflection to test private/internal buildIndex and match directly without Android Context / Firestore
        val engine = SignMatcherEngine::class.java.getDeclaredConstructor(android.content.Context::class.java).let {
            it.isAccessible = true
            it.newInstance(null as android.content.Context?)
        }

        val sample1 = SignSampleDto(label = "HELLO", landmarks = List(63) { 0.1f })
        val sample2 = SignSampleDto(label = "HELLO", landmarks = List(63) { 0.11f })
        val sample3 = SignSampleDto(label = "FOUR", landmarks = List(63) { 0.9f })

        val buildIndexMethod: Method = SignMatcherEngine::class.java.getDeclaredMethod("buildIndex", List::class.java)
        buildIndexMethod.isAccessible = true
        buildIndexMethod.invoke(engine, listOf(sample1, sample2, sample3))

        // 1. Close to HELLO
        val queryClose = List(63) { 0.105f }
        val resClose = engine.match(queryClose)
        assertEquals("HELLO", resClose.label)

        // 2. Far from all samples -> UNKNOWN
        val queryFar = List(63) { 5.0f }
        val resFar = engine.match(queryFar)
        assertEquals("UNKNOWN", resFar.label)

        // 3. Empty sample set
        val emptyEngine = SignMatcherEngine::class.java.getDeclaredConstructor(android.content.Context::class.java).let {
            it.isAccessible = true
            it.newInstance(null as android.content.Context?)
        }
        val resEmpty = emptyEngine.match(queryClose)
        assertEquals("UNKNOWN", resEmpty.label)
    }
}
