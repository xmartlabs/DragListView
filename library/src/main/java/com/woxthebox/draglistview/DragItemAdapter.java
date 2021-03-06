/*
 * Copyright 2014 Magnus Woxblom
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.woxthebox.draglistview;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

import java.util.Collections;
import java.util.List;

public abstract class DragItemAdapter<T, VH extends DragItemAdapter.ViewHolder> extends RecyclerView.Adapter<VH> {
    private static final int ITEM_ANIMATION_DURATION = 368;
    private static final float ITEM_INTERPOLATOR_ACCELERATION = 1.6f;

    private DragStartCallback mDragStartCallback;
    private long mDragItemId = RecyclerView.NO_ID;
    private long mDropTargetId = RecyclerView.NO_ID;
    private RecyclerView recyclerView;
    @Nullable
    private Animation itemAnimation;
    protected List<T> mItemList;

    public void setItemList(List<T> itemList) {
        mItemList = itemList;
        notifyDataSetChanged();
    }

    public List<T> getItemList() {
        return mItemList;
    }

    public int getPositionForItem(T item) {
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            if (mItemList.get(i) == item) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    public void animateHeight(final View view, final int height) {
        final int initialHeight = view.getMeasuredHeight();
        Interpolator interpolator = new AccelerateInterpolator(ITEM_INTERPOLATOR_ACCELERATION);

        view.getLayoutParams().height = initialHeight;
        view.requestLayout();

        itemAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (!hasEnded()) {
                    view.getLayoutParams().height = initialHeight - (int) (height * interpolatedTime);
                    view.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }

            @Override
            public boolean hasEnded() {
                boolean hasEnded = super.hasEnded();
                if (hasEnded) {
                    view.getLayoutParams().height = RecyclerView.LayoutParams.WRAP_CONTENT;
                    view.requestLayout();
                }
                return hasEnded;
            }
        };

        itemAnimation.setDuration(ITEM_ANIMATION_DURATION);
        itemAnimation.setInterpolator(interpolator);
        view.startAnimation(itemAnimation);
    }

    public Object removeItem(int pos) {
        if (mItemList != null && mItemList.size() > pos && pos >= 0) {
            View viewToRemove = recyclerView.findViewHolderForAdapterPosition(pos).itemView;
            Object item = mItemList.remove(pos);
            notifyItemRemoved(pos);
            animateHeight((View) viewToRemove.getParent(), viewToRemove.getMeasuredHeight());
            return item;
        }
        return null;
    }

    public void addItem(int pos, T item) {
        if (mItemList != null && mItemList.size() >= pos) {
            if (itemAnimation != null) {
                itemAnimation.cancel();
                itemAnimation = null;
            }
            mItemList.add(pos, item);
            notifyItemInserted(pos);
        }
    }

    public void changeItemPosition(int fromPos, int toPos) {
        if (mItemList != null && mItemList.size() > fromPos && mItemList.size() > toPos) {
            T item = mItemList.remove(fromPos);
            mItemList.add(toPos, item);
            notifyItemMoved(fromPos, toPos);
        }
    }

    public void swapItems(int pos1, int pos2) {
        if (mItemList != null && mItemList.size() > pos1 && mItemList.size() > pos2) {
            Collections.swap(mItemList, pos1, pos2);
            notifyDataSetChanged();
        }
    }

    public int getPositionForItemId(long id) {
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            if (id == getItemId(i)) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public int getItemCount() {
        return mItemList == null ? 0 : mItemList.size();
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        long itemId = getItemId(position);
        holder.mItemId = itemId;
        holder.itemView.setVisibility(mDragItemId == itemId ? View.INVISIBLE : View.VISIBLE);
        holder.setDragStartCallback(mDragStartCallback);
    }

    @Override
    public void onViewRecycled(VH holder) {
        super.onViewRecycled(holder);
        holder.setDragStartCallback(null);
    }

    void setDragStartedListener(DragStartCallback dragStartedListener) {
        mDragStartCallback = dragStartedListener;
    }

    void setDragItemId(long dragItemId) {
        mDragItemId = dragItemId;
    }

    void setDropTargetId(long dropTargetId) {
        mDropTargetId = dropTargetId;
    }

    public long getDropTargetId() {
        return mDropTargetId;
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {
        public View mGrabView;
        public long mItemId;

        private DragStartCallback mDragStartCallback;

        public ViewHolder(final View itemView, int handleResId, boolean dragOnLongPress) {
            super(itemView);
            mGrabView = itemView.findViewById(handleResId);

            if (dragOnLongPress) {
                mGrabView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (mDragStartCallback == null) {
                            return false;
                        }

                        if (mDragStartCallback.startDrag(itemView, mItemId)) {
                            return true;
                        }

                        if (itemView == mGrabView) {
                            return onItemLongClicked(view);
                        }
                        return false;
                    }
                });
            } else {
                mGrabView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        if (mDragStartCallback == null) {
                            return false;
                        }

                        if (event.getAction() == MotionEvent.ACTION_DOWN && mDragStartCallback.startDrag(itemView, mItemId)) {
                            return true;
                        }

                        if (!mDragStartCallback.isDragging() && itemView == mGrabView) {
                            return onItemTouch(view, event);
                        }
                        return false;
                    }
                });
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClicked(view);
                }
            });

            if (itemView != mGrabView) {
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        return onItemLongClicked(view);
                    }
                });
                itemView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        return onItemTouch(view, event);
                    }
                });
            }
        }

        public void setDragStartCallback(DragStartCallback dragStartedListener) {
            mDragStartCallback = dragStartedListener;
        }

        public void onItemClicked(View view) {
        }

        public boolean onItemLongClicked(View view) {
            return false;
        }

        public boolean onItemTouch(View view, MotionEvent event) {
            return false;
        }
    }

    interface DragStartCallback {
        boolean startDrag(View itemView, long itemId);

        boolean isDragging();
    }
}
