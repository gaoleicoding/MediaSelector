package com.aliyun.svideo.common.baseAdapter.decoration;

import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;


import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliyun.svideo.common.baseAdapter.BaseSectionQuickAdapter;
import com.aliyun.svideo.common.baseAdapter.BaseViewHolder;
import com.aliyun.svideo.common.baseAdapter.entity.SectionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用于RecyclerView的GridLayoutManager，水平方向上固定间距大小，从而使条目宽度自适应。<br>
 * 配合Brvah的Section使用，不对Head生效，仅对每个Head的子Grid列表生效<br>
 * Section Grid中Item的宽度应设为MATCH_PARAENT
 */
public class GridSectionAverageGapItemDecoration extends RecyclerView.ItemDecoration {

    private class Section {
        public int startPos = 0;
        public int endPos = 0;

        public int getCount() {
            return endPos - startPos + 1;
        }

        public boolean contains(int pos) {
            return pos >= startPos && pos <= endPos;
        }

        @Override
        public String toString() {
            return "Section{" +
                   "startPos=" + startPos +
                   ", endPos=" + endPos +
                   '}';
        }
    }

    private float gapHorizontalDp;
    private float gapVerticalDp;
    private float sectionEdgeHPaddingDp;
    private float sectionEdgeVPaddingDp;
    private int gapHSizePx = -1;
    private int gapVSizePx = -1;
    private int sectionEdgeHPaddingPx;
    private int eachItemHPaddingPx; //每个条目应该在水平方向上加的padding 总大小，即=paddingLeft+paddingRight
    private int sectionEdgeVPaddingPx;
    private List<Section> mSectionList = new ArrayList<>();
    private BaseSectionQuickAdapter mAdapter;
    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            markSections();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            markSections();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            markSections();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            markSections();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            markSections();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            markSections();
        }
    };


    /**
     * @param gapHorizontalDp       item之间的水平间距
     * @param gapVerticalDp         item之间的垂直间距
     * @param sectionEdgeHPaddingDp section左右两端的padding大小
     * @param sectionEdgeVPaddingDp section上下两端的padding大小
     */
    public GridSectionAverageGapItemDecoration(float gapHorizontalDp, float gapVerticalDp, float sectionEdgeHPaddingDp, float sectionEdgeVPaddingDp) {
        this.gapHorizontalDp = gapHorizontalDp;
        this.gapVerticalDp = gapVerticalDp;
        this.sectionEdgeHPaddingDp = sectionEdgeHPaddingDp;
        this.sectionEdgeVPaddingDp = sectionEdgeVPaddingDp;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            parent.getAdapter();
        }
        super.getItemOffsets(outRect, view, parent, state);
    }

    private void setUpWithAdapter(BaseSectionQuickAdapter<SectionEntity, BaseViewHolder> adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mDataObserver);
        }
        mAdapter = adapter;
        mAdapter.registerAdapterDataObserver(mDataObserver);
        markSections();
    }

    private void markSections() {
        if (mAdapter != null) {
            BaseSectionQuickAdapter<SectionEntity, BaseViewHolder> adapter = mAdapter;
            mSectionList.clear();
            SectionEntity sectionEntity = null;
            Section section = new Section();
            for (int i = 0, size = adapter.getItemCount(); i < size; i++) {
                sectionEntity = adapter.getItem(i);
                if (sectionEntity != null && sectionEntity.isHeader) {
                    //找到新Section起点
                    if (section != null && i != 0) {
                        //已经有待添加的section
                        section.endPos = i - 1;
                        mSectionList.add(section);
                    }
                    section = new Section();
                    section.startPos = i + 1;
                } else {
                    section.endPos = i;
                }
            }
            //处理末尾情况
            if (!mSectionList.contains(section)) {
                mSectionList.add(section);
            }

//            Log.w("GridAverageGapItem", "section list=" + mSectionList);
        }
    }

    private void transformGapDefinition(RecyclerView parent, int spanCount) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            parent.getDisplay().getMetrics(displayMetrics);
        }
        gapHSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, gapHorizontalDp, displayMetrics);
        gapVSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, gapVerticalDp, displayMetrics);
        sectionEdgeHPaddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sectionEdgeHPaddingDp, displayMetrics);
        sectionEdgeVPaddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sectionEdgeVPaddingDp, displayMetrics);
        eachItemHPaddingPx = (sectionEdgeHPaddingPx * 2 + gapHSizePx * (spanCount - 1)) / spanCount;
    }

    private Section findSectionLastItemPos(int curPos) {
        for (Section section : mSectionList) {
            if (section.contains(curPos)) {
                return section;
            }
        }
        return null;
    }

    private boolean isLastRow(int visualPos, int spanCount, int sectionItemCount) {
        int lastRowCount = sectionItemCount % spanCount;
        lastRowCount = lastRowCount == 0 ? spanCount : lastRowCount;
        return visualPos > sectionItemCount - lastRowCount;
    }
}
