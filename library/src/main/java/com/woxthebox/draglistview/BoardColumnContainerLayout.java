package com.woxthebox.draglistview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

public class BoardColumnContainerLayout extends LinearLayout {
    private GestureDetector mGestureDetector;
    private boolean mIsDragging = false;
    private BoardColumnDragListener mBoardColumnDragListener;
    private DragItem mDragItem;
    private float mScrollOffsetX = 0;
    private float mScrollOffsetY = 0;
    private float mStartDraggingDiff = 0;
    private boolean mStartDraggingDiffCalculated = false;

    public BoardColumnContainerLayout(Context context) {
        super(context);

        setOrientation(VERTICAL);
        setupGestureDetector(context);
    }

    private void setupGestureDetector(Context context) {
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (mDragItem != null && !mIsDragging
                        && isPressingOnActiveArea(getEventXPosition(e), getEventYPosition(e))) {
                    mIsDragging = true;
                    mStartDraggingDiffCalculated = false;
                    mBoardColumnDragListener.dragStarted();
                    onStartDrag(e.getX(), e.getY());
                }
                super.onLongPress(e);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (!mIsDragging && isPressingOnActiveArea(getEventXPosition(e), getEventYPosition(e))) {
                    onActiveAreaClick(e);
                }
                return super.onSingleTapUp(e);
            }

            private float getEventXPosition(MotionEvent e) {
                return e.getX() + mScrollOffsetX;
            }

            private float getEventYPosition(MotionEvent e) {
                return e.getY() + mScrollOffsetY;
            }
        });
        mGestureDetector.setIsLongpressEnabled(true);
    }

    protected boolean isPressingOnActiveArea(float xTouch, float yTouch) {
        return xTouch >= getX() && yTouch >= getY()
                && xTouch < getX() + getWidth() && yTouch < getY() + getHeight();
    }

    protected void onActiveAreaClick(MotionEvent e) {
    }

    public void setBoardColumnDragListener(BoardColumnDragListener dragItemListener) {
        mBoardColumnDragListener = dragItemListener;
    }

    public void setDragItem(DragItem dragItem) {
        mDragItem = dragItem;
    }

    public void setScrollOffset(float x, float y) {
        mScrollOffsetX = x;
        mScrollOffsetY = y;
    }

    public boolean isDragging() {
        return mIsDragging;
    }

    public GestureDetector getLongPressGestureDetector() {
        return mGestureDetector;
    }

    public void onStartDrag(float x, float y) {
        getParent().requestDisallowInterceptTouchEvent(false);
        mDragItem.startDrag(this, x, y);
        invalidate();
    }

    public void onDragging(float x, float y) {
        if (!mStartDraggingDiffCalculated) {
            mStartDraggingDiffCalculated = true;
            mStartDraggingDiff = mDragItem.getTouchX() - (x - getLeft()) - getLeft();
        }
        mDragItem.setPosition(mStartDraggingDiff + x, y);
    }

    public void onDragEnded() {
        mIsDragging = false;
        final View endView = this;
        post(new Runnable() {
                 @Override
                 public void run() {
                     mDragItem.endDrag(endView, new AnimatorListenerAdapter() {
                         @Override
                         public void onAnimationEnd(Animator animation) {
                             mDragItem.hide();
                             mDragItem.onEndDragAnimation(endView);
                             mBoardColumnDragListener.dragEnded();
                             super.onAnimationEnd(animation);
                         }
                     });
                 }
        });
    }

    public void addRecyclerView(@NonNull RecyclerView recyclerView) {
        addView(recyclerView);
    }
}
