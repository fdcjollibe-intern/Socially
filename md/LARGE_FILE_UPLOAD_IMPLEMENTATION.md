# Large File Upload Implementation Summary

## Problem
When uploading videos or large media files to create posts, users encountered the error:
```
Payload size exceeded max size (10486108)
```

This occurred because Cloudinary's standard upload API has a ~10MB limit.

## Solution Implemented

### 1. **File Size Limits**
- Maximum file size per media: **150MB**
- Files under 10MB: Use standard upload API
- Files over 10MB: Use chunked upload API (upload_large)

### 2. **CloudinaryUploader.kt Enhancements**

#### Added Constants:
```kotlin
const val MAX_FILE_SIZE = 150 * 1024 * 1024L // 150MB - Public for validation
private const val STANDARD_UPLOAD_LIMIT = 10 * 1024 * 1024L // 10MB
private const val CHUNK_SIZE = 5 * 1024 * 1024 // 5MB chunks
```

#### New Public Methods:
- `getFileSize(context: Context, uri: Uri): Long` - Gets file size from URI
- `formatFileSize(bytes: Long): String` - Formats bytes to human-readable format (e.g., "25.3 MB")

#### Chunked Upload Implementation:
- Files > 10MB are automatically uploaded using Cloudinary's chunked upload API
- Uses 5MB chunks with proper Content-Range headers
- Generates unique upload ID for each chunked session
- Progress tracking for each chunk
- Proper error handling and logging

### 3. **CloudinaryConfig.kt Updates**

Added upload_large endpoints:
```kotlin
const val IMAGE_UPLOAD_LARGE_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload_large"
const val VIDEO_UPLOAD_LARGE_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/video/upload_large"
```

### 4. **MediaItem.kt Enhancement**

Added `sizeBytes` field to track file sizes:
```kotlin
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val durationMs: Long = 0L, // for video only
    val sizeBytes: Long = 0L   // file size in bytes
)
```

### 5. **MediaLoader.kt Updates**

Now queries and populates file sizes when loading media:
- Queries `MediaStore.Images.Media.SIZE` for images
- Queries `MediaStore.Video.Media.SIZE` for videos
- Populates `sizeBytes` field in MediaItem

### 6. **CreatePostCaptionFragment.kt Validation**

Added pre-upload validation:
- Checks each file size before starting upload
- Shows error if file exceeds 150MB limit
- Shows warning for large uploads (>50MB)
- Displays human-readable file sizes in error messages

Example validation:
```kotlin
if (fileSize > CloudinaryUploader.MAX_FILE_SIZE) {
    Toast.makeText(
        requireContext(),
        "File too large: ${CloudinaryUploader.formatFileSize(fileSize)}. Max 150MB per file.",
        Toast.LENGTH_LONG
    ).show()
}
```

### 7. **CreatePostPickerFragment.kt Fix**

Updated camera capture MediaItem construction to include sizeBytes parameter.

## How It Works

### Upload Flow:

1. **User selects media** → MediaLoader queries file sizes from MediaStore
2. **User adds caption and clicks Post** → CreatePostCaptionFragment validates all file sizes
3. **If validation passes** → PostUploadService starts
4. **For each file**:
   - If < 10MB → Standard upload with progress tracking
   - If 10MB-150MB → Chunked upload with progress tracking
   - If > 150MB → Error shown to user before upload starts

### Chunked Upload Process:

```
File (e.g., 45MB video)
    ↓
Split into 5MB chunks (9 chunks)
    ↓
Upload chunk 1 with X-Unique-Upload-Id header
    ↓
Upload chunk 2-8 with same upload ID
    ↓
Upload chunk 9 (final) → Cloudinary returns secure_url
    ↓
Post created in Firestore with media URL
```

## Technical Details

### Cloudinary Chunked Upload Headers:
- `X-Unique-Upload-Id`: Unique identifier for the upload session
- `Content-Range`: bytes START-END/TOTAL (e.g., "bytes 0-5242879/45678901")

### Error Handling:
- File size validation before upload
- Chunk upload retry logic (built into PostUploadService)
- User-friendly error messages
- Logging with TAG "CloudinaryUploader" for debugging

### Performance:
- Parallel chunk uploads: No (sequential for reliability)
- Chunk size: 5MB (optimal for mobile networks)
- Progress tracking: Per-chunk with overall percentage

## Benefits

1. ✅ **No more 10MB limit errors** - Can upload videos up to 150MB
2. ✅ **Better UX** - Users see file sizes and get warnings before upload
3. ✅ **Reliable** - Chunked upload handles network interruptions better
4. ✅ **Transparent** - Progress tracking shows upload status
5. ✅ **Safe** - Pre-upload validation prevents wasted bandwidth

## Testing Recommendations

1. Test with small files (<10MB) - should use standard upload
2. Test with medium files (10-50MB) - should use chunked upload
3. Test with large files (50-150MB) - should use chunked upload with warning
4. Test with oversized files (>150MB) - should show error immediately
5. Test with poor network - chunked upload should handle better
6. Test progress tracking - should show accurate percentages

## Notes

- The 150MB limit can be adjusted by changing `MAX_FILE_SIZE` constant
- Cloudinary's chunked upload is more reliable for mobile networks
- File sizes are queried from MediaStore for efficiency
- Camera captures default to 0 bytes initially (size checked during upload)

