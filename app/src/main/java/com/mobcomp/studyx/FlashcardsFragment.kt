package com.mobcomp.studyx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FlashcardsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvDecks: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var fabAddDeck: FloatingActionButton

    private lateinit var deckAdapter: FlashcardDeckAdapter
    private var decksListener: ListenerRegistration? = null
    private val decks = mutableListOf<FlashcardDeck>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = requireActivity().resources.getIdentifier("fragment_flashcards", "layout", requireActivity().packageName)
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get view IDs dynamically
        val rvDecksId = requireActivity().resources.getIdentifier("rvDecks", "id", requireActivity().packageName)
        val llEmptyStateId = requireActivity().resources.getIdentifier("llEmptyState", "id", requireActivity().packageName)
        val fabAddDeckId = requireActivity().resources.getIdentifier("fabAddDeck", "id", requireActivity().packageName)

        // Initialize views
        rvDecks = view.findViewById(rvDecksId)
        llEmptyState = view.findViewById(llEmptyStateId)
        fabAddDeck = view.findViewById(fabAddDeckId)

        // Setup RecyclerView
        setupRecyclerView()

        // Load decks
        loadDecks()

        // FAB click listener
        fabAddDeck.setOnClickListener {
            showAddDeckDialog()
        }
    }

    private fun setupRecyclerView() {
        deckAdapter = FlashcardDeckAdapter(
            decks = decks,
            onDeckClick = { deck ->
                openDeckDetail(deck)
            },
            onDeckDelete = { deck ->
                showDeleteDeckDialog(deck)
            }
        )

        rvDecks.layoutManager = LinearLayoutManager(requireContext())
        rvDecks.adapter = deckAdapter
    }

    private fun loadDecks() {
        val userId = auth.currentUser?.uid ?: return

        println("📚 DEBUG: Loading flashcard decks for user: $userId")

        decksListener = db.collection("flashcard_decks")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("❌ ERROR loading decks: ${error.message}")
                    showToast("Error loading decks: ${error.message}")
                    return@addSnapshotListener
                }

                decks.clear()
                snapshot?.documents?.forEach { document ->
                    val deck = FlashcardDeck(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        cardCount = (document.getLong("cardCount") ?: 0L).toInt(),
                        createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                        userId = document.getString("userId") ?: userId
                    )
                    decks.add(deck)
                    println("✅ Loaded deck: ${deck.name} (${deck.cardCount} cards)")
                }

                deckAdapter.updateDecks(decks)
                updateEmptyState()
                println("✅ Total decks loaded: ${decks.size}")
            }
    }

    private fun showAddDeckDialog() {
        val context = requireContext()

        val dialogView = LinearLayout(context)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(50, 30, 50, 30)

        val nameInput = EditText(context)
        nameInput.hint = "Deck Name *"
        nameInput.setPadding(20, 20, 20, 20)

        val descInput = EditText(context)
        descInput.hint = "Description (Optional)"
        descInput.setPadding(20, 20, 20, 20)

        dialogView.addView(nameInput)
        dialogView.addView(descInput)

        AlertDialog.Builder(context)
            .setTitle("📚 Create New Deck")
            .setView(dialogView)
            .setPositiveButton("Create") { dialog, _ ->
                val name = nameInput.text.toString().trim()
                val description = descInput.text.toString().trim()

                if (name.isEmpty()) {
                    nameInput.error = "Deck name is required"
                    return@setPositiveButton
                }

                val userId = auth.currentUser?.uid ?: return@setPositiveButton

                val deck = FlashcardDeck(
                    name = name,
                    description = description,
                    cardCount = 0,
                    userId = userId
                )

                saveDeck(deck)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveDeck(deck: FlashcardDeck) {
        val userId = auth.currentUser?.uid ?: return

        println("💾 Saving deck: ${deck.name}")

        val deckData = hashMapOf(
            "name" to deck.name,
            "description" to deck.description,
            "cardCount" to deck.cardCount,
            "createdAt" to System.currentTimeMillis(),
            "userId" to userId
        )

        db.collection("flashcard_decks")
            .add(deckData)
            .addOnSuccessListener { documentReference ->
                println("✅ Deck saved! ID: ${documentReference.id}")
                showToast("🎉 Deck '${deck.name}' created!")
            }
            .addOnFailureListener { e ->
                println("❌ Error saving deck: ${e.message}")
                showToast("Error: ${e.message}")
            }
    }

    private fun openDeckDetail(deck: FlashcardDeck) {
        val items = arrayOf("➕ Add Cards", "📖 Study Cards", "👁️ View Cards", "🗑️ Delete Deck")

        AlertDialog.Builder(requireContext())
            .setTitle("📚 ${deck.name}")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showAddCardDialog(deck)
                    1 -> startStudySession(deck)
                    2 -> viewCards(deck)
                    3 -> showDeleteDeckDialog(deck)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAddCardDialog(deck: FlashcardDeck) {
        val context = requireContext()

        val dialogView = LinearLayout(context)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(50, 30, 50, 30)

        val frontInput = EditText(context)
        frontInput.hint = "Front (Question) *"
        frontInput.setPadding(20, 20, 20, 20)

        val backInput = EditText(context)
        backInput.hint = "Back (Answer) *"
        backInput.setPadding(20, 20, 20, 20)

        dialogView.addView(frontInput)
        dialogView.addView(backInput)

        AlertDialog.Builder(context)
            .setTitle("Add Card to '${deck.name}'")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val front = frontInput.text.toString().trim()
                val back = backInput.text.toString().trim()

                if (front.isEmpty()) {
                    frontInput.error = "Question is required"
                    return@setPositiveButton
                }
                if (back.isEmpty()) {
                    backInput.error = "Answer is required"
                    return@setPositiveButton
                }

                val userId = auth.currentUser?.uid ?: return@setPositiveButton

                val card = Flashcard(
                    deckId = deck.id,
                    front = front,
                    back = back,
                    userId = userId
                )

                saveCard(card, deck)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCard(card: Flashcard, deck: FlashcardDeck) {
        println("💾 Saving card to deck: ${deck.name}")
        println("💾 Question: ${card.front}")
        println("💾 Answer: ${card.back}")

        val cardData = hashMapOf(
            "deckId" to card.deckId,
            "front" to card.front,
            "back" to card.back,
            "isLearned" to false,
            "createdAt" to System.currentTimeMillis(),
            "userId" to card.userId
        )

        db.collection("flashcards")
            .add(cardData)
            .addOnSuccessListener { documentReference ->
                println("✅ Card saved! ID: ${documentReference.id}")

                // Update deck card count
                val newCount = deck.cardCount + 1
                db.collection("flashcard_decks")
                    .document(deck.id)
                    .update("cardCount", newCount)
                    .addOnSuccessListener {
                        println("✅ Deck count updated: $newCount")
                        showToast("✅ Card added to '${deck.name}'!")
                    }
                    .addOnFailureListener { e ->
                        println("⚠️ Couldn't update count: ${e.message}")
                        showToast("Card added but count not updated")
                    }
            }
            .addOnFailureListener { e ->
                println("❌ Error saving card: ${e.message}")
                showToast("Error: ${e.message}")
            }
    }

    private fun startStudySession(deck: FlashcardDeck) {
        println("📖 Starting study session for deck: ${deck.name}")

        val userId = auth.currentUser?.uid ?: return

        db.collection("flashcards")
            .whereEqualTo("deckId", deck.id)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val cards = mutableListOf<Flashcard>()
                snapshot.documents.forEach { document ->
                    val card = Flashcard(
                        id = document.id,
                        deckId = document.getString("deckId") ?: "",
                        front = document.getString("front") ?: "",
                        back = document.getString("back") ?: "",
                        isLearned = document.getBoolean("isLearned") ?: false,
                        userId = document.getString("userId") ?: ""
                    )
                    cards.add(card)
                }

                println("📖 Found ${cards.size} cards in deck")

                if (cards.isEmpty()) {
                    showToast("📭 No cards in this deck yet!")
                } else {
                    showStudyDialog(cards)
                }
            }
            .addOnFailureListener { e ->
                println("❌ Error loading cards: ${e.message}")
                showToast("Error: ${e.message}")
            }
    }

    private fun showStudyDialog(cards: List<Flashcard>) {
        var currentIndex = 0
        var isShowingFront = true

        // Create dialog layout programmatically
        val dialogView = LinearLayout(requireContext())
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(40, 40, 40, 40)

        // Card counter
        val tvCardCounter = TextView(requireContext())
        tvCardCounter.text = "1/${cards.size}"
        tvCardCounter.textSize = 16f
        tvCardCounter.setTextColor(0xFF1976D2.toInt())
        tvCardCounter.gravity = android.view.Gravity.CENTER
        dialogView.addView(tvCardCounter)

        // Card content
        val tvCardContent = TextView(requireContext())
        tvCardContent.text = cards[0].front
        tvCardContent.textSize = 24f
        tvCardContent.setTextColor(0xFF333333.toInt())
        tvCardContent.gravity = android.view.Gravity.CENTER
        tvCardContent.setPadding(0, 40, 0, 40)
        tvCardContent.minHeight = 300
        tvCardContent.gravity = android.view.Gravity.CENTER
        dialogView.addView(tvCardContent)

        // Buttons layout
        val buttonsLayout = LinearLayout(requireContext())
        buttonsLayout.orientation = LinearLayout.HORIZONTAL
        buttonsLayout.gravity = android.view.Gravity.CENTER

        val btnPrev = Button(requireContext())
        btnPrev.text = "◀"
        btnPrev.setTextColor(0xFF1976D2.toInt())
        btnPrev.setBackgroundColor(0xFFFFFFFF.toInt())
        btnPrev.setPadding(30, 15, 30, 15)

        val btnFlip = Button(requireContext())
        btnFlip.text = "FLIP"
        btnFlip.setTextColor(0xFFFFFFFF.toInt())
        btnFlip.setBackgroundColor(0xFF1976D2.toInt())
        btnFlip.setPadding(40, 15, 40, 15)

        val btnNext = Button(requireContext())
        btnNext.text = "▶"
        btnNext.setTextColor(0xFF1976D2.toInt())
        btnNext.setBackgroundColor(0xFFFFFFFF.toInt())
        btnNext.setPadding(30, 15, 30, 15)

        buttonsLayout.addView(btnPrev)
        buttonsLayout.addView(btnFlip)
        buttonsLayout.addView(btnNext)
        dialogView.addView(buttonsLayout)

        fun updateCard() {
            if (currentIndex < cards.size) {
                val card = cards[currentIndex]
                tvCardContent.text = if (isShowingFront) card.front else card.back
                tvCardCounter.text = "${currentIndex + 1}/${cards.size}"

                // Change color based on side
                if (isShowingFront) {
                    tvCardContent.setTextColor(0xFF1976D2.toInt())
                } else {
                    tvCardContent.setTextColor(0xFF4CAF50.toInt())
                }
            }
        }

        btnFlip.setOnClickListener {
            isShowingFront = !isShowingFront
            updateCard()
        }

        btnNext.setOnClickListener {
            if (currentIndex < cards.size - 1) {
                currentIndex++
                isShowingFront = true
                updateCard()
            } else {
                showToast("🎉 Last card reached!")
            }
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                isShowingFront = true
                updateCard()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("📖 Study Mode")
            .setView(dialogView)
            .setPositiveButton("✅ Mark as Learned") { dialog, _ ->
                if (currentIndex < cards.size) {
                    val card = cards[currentIndex]
                    db.collection("flashcards")
                        .document(card.id)
                        .update("isLearned", true)
                        .addOnSuccessListener {
                            showToast("✅ Card marked as learned!")
                        }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun viewCards(deck: FlashcardDeck) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("flashcards")
            .whereEqualTo("deckId", deck.id)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isEmpty()) {
                    showToast("📭 No cards in this deck yet!")
                    return@addOnSuccessListener
                }

                val cardsText = StringBuilder()
                cardsText.append("Cards in '${deck.name}':\n\n")

                snapshot.documents.forEachIndexed { index, document ->
                    val front = document.getString("front") ?: ""
                    val back = document.getString("back") ?: ""
                    val isLearned = document.getBoolean("isLearned") ?: false
                    val status = if (isLearned) "✅" else "📝"

                    cardsText.append("${index + 1}. $status Q: $front\n   A: $back\n\n")
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("📚 ${deck.name} (${snapshot.documents.size} cards)")
                    .setMessage(cardsText.toString())
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    private fun showDeleteDeckDialog(deck: FlashcardDeck) {
        AlertDialog.Builder(requireContext())
            .setTitle("🗑️ Delete Deck")
            .setMessage("Delete '${deck.name}' and all its ${deck.cardCount} cards?")
            .setPositiveButton("Delete") { _, _ ->
                deleteDeck(deck)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDeck(deck: FlashcardDeck) {
        println("🗑️ Deleting deck: ${deck.name}")

        val userId = auth.currentUser?.uid ?: return

        // First delete all cards in the deck
        db.collection("flashcards")
            .whereEqualTo("deckId", deck.id)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        println("🗑️ Deleted ${snapshot.documents.size} cards")

                        // Then delete the deck
                        db.collection("flashcard_decks")
                            .document(deck.id)
                            .delete()
                            .addOnSuccessListener {
                                println("✅ Deck deleted successfully!")
                                showToast("🗑️ Deck '${deck.name}' deleted!")
                            }
                            .addOnFailureListener { e ->
                                println("❌ Error deleting deck: ${e.message}")
                                showToast("Error deleting deck: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        println("❌ Error deleting cards: ${e.message}")
                        showToast("Error: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                println("❌ Error fetching cards: ${e.message}")
                showToast("Error: ${e.message}")
            }
    }

    private fun updateEmptyState() {
        val isEmpty = decks.isEmpty()
        llEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvDecks.visibility = if (isEmpty) View.GONE else View.VISIBLE
        println("📊 Empty state: $isEmpty (${decks.size} decks)")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        decksListener?.remove()
    }
}