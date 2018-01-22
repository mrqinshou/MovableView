package com.qinshou.movableview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/**
 * Description:可自由移动的控件
 * Created by 禽兽先生
 * Created on 2017/12/20
 */

public class MovableView extends View {
    private int width;  //控件宽
    private int height; //控件高
    private String mvText;  //控件的文字
    private int mvTextSize; //文字大小
    private int mvTextColor;    //文字颜色
    private Drawable mvDrawable;    //控件图片
    private Bitmap mBitmap; //图片转为的 Bitmap
    private Paint mTextPaint;   //文字画笔
    private float lastX, lastY; //最后一次触摸事件的坐标
    private int containerWidth; //该控件的父布局的宽
    private int containerHeight;    //该控件的父布局的高
    private Rect src;   //图片绘制区域
    private Rect dst;   //图片显示区域
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener; //测量父布局宽高的监听器
    private Rect mBound;    //文字绘制区域

    public MovableView(Context context) {
        this(context, null);
    }

    public MovableView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MovableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttribute(context, attrs);
        initPaint();

        src = new Rect();
        dst = new Rect();

        onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewGroup viewGroup = (ViewGroup) getParent();
                if (viewGroup != null) {
                    containerWidth = viewGroup.getMeasuredWidth();
                    containerHeight = viewGroup.getMeasuredHeight();
                }
            }
        };
    }

    private void initAttribute(Context context, AttributeSet attrs) {
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.MovableView);
        //文字
        mvText = mTypedArray.getString(R.styleable.MovableView_mv_text);
        //文字大小
        mvTextSize = mTypedArray.getDimensionPixelSize(R.styleable.MovableView_mv_textSize, 15);
        mvTextSize /= context.getResources().getDisplayMetrics().density;
        //文字颜色
        mvTextColor = mTypedArray.getColor(R.styleable.MovableView_mv_textColor, Color.BLACK);
        //图片
        mvDrawable = mTypedArray.getDrawable(R.styleable.MovableView_mv_drawable);
        if (mvDrawable != null) {
            mBitmap = ((BitmapDrawable) mvDrawable).getBitmap();
        }
        mTypedArray.recycle();
    }

    private void initPaint() {
        mTextPaint = new Paint();
        mTextPaint.setColor(mvTextColor);
        mTextPaint.setTextSize(mvTextSize);
        mTextPaint.setAntiAlias(true);
        if (!TextUtils.isEmpty(mvText)) {
            mBound = new Rect();
            mTextPaint.getTextBounds(mvText, 0, mvText.length(), mBound);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            //如果指定了宽,则使用指定的宽
            width = widthSize;
        } else {
            if (mBitmap != null && TextUtils.isEmpty(mvText)) {
                //如果没有指定宽,当图片和文字都有时,使用两者宽的最大值
                width = Math.max(mBitmap.getWidth(), mBound.width());
            } else if (mBitmap != null) {
                //如果没有指定宽,当只有图片没有文字时,使用图片宽
                width = mBitmap.getWidth();
            } else if (!TextUtils.isEmpty(mvText)) {
                //如果没有指定宽,当只有文字没有图片时,使用文字宽
                width = mBound.width();
            } else {
                //如果没有指定宽,也没有图片和文字时,宽为 0
                width = 0;
            }
        }
        //高同宽
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            if (mBitmap != null && TextUtils.isEmpty(mvText)) {
                height = Math.max(mBitmap.getHeight(), mBound.height());
            } else if (mBitmap != null) {
                height = mBitmap.getHeight();
            } else if (!TextUtils.isEmpty(mvText)) {
                height = mBound.height();
            } else {
                height = 0;
            }
        }
        //设置宽高
        setMeasuredDimension(width, height);
        //获取父布局,并添加测量监听器获取父布局的宽高
        ViewGroup viewGroup = (ViewGroup) getParent();
        if (viewGroup != null) {
            viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
        //如果有图片,则设置图片的绘制和显示区域
        if (mBitmap != null) {
            src.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            dst.set(0, 0, width, height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //有图片先画图片,文字画在图片之上
        if (mvDrawable != null) {
            canvas.drawBitmap(mBitmap, src, dst, null);
        }
        if (!TextUtils.isEmpty(mvText)) {
            canvas.drawText(mvText, (width - mBound.width()) / 2, (height + mBound.height()) / 2, mTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //记录触摸时的坐标,这里为什么要用getRawX()和getRawY()相信理解getX(),getY()和getRawX(),getRawY()的区别就知道为什么了
                lastX = event.getRawX();
                lastY = event.getRawY();
                //return true对事件进行拦截,不继续下发,防止继续响应onClick事件.
                return true;
            case MotionEvent.ACTION_MOVE:
                //每次移动的距离
                float distanceX = event.getRawX() - lastX;
                float distanceY = event.getRawY() - lastY;
                //控件将要移动到的位置
                float nextX = getX() + distanceX;
                float nextY = getY() + distanceY;
                //如果将要移动到的 x 轴坐标小于0,则等于0,防止移出容器左边
                if (nextX < 0) {
                    nextX = 0;
                }
                //防止移出容器右边
                if (nextX > containerWidth - width) {
                    nextX = containerWidth - width;
                }
                //防止移出容器顶边
                if (nextY < 0) {
                    nextY = 0;
                }
                //防止移出容器底边
                if (nextY > containerHeight - height) {
                    nextY = containerHeight - height;
                }
                setX(nextX);
                setY(nextY);
                //移动完之后记录当前坐标
                lastX = event.getRawX();
                lastY = event.getRawY();
                break;
        }
        return false;
    }
}
