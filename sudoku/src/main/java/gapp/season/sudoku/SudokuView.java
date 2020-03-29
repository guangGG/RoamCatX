package gapp.season.sudoku;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class SudokuView extends View {
    private Paint mPaint;
    private float mMainLineWidth;
    private float mDivideLineWidth;
    private int mBgColor;
    private int mStressBgColor;
    private int mMainLineColor;
    private int mDivideLineColor;
    private int mTextColor;
    private int mOriginalColor;
    private int mHintColor;
    private GestureDetector mGestureDetector;

    private List<SudokuCell> mCells;
    private SudokuCellClickListener mCellClickListener;

    public SudokuView(Context context) {
        super(context);
        initView(context);
    }

    public SudokuView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        mDivideLineWidth = density;
        mMainLineWidth = density * 2;
        mBgColor = Color.WHITE;
        mStressBgColor = 0xfff0f0f0;
        mMainLineColor = 0xff888888;
        mDivideLineColor = 0xffaaaaaa;
        mTextColor = 0xff666666;
        mOriginalColor = 0xff6666FF;
        mHintColor = 0xff999999;

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                int cellIndex = getCellIndex(e.getX(), e.getY());
                if (cellIndex >= 0 && mCellClickListener != null) {
                    return mCellClickListener.onClick(SudokuView.this, cellIndex, false);
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                int cellIndex = getCellIndex(e.getX(), e.getY());
                if (cellIndex >= 0 && mCellClickListener != null) {
                    mCellClickListener.onClick(SudokuView.this, cellIndex, true);
                }
            }
        });
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mGestureDetector != null)
                    return mGestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });
    }

    public void setCells(List<SudokuCell> cells) {
        mCells = cells;
        postInvalidate();
    }

    public void setCellClickListener(SudokuCellClickListener cellClickListener) {
        mCellClickListener = cellClickListener;
    }

    @NonNull
    private Paint getPaint() {
        if (mPaint == null)
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG); //设置画布图像无锯齿
        return mPaint;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取view设置的宽高
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        //设置wrap_content的默认宽 / 高值
        int size = 1080;
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            int widthScreen = wm.getDefaultDisplay().getWidth();
            int heightScreen = wm.getDefaultDisplay().getHeight();
            size = Math.min(widthScreen, heightScreen);
        }
        //当布局参数设置为wrap_content时，设置默认值
        if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(size, size);
        } else if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(size, heightSize);
        } else if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(widthSize, size);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            int width = getWidth();
            int height = getHeight();
            int pl = getPaddingLeft();
            int pt = getPaddingTop();
            int pr = getPaddingRight();
            int pb = getPaddingBottom();
            float size = Math.min((width - pl - pr), (height - pt - pb));
            if (size > 0) {
                float cellSize = (size - mMainLineWidth * 4 - mDivideLineWidth * 6) / 9;
                //draw背景色
                getPaint().setColor(mBgColor);
                canvas.drawColor(mBgColor);
                float l = 0;
                if (mStressBgColor != mBgColor) {
                    getPaint().setColor(mStressBgColor);
                    float ssize = cellSize * 3 + mMainLineWidth + mDivideLineWidth * 2;
                    for (int i = 0; i < 9; i++) {
                        if (i % 2 == 0) {
                            int dx = i % 3;
                            int dy = i / 3;
                            canvas.drawRect(pl + dx * ssize, pt + dy * ssize,
                                    pl + (dx + 1) * ssize, pt + (dy + 1) * ssize, getPaint());
                        }
                    }
                }
                //draw线条
                for (int i = 0; i < 9; i++) {
                    if (i % 3 == 0) {
                        getPaint().setColor(mMainLineColor);
                        canvas.drawRect(l + pl, pt, l + pl + mMainLineWidth, size + pt, getPaint());
                        canvas.drawRect(pl, l + pt, size + pl, l + pt + mMainLineWidth, getPaint());
                        l += mMainLineWidth;
                    } else {
                        getPaint().setColor(mDivideLineColor);
                        canvas.drawRect(l + pl, pt, l + pl + mDivideLineWidth, size + pt, getPaint());
                        canvas.drawRect(pl, l + pt, size + pl, l + pt + mDivideLineWidth, getPaint());
                        l += mDivideLineWidth;
                    }
                    l += cellSize;
                }
                getPaint().setColor(mMainLineColor);
                canvas.drawRect(l + pl, pt, l + pl + mMainLineWidth, size + pt, getPaint());
                canvas.drawRect(pl, l + pt, size + pl, l + pt + mMainLineWidth, getPaint());
                //draw数字
                if (mCells != null && mCells.size() == 81) {
                    for (int i = 0; i < 81; i++) {
                        if (!mCells.get(i).isEmpty()) {
                            RectF rect = getRect(pl, pt, mMainLineWidth, mDivideLineWidth, cellSize, i);
                            drawCell(canvas, mCells.get(i), rect, cellSize);
                        }
                    }
                }
            } else {
                super.onDraw(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawCell(Canvas canvas, SudokuCell sudokuCell, RectF rect, float cellSize) {
        if (sudokuCell.getNumber() > 0) {
            getPaint().setColor(sudokuCell.isOriginal() ? mOriginalColor : mTextColor);
            getPaint().setTextSize(cellSize * 0.5f);
            canvas.drawText(String.valueOf(sudokuCell.getNumber()), rect.left + cellSize * 0.35f, rect.top + cellSize * 0.7f, getPaint());
        } else if (sudokuCell.getHintNumbers() != null && sudokuCell.getHintNumbers().size() > 0) {
            getPaint().setColor(mHintColor);
            getPaint().setTextSize(cellSize * 0.3f);
            for (int i = 0; i < 9; i++) {
                if (sudokuCell.getHintNumbers() != null && sudokuCell.getHintNumbers().contains(i + 1)) {
                    int dx = (i % 3);
                    int dy = (i / 3);
                    float fx = 0.3333f * dx + 0.09f;
                    float fy = 0.3333f * dy + 0.27f;
                    canvas.drawText(String.valueOf(i + 1), rect.left + cellSize * fx, rect.top + cellSize * fy, getPaint());
                }
            }
        }
    }

    private RectF getRect(int paddingLeft, int paddingTop, float mainLineWidth, float divideLineWidth, float cellSize, int index) {
        float dx = paddingLeft + mainLineWidth;
        float dy = paddingTop + mainLineWidth;
        for (int i = 0; i < index % 9; i++) {
            dx += cellSize;
            if (i % 3 == 2) {
                dx += mainLineWidth;
            } else {
                dx += divideLineWidth;
            }
        }
        for (int j = 0; j < index / 9; j++) {
            dy += cellSize;
            if (j % 3 == 2) {
                dy += mainLineWidth;
            } else {
                dy += divideLineWidth;
            }
        }
        return new RectF(dx, dy, dx + cellSize, dy + cellSize);
    }

    private int getCellIndex(float x, float y) {
        int cx = getWidth() - getPaddingLeft() - getPaddingRight();
        int cy = getHeight() - getPaddingTop() - getPaddingBottom();
        int ix = (int) ((x - getPaddingLeft()) / cx / 0.1111f);
        int iy = (int) ((y - getPaddingTop()) / cy / 0.1111f);
        if (ix >= 0 && ix < 9 && iy >= 0 && iy < 9) {
            return iy * 9 + ix;
        }
        return -1;
    }

    public interface SudokuCellClickListener {
        boolean onClick(View view, int cellIndex, boolean longClick);
    }
}
