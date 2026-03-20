package com.apollo.socially.ui.create

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.databinding.ItemCaptionMediaPreviewBinding
import com.apollo.socially.model.MediaItem
import com.bumptech.glide.Glide

class CaptionMediaPreviewAdapter(
    private val items: List<MediaItem>
) : RecyclerView.Adapter<CaptionMediaPreviewAdapter.PreviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val binding = ItemCaptionMediaPreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) =
        holder.bind(items[position], position + 1)

    override fun getItemCount() = items.size

    inner class PreviewViewHolder(private val binding: ItemCaptionMediaPreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem, order: Int) {
            Glide.with(binding.root.context)
                .load(item.uri)
                .centerCrop()
                .into(binding.captionPreviewThumbnail)
            binding.captionPreviewNumber.text = order.toString()
        }
    }
}
