package com.apollo.socially.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.databinding.ItemPostImagePagerBinding
import com.bumptech.glide.Glide

class PostImageUrlPagerAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<PostImageUrlPagerAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemPostImagePagerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUrls[position])
    }

    override fun getItemCount() = imageUrls.size

    inner class ImageViewHolder(private val binding: ItemPostImagePagerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(url: String) {
            Glide.with(binding.root.context)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.sample_photo)
                .error(R.drawable.sample_photo)
                .into(binding.pagerImage)
        }
    }
}