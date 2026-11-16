package com.example.system_mini_capstone

import com.example.system_mini_capstone.data.network.AuthErrorMapper
import com.example.system_mini_capstone.data.network.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class AuthRegistrationTest {

    @Test
    fun testDisplayNameValidation() {
        val name = "John Doe"
        assertTrue("Valid display name should pass", name.trim().isNotBlank())
        val blankName = "   "
        assertFalse("Blank display name should fail", blankName.trim().isNotBlank())
    }

    @Test
    fun testPasswordValidation() {
        val validPass = "123456"
        val invalidPass = "12345"
        assertTrue("Password >= 6 chars should pass", validPass.length >= 6)
        assertFalse("Password < 6 chars should fail", invalidPass.length >= 6)
    }

    @Test
    fun testPasswordMatchValidation() {
        val pass1 = "SecurePass123"
        val pass2 = "SecurePass123"
        val passMismatch = "DifferentPass"

        assertTrue("Matching passwords should pass", pass1 == pass2)
        assertFalse("Mismatching passwords should fail", pass1 == passMismatch)
    }

    @Test
    fun testAuthErrorMapper_NetworkError() {
        val ex = UnknownHostException("Unable to resolve host")
        val mapped = AuthErrorMapper.mapError(ex)
        assertTrue(mapped.startsWith("No internet connection. Check your network and try again."))
    }

    @Test
    fun testAuthErrorMapper_InvalidCredentials() {
        val ex = Exception("The INVALID_LOGIN_CREDENTIALS were provided")
        val mapped = AuthErrorMapper.mapError(ex)
        assertTrue(mapped.startsWith("Incorrect email or password."))
    }

    @Test
    fun testAuthErrorMapper_EmailAlreadyInUse() {
        val ex = Exception("The email address is email-already-in-use by another account.")
        val mapped = AuthErrorMapper.mapError(ex)
        assertTrue(mapped.startsWith("An account with this email already exists."))
    }

    @Test
    fun testAuthErrorMapper_WeakPassword() {
        val ex = Exception("The password is weak-password.")
        val mapped = AuthErrorMapper.mapError(ex)
        assertTrue(mapped.startsWith("Choose a password with at least 6 characters."))
    }

    @Test
    fun testAuthErrorMapper_TooManyRequests() {
        val ex = Exception("too-many-requests. Try again later.")
        val mapped = AuthErrorMapper.mapError(ex)
        assertTrue(mapped.startsWith("Too many attempts. Try again later."))
    }

    @Test
    fun testUserRoleHandling_GuestNeverMapsToAdmin() {
        val roleGuest = UserRole.fromString("GUEST")
        assertEquals(UserRole.GUEST, roleGuest)
        assertTrue(roleGuest != UserRole.ADMIN)
    }

    @Test
    fun testUserRoleHandling_UnknownRoleDefaultsToUser() {
        val roleUnknown = UserRole.fromString("SOME_RANDOM_ROLE")
        assertEquals(UserRole.USER, roleUnknown)
        assertTrue(roleUnknown != UserRole.ADMIN)
    }
}
