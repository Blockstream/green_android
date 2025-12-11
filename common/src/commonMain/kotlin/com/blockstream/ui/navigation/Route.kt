package com.blockstream.ui.navigation

interface Route {
    val uniqueId: String
    val unique: Boolean
    val makeItRoot: Boolean
    val isBottomNavigation: Boolean
}