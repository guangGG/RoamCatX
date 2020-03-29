package gapp.season.nerverabbit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Vector;

public class NerveRabbitView extends SurfaceView implements OnTouchListener, Callback {
    public static final int STATUS_INIT = 0;
    public static final int STATUS_MOVE = 1;
    public static final int STATUS_LOSE = 2;
    public static final int STATUS_WIN = 3;
    public static final int STATUS_GAMEOVER = 4;
    // 默认宽高相同（宽高不同,不影响功能,会影响布局效果）
    private static int ROW = 9; // 设置行数量
    private static int COL = 9; // 设置列数量
    private static int BLOCKS = 10; // 默认的路障数量
    private Paint paint;
    private PlayCallBack playCallBack;// 回调器
    private static int diameter; // 设置圆圈直径（在callback中设置值确保适应不同分辨率的手机）
    private static float blankLeft; // 设置左侧空白高度
    private static float blankTop; // 设置上方空白高度
    private Bitmap bitmap; // 卡通小动物图片（位图）
    private int stap; // 小动物移动的步数
    private Dot matrix[][]; // 所有可操作点阵
    private Dot protagonist; // 小动物所在的点阵

    public NerveRabbitView(Context context) {
        super(context);
        onCreated(context);
    }

    public NerveRabbitView(Context context, AttributeSet attrs) {
        super(context, attrs);
        onCreated(context);
    }

