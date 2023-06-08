package com.github.roihershberg.mokoresources

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testDummy() {
    }

    override fun getTestDataPath() = "src/test/testData"
}
