# Testing Checklist for Large File Upload Feature

## Pre-Testing Setup
- [ ] Ensure you have test media files of various sizes:
  - Small image: < 5MB
  - Medium image: 5-10MB
  - Large video: 15-30MB
  - Very large video: 50-100MB
  - Oversized video: > 150MB

## Test Cases

### 1. Small File Upload (< 10MB)
- [ ] Select a small image or short video (< 10MB)
- [ ] Add a caption
- [ ] Click "Post"
- [ ] **Expected**: Upload completes successfully using standard upload
- [ ] Check logcat for: `Using standard upload`
- [ ] Verify post appears in feed

### 2. Medium File Upload (10-50MB)
- [ ] Select a medium video (10-50MB)
- [ ] Add a caption
- [ ] Click "Post"
- [ ] **Expected**: 
  - Warning toast: "Uploading X.X MB. This may take a while."
  - Upload progresses with notification
  - Logcat shows: `Using chunked upload for large file`
  - Upload completes successfully
- [ ] Verify post appears in feed

### 3. Large File Upload (50-150MB)
- [ ] Select a large video (50-150MB)
- [ ] Add a caption
- [ ] Click "Post"
- [ ] **Expected**:
  - Warning toast about large file size
  - Chunked upload with progress tracking
  - Multiple chunks uploaded (check logcat)
  - Upload completes successfully
- [ ] Verify post appears in feed with correct video

### 4. Oversized File Rejection (> 150MB)
- [ ] Select a file larger than 150MB
- [ ] Add a caption
- [ ] Click "Post"
- [ ] **Expected**:
  - Error toast: "File too large: X.X MB. Max 150MB per file."
  - Upload does NOT start
  - User remains on caption screen
- [ ] Verify no upload service started

### 5. Multiple Files Mixed Sizes
- [ ] Select multiple files with mixed sizes (e.g., 5MB + 20MB + 40MB)
- [ ] Add a caption
- [ ] Click "Post"
- [ ] **Expected**:
  - Each file uploads using appropriate method (standard or chunked)
  - Progress updates for each file
  - All files upload successfully
- [ ] Verify post appears with all media

### 6. File Size Display (Optional Enhancement)
- [ ] Open create post picker
- [ ] Observe media grid
- [ ] **Expected**: File sizes should be visible (if implemented in UI)

### 7. Network Interruption Handling
- [ ] Start uploading a large file (30-50MB)
- [ ] Turn off Wi-Fi/data mid-upload
- [ ] Wait a few seconds
- [ ] Turn network back on
- [ ] **Expected**: Upload should fail gracefully with error message
- [ ] Verify user can retry upload

### 8. Progress Tracking Accuracy
- [ ] Upload a large file (50MB+)
- [ ] Watch the notification progress
- [ ] **Expected**:
  - Progress starts at 0%
  - Updates throughout upload
  - Reaches 100% when complete
  - Progress is relatively smooth (no big jumps)

### 9. Camera Capture
- [ ] Use camera to take a photo
- [ ] Add caption
- [ ] Click "Post"
- [ ] **Expected**: Upload works normally (size validated during upload)

### 10. Edge Cases
- [ ] Try selecting media from external storage
- [ ] Try selecting media from different sources (Downloads, WhatsApp, etc.)
- [ ] **Expected**: File size detection works for all sources

## Logcat Commands

### View CloudinaryUploader logs:
```bash
adb logcat -s CloudinaryUploader:D
```

### View all upload-related logs:
```bash
adb logcat | grep -i "upload\|cloudinary"
```

### View specific chunk upload logs:
```bash
adb logcat -s CloudinaryUploader:D | grep "chunk"
```

## Success Criteria

✅ All test cases pass
✅ No crashes or force closes
✅ Appropriate error messages shown to users
✅ Large files upload successfully using chunked upload
✅ Files > 150MB are rejected before upload starts
✅ Progress tracking works accurately
✅ Posts appear correctly in feed after upload

## Known Limitations

- Camera captures show 0 bytes initially (size is validated during upload)
- Chunked uploads are sequential, not parallel (for reliability)
- Network interruptions may require manual retry
- Very large files (100MB+) may take several minutes on slow connections

## Debugging Tips

If upload fails:
1. Check logcat for `CloudinaryUploader` logs
2. Verify Cloudinary preset `Socially_CDN` is configured correctly
3. Check network connectivity
4. Ensure file is not corrupted
5. Verify file size is within limits

If chunked upload fails:
1. Check `X-Unique-Upload-Id` header is being sent
2. Verify `Content-Range` header format
3. Ensure all chunks use the same upload ID
4. Check Cloudinary account supports chunked uploads

