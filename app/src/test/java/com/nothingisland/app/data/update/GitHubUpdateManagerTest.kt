package com.nothingisland.app.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateManagerTest {

    @Test
    fun isNewerVersion_detectsPatchIncrement() {
        assertTrue(GitHubUpdateManager.isNewerVersion("0.1.0", "0.1.1"))
    }

    @Test
    fun isNewerVersion_detectsMinorIncrement() {
        assertTrue(GitHubUpdateManager.isNewerVersion("0.1.5", "0.2.0"))
    }

    @Test
    fun isNewerVersion_detectsMajorIncrement() {
        assertTrue(GitHubUpdateManager.isNewerVersion("0.9.9", "1.0.0"))
    }

    @Test
    fun isNewerVersion_sameOrOlderReturnsFalse() {
        assertFalse(GitHubUpdateManager.isNewerVersion("0.1.0", "0.1.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("0.2.0", "0.1.9"))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.0.0", "0.9.9"))
    }
}
