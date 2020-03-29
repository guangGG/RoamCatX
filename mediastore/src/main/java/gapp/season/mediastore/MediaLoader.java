package gapp.season.mediastore;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 媒体库数据加载过程属于耗时操作，Sync方法都需要放在子线程执行
 */
public class MediaLoader {
    public static final Uri MEDIA_IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final Uri MEDIA_VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    public static final Uri MEDIA_AUDIO_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    public static List<MediaBucket> loadImageBucketsSync(Context context) {
        return getBucketsSync(context, MEDIA_IMAGE_URI);
    }

    public static List<MediaBucket> loadVideoBucketsSync(Context context) {
        return getBucketsSync(context, MEDIA_VIDEO_URI);
    }

    public static List<MediaBucket> loadAudioBucketsSync(Context context) {
        return getBucketsSync(context, MEDIA_AUDIO_URI);
    }

    @NotNull
    private static List<MediaBucket> getBucketsSync(Context context, Uri uri) {
        List<MediaBucket> list = new ArrayList<>();
        Map<String, MediaBucket> map = new HashMap<>();
        String[] projection = {"bucket_id", "bucket_display_name", MediaStore.MediaColumns.DATA, "datetaken"};
        ContentResolver mContentResolver = context.getContentResolver();
        Cursor cursor = mContentResolver.query(uri, projection, null, null, "datetaken desc");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(projection[0]));
                String name = cursor.getString(cursor.getColumnIndex(projection[1]));
                String path = cursor.getString(cursor.getColumnIndex(projection[2]));
                long time = cursor.getLong(cursor.getColumnIndex(projection[3]));
                MediaBucket bucket = map.get(id);
                if (bucket == null) {
                    bucket = new MediaBucket();
                    bucket.id = id;
                    bucket.name = name;
                    bucket.count = 1;
                    bucket.tagPath = path;
                    bucket.tagTime = time;
                    map.put(id, bucket);
                    list.add(bucket);
                } else {
                    bucket.count = bucket.count + 1;
                    //选用最新日期的图片为封面
                    if (time > bucket.tagTime) {
                        bucket.tagPath = path;
                        bucket.tagTime = time;
                    }
                }
            }
            cursor.close();
        }
        return list;
    }

    @NotNull
    public static List<MediaFileItem> getBucketFilesSync(Context context, Uri uri, String bucketId, boolean joinTiteItem) {
        boolean hasDateTaken = !MEDIA_AUDIO_URI.equals(uri); //Audio库没有拍摄时间概念
        List<MediaFileItem> list = new ArrayList<>();
        String[] projection = {MediaStore.MediaColumns.DATA, "datetaken"};
        String[] projectionNoDate = {MediaStore.MediaColumns.DATA};
        String selection = null;
        String[] selectionArgs = null;
        if (!TextUtils.isEmpty(bucketId)) {
            selection = "bucket_id = ?";
            selectionArgs = new String[]{bucketId};
        }
        ContentResolver mContentResolver = context.getContentResolver();
        Cursor cursor = mContentResolver.query(uri, hasDateTaken ? projection : projectionNoDate,
                selection, selectionArgs, hasDateTaken ? "datetaken desc" : null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String lastTag = null;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(projection[0]));
                long time = hasDateTaken ? cursor.getLong(cursor.getColumnIndex(projection[1])) : 0;
                String dateTag = hasDateTaken ? sdf.format(new Date(time)) : "";
                if (joinTiteItem && !dateTag.equals(lastTag)) {
                    //增加标题项
                    MediaFileItem mediaFileItem = new MediaFileItem();
                    mediaFileItem.isTitleTag = true;
                    mediaFileItem.dateTag = dateTag;
                    list.add(mediaFileItem);
                    lastTag = dateTag;
                }
                //增加文件项
                MediaFileItem mediaFileItem = new MediaFileItem();
                mediaFileItem.dateTag = dateTag;
                mediaFileItem.file = new File(path);
                mediaFileItem.takeTime = time;
                list.add(mediaFileItem);
            }
            cursor.close();
        }
        return list;
    }

    /**
     * 获取视频文件的播放时长-ms
     */
    public static long getVideoDuration(String filePath) {
        long duration = 0;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Long.parseLong(durationStr);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return duration;
    }
}
