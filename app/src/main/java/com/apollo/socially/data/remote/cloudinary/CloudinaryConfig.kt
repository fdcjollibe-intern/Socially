package com.apollo.socially.data.remote.cloudinary

object CloudinaryConfig {
    const val CLOUD_NAME    = "dn6rffrwk"
    const val UPLOAD_PRESET = "Socially_CDN"
    const val IMAGE_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
    const val VIDEO_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/video/upload"

    // Optional transforms — append to any returned URL
    // e.g. avatarTransform for 200x200 cropped circle
    const val AVATAR_TRANSFORM  = "c_fill,g_face,h_200,w_200,r_max,q_auto,f_auto"
    const val POST_TRANSFORM     = "q_auto,f_auto"
    const val THUMBNAIL_TRANSFORM = "c_fill,h_300,w_300,q_auto,f_auto"
}
