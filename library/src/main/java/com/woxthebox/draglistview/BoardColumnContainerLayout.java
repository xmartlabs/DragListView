package com.woxthebox.draglistview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;

public class BoardColumnContainerLayout extends LinearLayout {
    public BoardColumnContainerLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    public void addRecyclerView(@NonNull RecyclerView recyclerView) {
        addView(recyclerView);
    }
}
