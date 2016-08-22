package scroll.xrecyclerviewdemo;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.sql.Ref;
import java.util.ArrayList;
import java.util.List;

import static scroll.xrecyclerviewdemo.IRefreshHeader.*;


/**
 * Created by xiaoning.wang on 16/7/6.
 */

public class XRecyclerView2 extends RecyclerView{
    private Context mContext;
    private boolean isLoadingData = false;
    private boolean isnomore = false;
    private ArrayList<View> mHeaderViews = new ArrayList<>();
    private ArrayList<View> mFootViews = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.Adapter mWrapAdapter;
    private RefreshHeader mRefreshHeader;
    private LoadingMoreFooter mLoadMoreFooter;
    private boolean pullRefreshEnabled = true;
    private boolean loadingMoreEnabled = true;
    private static final int TYPE_REFRESH_HEADER = -5;
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FOOTER = -3;
    private static final int HEADER_INIT_INDEX = 10000;
    private static List<Integer> sHeaderTypes = new ArrayList<>();
    private int previousTotal = 0;
    private int mPageCount = 0;
    //adapter没有数据的时候显示,类似于listView的emptyView
    private View mEmptyView;

    /** 是否要使用下拉刷新功能 **/
    private boolean mIsRefreshable = true;


    /** 实际的padding的距离与界面上偏移距离的比例 **/
    private final static int RATIO = 2;





    /** 开始的Y坐标 **/
    private float mLastY = -1;


    private boolean mIsOnInterceptTouchEvent;

    /** 刷新和更多的事件接口 **/
    private OnPullDownListener mOnPullDownListener;


    /**
     * 刷新和获取更多事件接口
     */
    public interface OnPullDownListener {
        /** 刷新事件接口 这里要注意的是获取更多完 要关闭 刷新的进度条RefreshComplete() **/
        void onRefresh();

        /** 刷新事件接口 这里要注意的是获取更多完 要关闭 更多的进度条 notifyDidMore() **/
        void onMore();
    }

    public XRecyclerView2(Context context) {
        this(context, null);
    }

