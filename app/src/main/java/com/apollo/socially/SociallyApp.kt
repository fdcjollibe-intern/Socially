package com.apollo.socially

import android.app.Application
import com.apollo.socially.data.cache.OtherProfileCache
import com.apollo.socially.data.cache.ProfileCache
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.engine.cache.LruResourceCache

class SociallyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Clear in-memory caches on fresh process start
        ProfileCache.invalidateCache()
        OtherProfileCache.invalidate()

        // Cap Glide memory cache to 20MB to avoid OOM
        Glide.init(
            this,
            GlideBuilder()
                .setMemoryCache(LruResourceCache(20L * 1024 * 1024))
        )
    }
}