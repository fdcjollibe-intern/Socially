package com.apollo.socially.ui.create

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.databinding.ItemPickerCameraBinding
import com.apollo.socially.databinding.ItemPickerMediaBinding
import com.apollo.socially.model.MediaItem
import com.apollo.socially.model.MediaType
import com.bumptech.glide.Glide
import java.util.concurrent.TimeUnit

class MediaGridAdapter(
    private val onCameraClick: () -> Unit,
    private val onMediaClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CAMERA = 0
        private const val TYPE_MEDIA = 1
        const val MAX_SELECTION = 10
    }

    private var mediaItems: List<MediaItem> = emptyList()
    // orderedSelection: mediaId -> selection order (1-based)
    private val orderedSelection = linkedMapOf<Long, Int>()
    var isMultiSelectMode = false
        private set

    fun submitList(items: List<MediaItem>) {
        mediaItems = items
        notifyDataSetChanged()
    }

    fun toggleMultiSelectMode() {
        isMultiSelectMode = !isMultiSelectMode
        if (!isMultiSelectMode) orderedSelection.clear()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<MediaItem> {
        return orderedSelection.keys
            .sortedBy { orderedSelection[it] }
            .mapNotNull { id -> mediaItems.find { it.id == id } }
    }

    private fun toggleSelection(item: MediaItem) {
        if (orderedSelection.containsKey(item.id)) {
            // Deselect — re-number remaining
            orderedSelection.remove(item.id)
            var counter = 1
            val reordered = linkedMapOf<Long, Int>()
            orderedSelection.keys.forEach { reordered[it] = counter++ }
            orderedSelection.clear()
            orderedSelection.putAll(reordered)
        } else {
            if (orderedSelection.size >= MAX_SELECTION) return
            orderedSelection[item.id] = orderedSelection.size + 1
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (position == 0) TYPE_CAMERA else TYPE_MEDIA

    override fun getItemCount() = mediaItems.size + 1 // +1 for camera

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CAMERA) {
            val binding = ItemPickerCameraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            // Force square
            val size = parent.measuredWidth / 3
            binding.root.layoutParams = RecyclerView.LayoutParams(size, size)
            CameraViewHolder(binding)
        } else {
            val binding = ItemPickerMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            val size = parent.measuredWidth / 3
            binding.root.layoutParams = RecyclerView.LayoutParams(size, size)
            MediaViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CameraViewHolder) {
            holder.itemView.setOnClickListener { onCameraClick() }
        } else if (holder is MediaViewHolder) {
            holder.bind(mediaItems[position - 1])
        }
    }

    inner class CameraViewHolder(binding: ItemPickerCameraBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class MediaViewHolder(private val binding: ItemPickerMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            Glide.with(binding.root.context)
                .load(item.uri)
                .centerCrop()
                .into(binding.pickerMediaThumbnail)

            // Video duration
            if (item.type == MediaType.VIDEO && item.durationMs > 0) {
                binding.pickerMediaDuration.visibility = View.VISIBLE
                binding.pickerMediaDuration.text = formatDuration(item.durationMs)
            } else {
                binding.pickerMediaDuration.visibility = View.GONE
            }

            val selectionOrder = orderedSelection[item.id]
            val isSelected = selectionOrder != null

            if (isMultiSelectMode) {
                // Show circle indicators
                binding.pickerMediaUnselectedCircle.visibility = if (isSelected) View.GONE else View.VISIBLE
                binding.pickerMediaNumber.visibility = if (isSelected) View.VISIBLE else View.GONE
                if (isSelected) binding.pickerMediaNumber.text = selectionOrder.toString()
                binding.pickerMediaDim.visibility = if (isSelected) View.VISIBLE else View.GONE
            } else {
                // Single select — just highlight
                binding.pickerMediaUnselectedCircle.visibility = View.GONE
                binding.pickerMediaNumber.visibility = if (isSelected) View.VISIBLE else View.GONE
                if (isSelected) binding.pickerMediaNumber.text = "✓"
                binding.pickerMediaDim.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(item)
                } else {
                    // Single select — replace selection
                    orderedSelection.clear()
                    orderedSelection[item.id] = 1
                    notifyDataSetChanged()
                    onMediaClick(item)
                }
            }
        }

        private fun formatDuration(ms: Long): String {
            val min = TimeUnit.MILLISECONDS.toMinutes(ms)
            val sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
            return String.format("%d:%02d", min, sec)
        }
    }
}
