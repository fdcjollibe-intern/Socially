package com.apollo.socially.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.databinding.ItemPostImagePagerBinding

class PostImagePagerAdapter(
    private val imageResList: List<Int>
) : RecyclerView.Adapter<PostImagePagerAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemPostImagePagerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageResList[position])
    }

    override fun getItemCount() = imageResList.size

    inner class ImageViewHolder(private val binding: ItemPostImagePagerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(imageRes: Int) {
            binding.pagerImage.setImageResource(imageRes)
        }
    }
}
