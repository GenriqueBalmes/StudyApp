package com.mobcomp.studyx

data class Flashcard(
    var id: String = "",
    var deckId: String = "",
    var front: String = "",
    var back: String = "",
    var isLearned: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var userId: String = ""
)