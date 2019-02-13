package io.kani

import io.kani.gui.TestClass
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


class SampleTest {
    @Test
    fun testEasy() {
        val testClass = TestClass()
        Assertions.assertThat(testClass.foo(3,4)).isEqualTo(7)
    }

    @Test
    fun testEasy2() {
        val testClass = TestClass()
        Assertions.assertThat(testClass.foo(3,5)).isEqualTo(8)
    }
}
