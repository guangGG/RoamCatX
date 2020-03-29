package gapp.season.textviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.widget.NestedScrollView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import gapp.season.util.file.FileUtil;

public class VastTextView extends FrameLayout {
    public static final String[] CHARSETS = {"UTF-8", "GBK", "UNICODE", "BIG5", "ISO-8859-1", "ASCII", "UTF-16LE", "UTF-16BE"}; // 常见编码
    private static final int DEFAULT_SINGLE_PAGE_SIZE = 3 * 1024;// 默认分页大小(字节)
    private static final int TYPE_STRING = 0;
    private static final int TYPE_FILE = 1;
    private static final int SCROLL_TYPE_PROGRESS = -1;
    private static final int SCROLL_TYPE_TOP = 0;
    private static final int SCROLL_TYPE_PRE_PAGE = 1;
    private static final int SCROLL_TYPE_NEXT_PAGE = 2;

    private NestedScrollView mScrollView;
    private TextView mTextView;
    private ProgressListener mProgressListener;

    private int mSinglePageSize = DEFAULT_SINGLE_PAGE_SIZE;
    private int mType; //0展示String文本，1展示文件内容
    private String mStringData;
    private File mFileData;
    private String mCharset;// 编码
    private long mTotalDataLength;
    private int mTotalPage;
    private int mCurrentPage;// 当前页index(实际显示两页，这是第一页的index)
    private float mProgress;
    private long mTouchTagTime;

    public VastTextView(@NonNull Context context) {
        super(context);
        initView(context);
        initTypedArray(context, null, 0);
    }

