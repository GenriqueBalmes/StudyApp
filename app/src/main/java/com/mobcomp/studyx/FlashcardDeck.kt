package com.mobcomp.studyx

data class FlashcardDeck(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var cardCount: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var userId: String = ""
)