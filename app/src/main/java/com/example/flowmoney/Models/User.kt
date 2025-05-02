package com.example.flowmoney.Models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

@IgnoreExtraProperties
class User {
    // Removed @DocumentId annotation
    @PropertyName("userId")
    var userId: String = ""

    @PropertyName("full_name")
    var fullName: String = ""

    @PropertyName("username")
    var username: String = ""

    @PropertyName("email")
    var email: String = ""

    @PropertyName("phone_number")
    var phoneNumber: String? = null

    @PropertyName("profile_image_url")
    var profileImageUrl: String? = null

    @PropertyName("address")
    var address: String? = null

    @PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis()

    @PropertyName("last_login_at")
    var lastLoginAt: Long = System.currentTimeMillis()

    @PropertyName("is_social_login")
    var isSocialLogin: Boolean = false

    @PropertyName("social_login_type")
    var socialLoginType: String? = null

    // Empty constructor required for Firestore
    constructor()

    // Constructor with required fields
    constructor(
        userId: String,
        fullName: String,
        username: String,
        email: String
    ) {
        this.userId = userId
        this.fullName = fullName
        this.username = username
        this.email = email
    }

    // Full constructor
    constructor(
        userId: String,
        fullName: String,
        username: String,
        email: String,
        phoneNumber: String? = null,
        profileImageUrl: String? = null,
        address: String? = null,
        isSocialLogin: Boolean = false,
        socialLoginType: String? = null
    ) {
        this.userId = userId
        this.fullName = fullName
        this.username = username
        this.email = email
        this.phoneNumber = phoneNumber
        this.profileImageUrl = profileImageUrl
        this.address = address
        this.isSocialLogin = isSocialLogin
        this.socialLoginType = socialLoginType
    }

    // Helper method to create a User from a social login
    @Exclude
    fun createFromSocialLogin(loginType: String): User {
        this.isSocialLogin = true
        this.socialLoginType = loginType
        return this
    }

    // Helper method to update last login time
    @Exclude
    fun updateLoginTime() {
        this.lastLoginAt = System.currentTimeMillis()
    }

    // Convert to HashMap for Firestore (useful for updates)
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "full_name" to fullName,
            "username" to username,
            "email" to email,
            "phone_number" to phoneNumber,
            "profile_image_url" to profileImageUrl,
            "address" to address,
            "created_at" to createdAt,
            "last_login_at" to lastLoginAt,
            "is_social_login" to isSocialLogin,
            "social_login_type" to socialLoginType
        )
    }

    override fun toString(): String {
        return "User(userId='$userId', fullName='$fullName', username='$username', email='$email')"
    }
}