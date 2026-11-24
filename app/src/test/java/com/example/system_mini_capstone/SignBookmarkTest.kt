package com.example.system_mini_capstone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignBookmarkTest {

    @Test
    fun testBookmarkAddAndRemoveLogic() {
        val bookmarkedIds = mutableSetOf<String>()
        val signId = "hello"

        // Toggle add
        val isAdded1 = if (bookmarkedIds.contains(signId)) {
            bookmarkedIds.remove(signId)
            false
        } else {
            bookmarkedIds.add(signId)
            true
        }
        assertTrue("Bookmark should be added", isAdded1)
        assertTrue("Set should contain signId", bookmarkedIds.contains(signId))

        // Toggle remove
        val isAdded2 = if (bookmarkedIds.contains(signId)) {
            bookmarkedIds.remove(signId)
            false
        } else {
            bookmarkedIds.add(signId)
            true
        }
        assertFalse("Bookmark should be removed", isAdded2)
        assertFalse("Set should not contain signId", bookmarkedIds.contains(signId))
    }
}
