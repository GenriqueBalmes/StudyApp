package com.mobcomp.studyx

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FlashcardDeckAdapter(
    private var decks: List<FlashcardDeck> = emptyList(),
    private val onDeckClick: (FlashcardDeck) -> Unit,
    private val onDeckDelete: (FlashcardDeck) -> Unit
) : RecyclerView.Adapter<FlashcardDeckAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeckName: TextView = itemView.findViewById(
            itemView.resources.getIdentifier("tvDeckName", "id", itemView.context.packageName)
        )
        val tvCardCount: TextView = itemView.findViewById(
            itemView.resources.getIdentifier("tvCardCount", "id", itemView.context.packageName)
        )
        val tvDescription: TextView = itemView.findViewById(
            itemView.resources.getIdentifier("tvDescription", "id", itemView.context.packageName)
        )
        val ivDelete: ImageView = itemView.findViewById(
            itemView.resources.getIdentifier("ivDelete", "id", itemView.context.packageName)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = parent.resources.getIdentifier("item_deck", "layout", parent.context.packageName)
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deck = decks[position]

        holder.tvDeckName.text = deck.name
        holder.tvCardCount.text = "${deck.cardCount} cards"
        holder.tvDescription.text = deck.description

        if (deck.description.isEmpty()) {
            holder.tvDescription.visibility = View.GONE
        } else {
            holder.tvDescription.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            onDeckClick(deck)
        }

        holder.ivDelete.setOnClickListener {
            onDeckDelete(deck)
        }
    }

    override fun getItemCount(): Int = decks.size

    fun updateDecks(newDecks: List<FlashcardDeck>) {
        decks = newDecks
        notifyDataSetChanged()
    }
}