    public VastTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        initTypedArray(context, attrs, 0);
    }

    public VastTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        initTypedArray(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VastTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
        initTypedArray(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView(Context context) {
        View view = View.inflate(context, R.layout.textv_layout, null);
        mScrollView = view.findViewById(R.id.textv_scroll_view);
        mTextView = view.findViewById(R.id.textv_text_view);
        mScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                refreshProgress();
            }
        });
        mScrollView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && System.currentTimeMillis() - mTouchTagTime > 200) {
                    int scrollY = v.getScrollY(); //当前Y位置
                    int height = v.getHeight(); //控件高度
                    int scrollViewMeasuredHeight = mScrollView.getChildAt(0).getMeasuredHeight();
                    if (scrollY <= 0) {
                        // 显示上一页
                        if (mCurrentPage > 0) {
                            //设置了textIsSelectable，使用长按选择文本后，上下页衔接会出现跳动，所以跳页时暂关掉
                            mTextView.setTextIsSelectable(false);
                            loadData(mCurrentPage - 1, SCROLL_TYPE_PRE_PAGE);
                        }
                        mTouchTagTime = System.currentTimeMillis(); //增加此标记防止页面连续跳动
                    }
                    if ((scrollY + height) >= scrollViewMeasuredHeight) {
                        // 显示下一页
                        if (mCurrentPage < mTotalPage - 2) {
                            //设置了textIsSelectable，使用长按选择文本后，上下页衔接会出现跳动，所以跳页时暂关掉
                            mTextView.setTextIsSelectable(false);
                            loadData(mCurrentPage + 1, SCROLL_TYPE_NEXT_PAGE);
                        }
                        mTouchTagTime = System.currentTimeMillis(); //增加此标记防止页面连续跳动
                    }
                }
                return false;
            }
        });
        addView(view);
    }

    private void initTypedArray(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VastTextView, defStyleAttr, 0);
            int pageSize = typedArray.getInt(R.styleable.VastTextView_textv_page_size, 0);
            if (pageSize > 0) {
                mSinglePageSize = pageSize;
            }
            float textSize = typedArray.getDimensionPixelSize(R.styleable.VastTextView_textv_text_size, 0);
            if (textSize > 0) {
                mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            }
            int textColor = typedArray.getColor(R.styleable.VastTextView_textv_text_color, 0);
            if (textColor != 0) {
                mTextView.setTextColor(textColor);
            }
            typedArray.recycle();
        }
    }

    private void loadData() {
        loadData(0, SCROLL_TYPE_TOP);
    }

    private void loadData(int pageId, int scrollType) {
        loadData(pageId, scrollType, 0);
    }

    private void loadData(int pageId, int scrollType, float pageProgress) {
        try {
            InputStream is = null;
            long length = 0;
            int totalPageSize = 0;
            if (mType == TYPE_STRING) {
                if (mStringData == null) {
                    mTextView.setText(null);
                } else {
                    byte[] bytes = mStringData.getBytes(mCharset);
                    is = new ByteArrayInputStream(bytes);
                    length = bytes.length;
                }
            } else if (mType == TYPE_FILE) {
                if (mFileData == null || !mFileData.exists() || mFileData.length() < 1) {
                    mTextView.setText(null);
                } else {
                    is = new FileInputStream(mFileData);
                    length = mFileData.length();
                }
            } else {
                mTextView.setText(null);
            }
            if (is != null) {
                totalPageSize = (int) ((length - 1) / mSinglePageSize + 1);
                if (pageId >= totalPageSize) { //pageId超出范围
                    pageId = 0;
                }
                byte[] bytes = getFileBytes(is, mSinglePageSize * pageId, mSinglePageSize * 2);
                if (bytes != null)
                    mTextView.setText(new String(bytes, mCharset));
                else
                    mTextView.setText(null);
            }
            mTotalDataLength = length;
            mTotalPage = totalPageSize;
            mCurrentPage = pageId;
            //滑动到页面对接位置(这里是不太精确的处理方式)
            if (scrollType == SCROLL_TYPE_TOP) {
                mScrollView.scrollTo(0, 0);
            } else if (scrollType == SCROLL_TYPE_PRE_PAGE) {
                int toY = mScrollView.getChildAt(0).getMeasuredHeight() / 2;
                mScrollView.scrollTo(0, toY);
            } else if (scrollType == SCROLL_TYPE_NEXT_PAGE) {
                int toY = mScrollView.getChildAt(0).getMeasuredHeight() / 2 - mScrollView.getHeight();
                mScrollView.scrollTo(0, toY > 0 ? toY : 0);
            } else if (scrollType == SCROLL_TYPE_PROGRESS) {
                int toY = (int) (mScrollView.getChildAt(0).getMeasuredHeight() / 2 * pageProgress);
                mScrollView.scrollTo(0, toY > 0 ? toY : 0);
            }
            refreshProgress();
            mTextView.setTextIsSelectable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] getFileBytes(InputStream is, long startIndex, int bufferSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        is.skip(startIndex);
        byte[] buffer = new byte[bufferSize];
        int len = is.read(buffer);
        if (len > -1) {
            baos.write(buffer, 0, len);
            byte[] bs = baos.toByteArray();
            is.close();
            baos.close();
            return bs;
        } else {
            return null;
        }
    }

    private void refreshProgress() {
        try {
            int measuredHeight = mScrollView.getChildAt(0).getMeasuredHeight();
            if (measuredHeight > 0 && mTotalPage > 0) {
                float p1 = 1f * mCurrentPage / mTotalPage;
                float p2 = 1f * mScrollView.getScrollY() / measuredHeight / mTotalPage;
                mProgress = p1 + p2;
            } else {
                mProgress = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mProgress = 0;
        }
        //回调进度
        if (mProgressListener != null) {
            mProgressListener.onProgressUpdate(mProgress, mTotalDataLength, mTotalPage, mCurrentPage);
        }
    }


    /**
     * 设置文本数据
     */
    public void setStringData(@Nullable String data) {
        mType = TYPE_STRING;
        mStringData = data;
        mCharset = CHARSETS[0];
        loadData();
    }

    /**
     * 设置文件数据
     */
    public void setFileData(@Nullable File file, String charset) {
        mType = TYPE_FILE;
        mFileData = file;
        mCharset = charset == null ? FileUtil.getFileCharset(file) : charset;
        loadData();
    }

    /**
     * 更换编码
     */
    public void setCharset(String charset) {
        mCharset = charset == null ? FileUtil.getFileCharset(mFileData) : charset;
        loadData();
    }

    /**
     * 跳转到指定进度
     */
    public void toProgress(float progress) {
        try {
            long toPosition = (long) (mTotalDataLength * progress);
            int toPage = (int) ((toPosition + 1) / mSinglePageSize);
            int pagePosition = (int) (toPosition % mSinglePageSize);
            loadData(toPage, SCROLL_TYPE_PROGRESS, 1f * pagePosition / mSinglePageSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setProgressListener(ProgressListener progressListener) {
        mProgressListener = progressListener;
    }

    /**
     * 正在显示中的内容(非全部内容)
     */
    public String getText() {
        return mTextView.getText().toString();
    }

    /**
     * 当前正在展示的全部文本内容
     */
    public String getStringData() {
        if (mType == TYPE_STRING)
            return mStringData;
        else
            return null;
    }

    /**
     * 当前正在展示的文件
     */
    public File getFileData() {
        if (mType == TYPE_FILE)
            return mFileData;
        else
            return null;
    }

    /**
     * 当前正在展示的编码类型(文件对设置的编码类型有效，文本类型内容不关注编码)
     */
    public String getCharset() {
        return mCharset;
    }

    /**
     * 获取当前展示的进度位置
     */
    public float getProgress() {
        return mProgress;
    }

    public interface ProgressListener {
        void onProgressUpdate(float progress, long dataLength, int totalPages, int currentPage);
    }
}
