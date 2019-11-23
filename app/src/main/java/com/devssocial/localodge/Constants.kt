package com.devssocial.localodge

// SHARED PREF
const val LOCALODGE_SHARED_PREF = "com.localodge.shared_pref"
const val TRIAL_ACCOUNT_REQUESTED = "trialAccRequested"

// ALGOLIA API
const val ALGOLIA_APP_ID = "L4UXH3U7Y3"
const val ALGOLIA_SEARCH_API_KEY = "80571fb1652061854afb2cce4db3b1ae"

// ALGOLIA indices
const val POSTS_INDEX = "posts"

// firestore collections
const val COLLECTION_AVATARS = "avatars"
const val COLLECTION_USERS = "users"
const val COLLECTION_CITIES = "cities"
const val COLLECTION_POSTS = "posts"
const val COLLECTION_FEEDBACK = "feedback"
const val COLLECTION_BLACKLIST = "blacklist"
const val COLLECTION_NOTIFICATIONS = "notifications"
const val COLLECTION_REPORTS = "reports"
const val COLLECTION_BLOCKING = "blocking"
const val COLLECTION_BLOCKED_POSTS = "blockedPosts"
const val COLLECTION_COMMENTS = "comments"
const val COLLECTION_METADATA = "metadata"

// firestore documents
const val DOC_CUSTOMER_INFO = "customerInfo"
const val DOC_META = "meta"

// firestore fields
const val FIELD_LIKES = "likes"
const val FIELD_TIMESTAMP = "timestamp"
const val FIELD_UNREAD = "unread"
const val FIELD_OBJECT_ID = "objectID"

// RxJava error message
const val NO_VALUE = "No value"

val REPORT_REASONS = arrayOf("Illegal", "Hatred", "Violence", "Discrimination", "Harassment", "Trolling", "Spamming",
    "Impersonation", "Advertisement", "Fraud", "Age Limit", "Other")

// Firebase error codes
const val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
const val ERROR_USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"
const val ERROR_EMAIL_ALREADY_IN_USE  = "ERROR_EMAIL_ALREADY_IN_USE"
const val ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL = "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"
const val ERROR_CREDENTIAL_ALREADY_IN_USE = "ERROR_CREDENTIAL_ALREADY_IN_USE"