    private void onCreated(Context context) {
        // 获得界面监听器
        getHolder().addCallback(this);
        // 添加动作监听器
        setOnTouchListener(this);
        // 加载小动物的卡通图片
        try {
            // 访问assets资产目录的资源
            // InputStream in = mContext.getAssets().open("nrbt_animal.png");
            // 访问res资源目录的资源
            InputStream in = context.getResources().openRawResource(R.raw.nrbt_animal);
            bitmap = BitmapFactory.decodeStream(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 回调接口
     */
    public interface PlayCallBack {
        /**
         * 步数改变时的回调
         *
         * @param status STATUS_INIT,STATUS_MOVE,STATUS_LOSE,STATUS_WIN,
         *               STATUS_GAMEOVER
         * @param stap   当前的设置的障碍数
         */
        void onPlayChanged(int status, int stap);
    }

    /**
     * 初始化
     *
     * @param width        view宽度
     * @param height       view高度(默认与宽度一致)
     * @param line         行数
     * @param blocks       初始障碍
     * @param playCallBack 数据回调
     */
    public void init(int width, int height, int line, int blocks, PlayCallBack playCallBack) {
        // 设置初始值
        if (line > 0) {
            ROW = line;
            COL = line;
        }
        if (blocks >= 0)
            BLOCKS = blocks;
        this.playCallBack = playCallBack;
        refreshSize(width, height);
        // 初始化点阵数组
        matrix = new Dot[ROW][COL];
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                matrix[i][j] = new Dot(j, i);
            }
        }
        initGame(); // 初始化一盘游戏
        refreshCanvas(); // 更新界面画布
        // 回调初始化状态
        if (this.playCallBack != null)
            this.playCallBack.onPlayChanged(STATUS_INIT, 0);
    }

    /**
     * 游戏初始化:初始化小动物的位置和随机分配初始路障的位置
     */
    private void initGame() {
        // 初始化点阵状态
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                matrix[i][j].setStatus(Dot.STATUS_OFF);
            }
        }
        // 初始化小动物的位置和状态
        protagonist = new Dot(COL / 2, ROW / 2);
        getDot(protagonist.getX(), protagonist.getY()).setStatus(Dot.STATUS_IN);
        // 随机初始化路障位置
        for (int i = 0; i < BLOCKS; ) {
            int x = (int) ((Math.random() * 1000) % COL);
            int y = (int) ((Math.random() * 1000) % ROW);
            if (getDot(x, y).getStatus() == Dot.STATUS_OFF) {
                getDot(x, y).setStatus(Dot.STATUS_ON);
                i++;
            }
        }
        // 初始化步数（小动物移动的步数）
        stap = 0;
    }

    /**
     * 根据位置获取动作点
     */
    private Dot getDot(int x, int y) {
        return matrix[y][x];
    }

    /**
     * 判断点是否在边界
     */
    private boolean isAtEdge(Dot d) {
        return d.getX() * d.getY() == 0 || d.getX() + 1 == COL || d.getY() + 1 == ROW;
    }

    /**
     * 获得指定点的周围6个点中的某个点
     */
    private Dot getNeighbour(Dot one, int dir) {
        switch (dir) {
            case 1:
                return getDot(one.getX() - 1, one.getY());
            case 2:
                if (one.getY() % 2 == 0) {
                    return getDot(one.getX() - 1, one.getY() - 1);
                } else {
                    return getDot(one.getX(), one.getY() - 1);
                }
            case 3:
                if (one.getY() % 2 == 0) {
                    return getDot(one.getX(), one.getY() - 1);
                } else {
                    return getDot(one.getX() + 1, one.getY() - 1);
                }
            case 4:
                return getDot(one.getX() + 1, one.getY());
            case 5:
                if (one.getY() % 2 == 0) {
                    return getDot(one.getX(), one.getY() + 1);
                } else {
                    return getDot(one.getX() + 1, one.getY() + 1);
                }
            case 6:
                if (one.getY() % 2 == 0) {
                    return getDot(one.getX() - 1, one.getY() + 1);
                } else {
                    return getDot(one.getX(), one.getY() + 1);
                }

            default:
                break;
        }
        return null;
    }

    /**
     * 获得周围6条路线某条路线的的路程（若未能通到边界外，乘以-1）
     */
    private int getDistance(Dot one, int dir) {
        int distance = 0;
        if (isAtEdge(one)) {
            return 1; // 防止代码出现异常，本应为0，为区分开挨着路障的情况故设为1
        }
        Dot ori = one, next;
        while (true) {
            next = getNeighbour(ori, dir);
            if (next != null && next.getStatus() == Dot.STATUS_ON) {
                return distance * -1; // 碰到路障返回路程的负数
            }
            if (next != null && isAtEdge(next)) {
                distance++;
                return distance; // 通到界外返回路程
            }
            distance++;
            ori = next;
        }
    }

    /**
     * 小动物移动的逻辑
     */
    @SuppressWarnings("ConstantConditions")
    private void move() {
        if (isAtEdge(protagonist)) {
            // 小动物到边界了，这里说明小动物已经直接跑出去了，小动物停止思考
            if (playCallBack != null)
                playCallBack.onPlayChanged(STATUS_GAMEOVER, 0);
            return;
        }
        Vector<Dot> available = new Vector<>(); // 小动物的可移动路径集合
        Vector<Dot> positive = new Vector<>(); // 小动物可以直接移动向边界的路径集合
        HashMap<Dot, Integer> al = new HashMap<>(); // 小动物的可移动路径及此路径路程Map
        for (int i = 1; i < 7; i++) {
            Dot n = getNeighbour(protagonist, i);
            if (n != null && n.getStatus() == Dot.STATUS_OFF) {
                available.add(n);
                al.put(n, i);
                if (getDistance(n, i) > 0) {
                    positive.add(n);
                }
            }
        }
        if (available.size() == 0) {
            // 小动物的可移动路径数量为0，就赢了
            win();
            return;
        } else if (available.size() == 1) {
            MoveTo(available.get(0)); // 小动物只有一条路径，直接移动过去
        } else {
            Dot best = null;
            if (positive.size() != 0) { // 存在可以直接到达屏幕边缘的走向,直接走向最短的路径
                // System.out.println("向前进");
                int min = 999;
                for (int i = 0; i < positive.size(); i++) {
                    int a = getDistance(positive.get(i), al.get(positive.get(i)));
                    if (a < min) {
                        min = a;
                        best = positive.get(i);
                    }
                }
                if (best != null) MoveTo(best);
            } else { // 所有方向都存在路障，走向离路障最远的路径
                // System.out.println("躲路障");
                int max = 0;
                for (int i = 0; i < available.size(); i++) {
                    int k = getDistance(available.get(i), al.get(available.get(i)));
                    if (k <= max) {
                        max = k;
                        best = available.get(i);
                    }
                }
                if (best != null) MoveTo(best);
            }
        }
        // 小动物移动的步数累加
        stap++;
        // 回调
        if (playCallBack != null)
            playCallBack.onPlayChanged(STATUS_MOVE, stap);
        // 小动物移动后判断是否移动到了边界
        if (isAtEdge(protagonist)) {
            lose();
        }
    }

    /**
     * 小动物移到周围的某个点
     */
    private void MoveTo(Dot one) {
        one.setStatus(Dot.STATUS_IN);
        getDot(protagonist.getX(), protagonist.getY()).setStatus(Dot.STATUS_OFF);
        protagonist.setXY(one.getX(), one.getY());
    }

    /**
     * 没捉住小动物
     */
    private void lose() {
        stap++;// 小动物跑出去
        // 回调
        if (playCallBack != null)
            playCallBack.onPlayChanged(STATUS_LOSE, stap - 1);
    }

    /**
     * 捉住了小动物
     */
    private void win() {
        if (stap != 0) {
            // 设置小动物移动一步以上捉住才算胜利，以防止胜利次数不断累加(回调)
            if (playCallBack != null)
                playCallBack.onPlayChanged(STATUS_WIN, stap + 1);
        } else {
            // 如果stap为0，说明是赢了以后又触发了onTouch方法，就不再累加胜利次数和提示胜利信息(回调)
            if (playCallBack != null)
                playCallBack.onPlayChanged(STATUS_GAMEOVER, 0);
        }
        stap = 0; // 赢了初始化stap
    }

    /**
     * 更新界面画布
     */
    private void refreshCanvas() {
        try {
            Canvas c = getHolder().lockCanvas(); // 绑定画布
            if (c == null) return;
            c.drawColor(Color.LTGRAY); // 填充底色
            if (paint == null) {
                paint = new Paint(); // 创建画笔
                paint.setFlags(Paint.ANTI_ALIAS_FLAG); // 设置画布图像无锯齿
            }
            for (int i = 0; i < ROW; i++) {
                float offset = blankLeft; // 点阵左侧空白
                if (i % 2 != 0) {
                    offset = blankLeft + diameter / 2.0f;
                }
                // 画出点阵图案
                for (int j = 0; j < COL; j++) {
                    Dot one = getDot(j, i);
                    switch (one.getStatus()) {
                        case Dot.STATUS_OFF:
                            paint.setColor(0XFFF0F0F0);
                            c.drawOval(new RectF(one.getX() * diameter + offset, one.getY() * diameter + blankTop, (one.getX() + 1) * diameter + offset, (one.getY() + 1) * diameter
                                    + blankTop), paint);
                            break;
                        case Dot.STATUS_ON:
                            paint.setColor(0xFFFFA500);
                            c.drawOval(new RectF(one.getX() * diameter + offset, one.getY() * diameter + blankTop, (one.getX() + 1) * diameter + offset, (one.getY() + 1) * diameter
                                    + blankTop), paint);
                            break;
                        case Dot.STATUS_IN:
                            paint.setColor(0xFF87CEFA);
                            c.drawOval(new RectF(one.getX() * diameter + offset, one.getY() * diameter + blankTop, (one.getX() + 1) * diameter + offset, (one.getY() + 1) * diameter
                                    + blankTop), paint);
                            // 小动物的位置画出小动物的位图图片（设置稍微大点，偏上点）
                            c.drawBitmap(bitmap, null, new RectF((one.getX() - 0.2f) * diameter + offset, (one.getY() - 0.4f) * diameter + blankTop, (one.getX() + 1.2f) * diameter
                                    + offset, (one.getY() + 1.0f) * diameter + blankTop), null);
                            break;
                        default:
                            break;
                    }
                }
            }
            // 解锁画布并呈现到显示屏上
            getHolder().unlockCanvasAndPost(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置界面图标大小
     */
    private void refreshSize(int width, int height) {
        // width为View宽度像素，height为View高度像素
        diameter = Math.min(width, height) / (COL + 1); // 设置WIDTH为屏幕宽度除以（列数+1）
        blankLeft = diameter * 0.25f;
        blankTop = diameter * 0.5f;
    }

    /**
     * 界面创建
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    /**
     * 界面切换过来时执行
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        refreshSize(width, height);
        refreshCanvas(); // 更新界面画布
    }

    /**
     * 界面销毁
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    /**
     * 点击事件
     */
    @Override
    public boolean onTouch(View arg0, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) { // 手点击离开屏幕的事件时
            int x, y;
            y = (int) ((e.getY() - blankTop) / diameter); // float强转int时（-1~1）之间的数转换结果都是0
            if (y % 2 == 0) {
                x = (int) ((e.getX() - blankLeft) / diameter);
            } else {
                x = (int) ((e.getX() - diameter / 2 - blankLeft) / diameter);
            }
            if ((0 <= x && x < COL) && (0 <= y && y < ROW)) { // 判断在点阵范围内时
                if (getDot(x, y).getStatus() == Dot.STATUS_OFF) {
                    getDot(x, y).setStatus(Dot.STATUS_ON);
                    move(); // 小动物移动一步
                }
            }
            refreshCanvas(); // 更新画布
        }
        return true;
    }

    // 默认使布局高度与宽度保持一致(以宽度为准)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取view设置的宽高
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        //设置wrap_content的默认宽 / 高值
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int widthScreen = wm.getDefaultDisplay().getWidth();
        int heightScreen = wm.getDefaultDisplay().getHeight();
        int size = Math.min(widthScreen, heightScreen);
        //当布局参数设置为wrap_content时，设置默认值
        if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(size, size);
        } else if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(size, heightSize);
        } else if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(widthSize, size);
        }
    }

    /**
     * 圆点，有三种状态
     */
    public class Dot {
        private int x, y;
        private int status;

        static final int STATUS_ON = 1;
        static final int STATUS_OFF = 0;
        static final int STATUS_IN = 9;

        Dot(int x, int y) {
            super();
            this.x = x;
            this.y = y;
            status = STATUS_OFF;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        int getStatus() {
            return status;
        }

        void setStatus(int status) {
            this.status = status;
        }

        void setXY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
