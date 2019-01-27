package io.kani.tests

import io.kani.TestClass
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class SampleTest {
    @Test
    fun testEasy() {
        Assertions.assertEquals(TestClass().foo(3, 4), 7)
    }

    @Test
    fun testEasy2() {
        Assertions.assertEquals(TestClass().foo(3, 4), 7)
    }
}
