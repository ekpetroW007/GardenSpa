package ru.samates.gardenspa

import org.junit.Test

import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun `build config values are read at runtime`() {
        assertEquals("debug", buildConfigString("BUILD_TYPE"))
    }
}