    public XRecyclerView2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XRecyclerView2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }


    private void init(Context context) {
        mContext = context;
        if (pullRefreshEnabled) {
            RefreshHeader refreshHeader = new RefreshHeader(mContext);
            mHeaderViews.add(0, refreshHeader);
            mRefreshHeader = refreshHeader;
        }
        LoadingMoreFooter footView = new LoadingMoreFooter(mContext);
        footView.setPullDownListener(mOnPullDownListener);
        addFootView(footView);
        mLoadMoreFooter = footView;
        mFootViews.get(0).setVisibility(GONE);
    }
    /**
     * 设置监听器
     *
     * @param listener
     */
    public void setOnPullDownListener(OnPullDownListener listener) {
        mOnPullDownListener = listener;
        mRefreshHeader.setPullDownListener(mOnPullDownListener);
    }

    public void addHeaderView(View view) {
        if (pullRefreshEnabled && !(mHeaderViews.get(0) instanceof RefreshHeader)) {
            RefreshHeader refreshHeader = new RefreshHeader(mContext);
            mHeaderViews.add(0, refreshHeader);
            mRefreshHeader = refreshHeader;
        }
        mHeaderViews.add(view);
        sHeaderTypes.add(HEADER_INIT_INDEX + mHeaderViews.size());
    }

    public void addFootView(final View view) {
        mFootViews.clear();
        mFootViews.add(view);
    }

    public void loadMoreComplete() {
        isLoadingData = false;
        View footView = mFootViews.get(0);
        if (previousTotal < getLayoutManager().getItemCount()) {
            if (footView instanceof LoadingMoreFooter) {
                ((LoadingMoreFooter) footView).setState(LoadingMoreFooter.STATE_COMPLETE);
            } else {
                footView.setVisibility(View.GONE);
            }
        } else {
            if (footView instanceof LoadingMoreFooter) {
                ((LoadingMoreFooter) footView).setState(LoadingMoreFooter.STATE_NOMORE);
            } else {
                footView.setVisibility(View.GONE);
            }
            isnomore = true;
        }
        previousTotal = getLayoutManager().getItemCount();
    }

    public void noMoreLoading() {
        isLoadingData = false;
        View footView = mFootViews.get(0);
        isnomore = true;
        if (footView instanceof LoadingMoreFooter) {
            ((LoadingMoreFooter) footView).setState(LoadingMoreFooter.STATE_NOMORE);
        } else {
            footView.setVisibility(View.GONE);
        }
    }


    public void setRefreshHeader(RefreshHeader refreshHeader) {
        mRefreshHeader = refreshHeader;
    }

    public void setPullRefreshEnabled(boolean enabled) {
        pullRefreshEnabled = enabled;
    }

    public void setLoadingMoreEnabled(boolean enabled) {
        loadingMoreEnabled = enabled;
        if (!enabled) {
            if (mFootViews.size() > 0) {
                mFootViews.get(0).setVisibility(GONE);
            }
        }
    }

    /** 刷新完毕 关闭头部滚动条 **/
    public void refreshComplete() {
        Methods.log("refreshComplete");
        mRefreshHeader.refreshComplete();

    }


    /** 刷新完毕 关闭头部滚动条 **/
    public void refreshError() {
        mRefreshHeader.refreshError();
    }

    public void setEmptyView(View emptyView) {
        this.mEmptyView = emptyView;
        mDataObserver.onChanged();
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        mAdapter = adapter;
        mWrapAdapter = new WrapAdapter(mHeaderViews, mFootViews, adapter);
        super.setAdapter(mWrapAdapter);
        mAdapter.registerAdapterDataObserver(mDataObserver);
        mDataObserver.onChanged();
    }


    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);

        if (state == RecyclerView.SCROLL_STATE_IDLE && mOnPullDownListener != null && !isLoadingData && loadingMoreEnabled) {
            RecyclerView.LayoutManager layoutManager = getLayoutManager();
            int lastVisibleItemPosition;
            if (layoutManager instanceof GridLayoutManager) {
                lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(into);
                lastVisibleItemPosition = findMax(into);
            } else {
                lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            }
            if (layoutManager.getChildCount() > 0
                    && lastVisibleItemPosition >= layoutManager.getItemCount() - 1 && layoutManager.getItemCount() > layoutManager.getChildCount() && !isnomore && mRefreshHeader.getState() < REFRESHING) {

                View footView = mFootViews.get(0);
                isLoadingData = true;
                if (footView instanceof LoadingMoreFooter) {
                    ((LoadingMoreFooter) footView).setState(LoadingMoreFooter.STATE_LOADING);
                } else {
                    footView.setVisibility(View.VISIBLE);
                }
                mOnPullDownListener.onMore();
            }
        }
    }

    @Override
    public boolean
    onTouchEvent(MotionEvent ev) {

        if (!mIsRefreshable)
            return super.onTouchEvent(ev);
        if (mLastY == -1) {
            mLastY = ev.getRawY();
        }
        final int action = ev.getAction();
        cancelLongPress();
        switch (action) {
            case MotionEvent.ACTION_DOWN: { // 按下的时候
                 mLastY = ev.getRawY();   // 在down时候记录当前位置
                mIsOnInterceptTouchEvent = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: { // 手指正在移动的时候
                int tempY = (int) ev.getY();
                if (mIsRefreshable) {
                    final float deltaY = ev.getRawY() - mLastY;
                    mLastY = ev.getRawY();
                    if (isOnTop() && pullRefreshEnabled) {
                        mRefreshHeader.onMove(deltaY / RATIO);

                        if (mRefreshHeader.getVisiableHeight() > 0 ) {
                            Log.d("getVisiableHeight", "getVisiableHeight = " + mRefreshHeader.getVisiableHeight());
                            //Log.d("getVisiableHeight", " mRefreshHeader.getState() = " + mRefreshHeader.getState());
                        }
                    }


                    if (mRefreshHeader.mCurrentState != REFRESHING
                            && mRefreshHeader.mCurrentState != ERROR
                            && mRefreshHeader.mCurrentState != LOADING) {
                        // 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
                        if (mRefreshHeader.mCurrentState == PULL_To_REFRESH) {
                            // 下拉到可以进入RELEASE_TO_REFRESH的状态
                            if (deltaY/ RATIO >= mRefreshHeader.headContentHeight) {
                                mRefreshHeader.changeHeaderViewByState(RELEASE_To_REFRESH);
                                // 由done或者下拉刷新状态转变到松开刷新
                            }
//                            // 上推到顶了
                            else if (deltaY <= 0) {
                                mRefreshHeader.mCurrentState = DONE;
                                mRefreshHeader.changeHeaderViewByState(DONE);
                                // 由DOne或者下拉刷新状态转变到done状态
                                 mIsOnInterceptTouchEvent = false;
                            }
                        }
//
//                        if (mRefreshHeader.mCurrentState == RELEASE_To_REFRESH) {
//                            // 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
//                            if (deltaY / RATIO < mRefreshHeader.headContentHeight
//                                    && deltaY > 0) {
//                                mRefreshHeader.changeHeaderViewByState(PULL_To_REFRESH);
//                            }
//                            // 一下子推到顶了
//                            else if (deltaY <= 0) {
//                                mRefreshHeader.changeHeaderViewByState(DONE);
//                                // 由松开刷新状态转变到done状态;
//                            }
//                            // 往下拉了，或者还没有上推到屏幕顶部掩盖head的地步
//                            else {
//                                // 不用进行特别的操作，只用更新paddingTop的值就行了
//                            }
//                        }

//
                        // done状态下
                        if (mRefreshHeader.mCurrentState == DONE) {
                            if (deltaY > 0) {
                                mRefreshHeader.changeHeaderViewByState(PULL_To_REFRESH);
                            }
                        }
//                        // 更新headView的size
                        if (mRefreshHeader.mCurrentState == PULL_To_REFRESH || mRefreshHeader.mCurrentState == RELEASE_To_REFRESH) {
                            mIsOnInterceptTouchEvent = true;
                            int height = (int) (deltaY / RATIO);

                            // progressView已经完全显示出来，可以开始根据步长获取当前应当显示的帧，并显示出来
                            if (height >= mRefreshHeader.mAnimStartHeight) {
                                mRefreshHeader.setPullingImage(height);

                            }
                            return super.onTouchEvent(ev);
                        }

                        if (mIsOnInterceptTouchEvent && mRefreshHeader.mCurrentState == DONE) {
                            return true;
                        }
                    }
                }
                final int childCount = getChildCount();
                if (childCount == 0) {
                    return super.onTouchEvent(ev);
                }

                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: { // 手指抬起来的时候
                mLastY = -1; // reset

                if (mRefreshHeader.mCurrentState != REFRESHING && mRefreshHeader.mCurrentState != LOADING) {
                    if (mRefreshHeader.mCurrentState == DONE) {
                        // 什么都不做
                    }
                    if (mRefreshHeader.mCurrentState == PULL_To_REFRESH) {
                        mRefreshHeader.changeHeaderViewByState(DONE);
                        // 由下拉刷新状态，到done状态
                    }
                    if (mRefreshHeader.mCurrentState == RELEASE_To_REFRESH) {
                        mRefreshHeader.changeHeaderViewByState(AUTO_FLING);
                        mRefreshHeader.mCanRefleash = true;
                        // 由松开刷新状态，到done状态
                    }
                }
                mIsOnInterceptTouchEvent = false;
                isnomore = false;
                previousTotal = 0;
            }
        }
        return super.onTouchEvent(ev);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!mIsRefreshable)
            return super.dispatchTouchEvent(ev);
//        /**
//         * @author tianlei.gao
//         * @version 5.7
//         * @date 2012.1.5
//         * @describe 当收到MontionEvent.CANCEL事件时，手动触发ListView到IDLE状态
//         *           解决该view收到Cancel事件后
//         *           ，导致回不到IDLE状态引发的问题（置顶按钮不显示、Imagepool下载开关打不开）
//         *
//         * */
//        if (ev.getAction() == MotionEvent.ACTION_CANCEL
//                && (ev.getX() != 0 || ev.getY() != 0)
//                && pauseOnScrollListener != null
//                && pauseOnScrollListener.getCustomListener() != null) {
//            // dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
//            // SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
//            pauseOnScrollListener.getCustomListener().onScrollStateChanged(
//                    this, AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
//            // Log.v("--------------------event cancel--------------------", ""
//            // + System.currentTimeMillis());
//        }
        return super.dispatchTouchEvent(ev);
    }

    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private int findMin(int[] firstPositions) {
        int min = firstPositions[0];
        for (int value : firstPositions) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private boolean isOnTop() {

        if (mHeaderViews == null || mHeaderViews.isEmpty()) {
            return false;
        }

        View view = mHeaderViews.get(0);
        if (view.getParent() != null) {
            return true;
        } else {
            return false;
        }
//        LayoutManager layoutManager = getLayoutManager();
//        int firstVisibleItemPosition;
//        if (layoutManager instanceof GridLayoutManager) {
//            firstVisibleItemPosition = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
//        } else if ( layoutManager instanceof StaggeredGridLayoutManager ) {
//            int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
//            ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(into);
//            firstVisibleItemPosition = findMin(into);
//        } else {
//            firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
//        }
//        if ( firstVisibleItemPosition <= 1 ) {
//             return true;
//        }
//        return false;
    }

    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            RecyclerView.Adapter<?> adapter = getAdapter();
            if (adapter != null && mEmptyView != null) {
                int emptyCount = 0;
                if (pullRefreshEnabled) {
                    emptyCount++;
                }
                if (loadingMoreEnabled) {
                    emptyCount++;
                }
                if (adapter.getItemCount() == emptyCount) {
                    mEmptyView.setVisibility(View.VISIBLE);
                    XRecyclerView2.this.setVisibility(View.GONE);
                } else {
                    mEmptyView.setVisibility(View.GONE);
                    XRecyclerView2.this.setVisibility(View.VISIBLE);
                }
            }
            if (mWrapAdapter != null) {
                mWrapAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mWrapAdapter.notifyItemMoved(fromPosition, toPosition);
        }
    };



    public void setHeadBg(int bgResId){
        mRefreshHeader.setBackgroundColor(mContext.getResources().getColor(bgResId));
    }

    private class WrapAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private RecyclerView.Adapter adapter;

        private ArrayList<View> mHeaderViews;

        private ArrayList<View> mFootViews;

        private int headerPosition = 1;

        public WrapAdapter(ArrayList<View> headerViews, ArrayList<View> footViews, RecyclerView.Adapter adapter) {
            this.adapter = adapter;
            this.mHeaderViews = headerViews;
            this.mFootViews = footViews;
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) manager);
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return (isHeader(position) || isFooter(position))
                                ? gridManager.getSpanCount() : 1;
                    }
                });
            }
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null
                    && lp instanceof StaggeredGridLayoutManager.LayoutParams
                    && (isHeader(holder.getLayoutPosition()) || isFooter(holder.getLayoutPosition()))) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
        }

        public boolean isHeader(int position) {
            return position >= 0 && position < mHeaderViews.size();
        }

        public boolean isContentHeader(int position) {
            return position >= 1 && position < mHeaderViews.size();
        }

        public boolean isFooter(int position) {
            return position < getItemCount() && position >= getItemCount() - mFootViews.size();
        }

        public boolean isRefreshHeader(int position) {
            return position == 0;
        }

        public int getHeadersCount() {
            return mHeaderViews.size();
        }

        public int getFootersCount() {
            return mFootViews.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_REFRESH_HEADER) {
                mCurrentPosition++;
                return new SimpleViewHolder(mHeaderViews.get(0));
            } else if (isContentHeader(mCurrentPosition)) {
                if (viewType == sHeaderTypes.get(mCurrentPosition - 1)) {
                    mCurrentPosition++;
                    return new SimpleViewHolder(mHeaderViews.get(headerPosition++));
                }
            } else if (viewType == TYPE_FOOTER) {
                return new SimpleViewHolder(mFootViews.get(0));
            }
            return adapter.onCreateViewHolder(parent, viewType);
        }

        private int mCurrentPosition;

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (isHeader(position)) {
                return;
            }
            int adjPosition = position - getHeadersCount();
            int adapterCount;
            if (adapter != null) {
                adapterCount = adapter.getItemCount();
                if (adjPosition < adapterCount) {
                    adapter.onBindViewHolder(holder, adjPosition);
                    return;
                }
            }
        }

        @Override
        public int getItemCount() {
            if (adapter != null) {
                return getHeadersCount() + getFootersCount() + adapter.getItemCount();
            } else {
                return getHeadersCount() + getFootersCount();
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (isRefreshHeader(position)) {
                return TYPE_REFRESH_HEADER;
            }
            if (isHeader(position)) {
                position = position - 1;
                return sHeaderTypes.get(position);
            }
            if (isFooter(position)) {
                return TYPE_FOOTER;
            }
            int adjPosition = position - getHeadersCount();
            int adapterCount;
            if (adapter != null) {
                adapterCount = adapter.getItemCount();
                if (adjPosition < adapterCount) {
                    return adapter.getItemViewType(adjPosition);
                }
            }
            return TYPE_NORMAL;
        }

        @Override
        public long getItemId(int position) {
            if (adapter != null && position >= getHeadersCount()) {
                int adjPosition = position - getHeadersCount();
                int adapterCount = adapter.getItemCount();
                if (adjPosition < adapterCount) {
                    return adapter.getItemId(adjPosition);
                }
            }
            return -1;
        }

        @Override
        public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
            if (adapter != null) {
                adapter.unregisterAdapterDataObserver(observer);
            }
        }

        @Override
        public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
            if (adapter != null) {
                adapter.registerAdapterDataObserver(observer);
            }
        }

        private class SimpleViewHolder extends RecyclerView.ViewHolder {
            public SimpleViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

//    public void setRefreshing(boolean refreshing) {
//        if (refreshing && pullRefreshEnabled && mOnPullDownListener != null) {
//            mRefreshHeader.changeHeaderViewByState(RefreshHeader.REFRESHING);
//            mRefreshHeader.onMove(mRefreshHeader.getMeasuredHeight());
//            mOnPullDownListener.onRefresh();
//            isnomore = false;
//            previousTotal = 0;
//        }
//    }

    /** 隐藏头部 禁用下拉更新 **/
    public void setHideHeader() {
        mIsRefreshable = false;
    }
}
