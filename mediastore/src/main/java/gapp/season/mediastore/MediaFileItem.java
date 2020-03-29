package gapp.season.mediastore;

import java.io.File;

/**
 * 图片/视频文件
 */
public class MediaFileItem {
    boolean isTitleTag; //标记是否分组标题项
    String dateTag; //日期标志(按日期分组)，格式：20180808
    File file;
    long takeTime; //拍摄时间
    //long duration; //视频播放时间-ms
}
