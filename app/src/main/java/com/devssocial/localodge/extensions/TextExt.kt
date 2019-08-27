package com.devssocial.localodge.extensions

val String.isEmail get() = matches("[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}".toRegex())