package scroll.xrecyclerviewdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LoadingMoreFooter extends LinearLayout implements ILoadMore{

    private Context mContext;
    public final static int STATE_LOADING = 0;
    public final static int STATE_COMPLETE = 1;
    public final static int STATE_NOMORE = 2;

    public boolean mIsFetchMoreing; // 是否获取更多中

    private boolean mIsNoMore; //无更多内容


    private int mFooterPadding;



    /** 刷新和更多的事件接口 **/
    private XRecyclerView2.OnPullDownListener mOnPullDownListener;

    /** 底部更多的按键 **/
    private RelativeLayout mFooterView;
    /** 底部更多的按键 **/
    private TextView mFooterTextView;
    private ProgressBar mFooterLoadingView;

	public LoadingMoreFooter(Context context) {
		super(context);
		initView(context);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public LoadingMoreFooter(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}
    public void initView(Context context ){
        mFooterPadding = 0;
        mContext = context;
        setGravity(Gravity.CENTER);
        /* 自定义底部文件 */
        mFooterView = (RelativeLayout) LayoutInflater.from(context).inflate(
                R.layout.common_pulldown_footer, null);
        mFooterView.setBackgroundColor(getResources().getColor(R.color.home_free_bg));
        mFooterTextView = (TextView) mFooterView
                .findViewById(R.id.pulldown_footer_text);
        mFooterLoadingView = (ProgressBar) mFooterView
                .findViewById(R.id.pulldown_footer_loading);
        mFooterView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsNoMore){ //如果当前加载更多布局显示的是“无更多内容”则屏蔽点击事件
                    return;
                }
                if (!mIsFetchMoreing) {
                    mIsFetchMoreing = true;
                    mFooterLoadingView.setVisibility(View.VISIBLE);
                    mFooterTextView.setText(mContext.getResources().getString(
                            R.string.load_more_item_layout_1));
                    Methods.log( "set has more content L1213");
                    if(mOnPullDownListener != null){
                        mOnPullDownListener.onMore();
                    }
                }
            }
        });

		/* ScrollOverListView 同样是考虑到都是使用，所以放在这里 同时因为，需要它的监听事件 */
        setHideFooter();// 默认关闭底部加载更多按钮
        addView(mFooterView);
    }

    public void  setState(int state) {
        switch(state) {
            case STATE_LOADING:
                mIsFetchMoreing = true;
                mFooterLoadingView.setVisibility(View.VISIBLE);
                 mFooterTextView.setText("Loading");
                this.setVisibility(View.VISIBLE);
                    break;
            case STATE_COMPLETE:
                mIsFetchMoreing = false;
                notifyLoadMoreComplete();
                mFooterTextView.setVisibility(View.VISIBLE);
                this.setVisibility(View.GONE);
                mFooterTextView.setText(mContext.getResources().getString(
                        R.string.load_more_item_layout_1));
                break;
            case STATE_NOMORE:
                mIsFetchMoreing = false;
                mFooterTextView.setText(mContext.getResources().getString(
                        R.string.load_no_more_comments_item_layout_1));
                mFooterLoadingView.setVisibility(View.GONE);
                this.setVisibility(View.VISIBLE);
                break;
        }

    }

    public void setPullDownListener(XRecyclerView2.OnPullDownListener pullDownListener) {
        mOnPullDownListener = pullDownListener;
    }


    /** 隐藏底部 禁用上拉更多 **/
    @Override
    public void setHideFooter() {
        mFooterView.setVisibility(View.GONE);
        mFooterTextView.setVisibility(View.INVISIBLE);
        mFooterLoadingView.setVisibility(View.GONE);
        enableAutoFetchMore(false, 1);
        mFooterView.setPadding(0, -10000, 0, 0);
    }

    public void setNewsFeedHideFooter() {
        mIsFetchMoreing = false;
        mFooterView.setVisibility(View.GONE);
        mFooterTextView.setVisibility(View.INVISIBLE);
        mFooterLoadingView.setVisibility(View.GONE);
        mFooterView.setPadding(0, -10000, 0, 0);
    }

    /** 显示底部 使用上拉更多 **/
    @Override
    public void setShowFooter() {
        mFooterView.setVisibility(View.VISIBLE);
        mFooterView.findViewById(R.id.pulldown_footer_layout).setVisibility(View.VISIBLE);
        mFooterTextView.setVisibility(View.VISIBLE);
        mFooterTextView.setText(mContext.getResources().getString(
                R.string.load_more_item_layout_1));
        Methods.log("set has more content L1324");
        mFooterLoadingView.setVisibility(View.GONE);
        enableAutoFetchMore(true, 1);
        mFooterView.setPadding(mFooterPadding, mFooterPadding, mFooterPadding,
                mFooterPadding);
        mIsNoMore = false;
        mFooterView.invalidate();
    }

    /** 显示底部 已无更多按钮 **/
    public void setShowFooterNoMoreComments() {
        mFooterView.setVisibility(View.VISIBLE);
        mFooterView.findViewById(R.id.pulldown_footer_layout).setVisibility(View.VISIBLE);
        mFooterTextView.setText(mContext.getResources().
                getString(R.string.load_no_more_comments_item_layout_1));
        Methods.log( "set no more content L1339");
        mFooterTextView.setVisibility(View.VISIBLE);
        mFooterLoadingView.setVisibility(View.GONE);
        enableAutoFetchMore(false, 1);
        mFooterView.setPadding(mFooterPadding, mFooterPadding, mFooterPadding,
                mFooterPadding);
        Log.d("Scrollwht", "no --- width: " + mFooterView.getWidth() + " height: " + mFooterView.getHeight() + " mFooterPadding: " + mFooterPadding);
        mIsNoMore = true;
    }


    @Override
    public void notifyLoadMoreComplete() {
        post(new Runnable() {
            @Override
            public void run() {
                mIsFetchMoreing = false;
            }
        });
    }

    /**
     * This Method is useless!!!!--------> 是否开启自动获取更多
     * 自动获取更多，将会隐藏footer，并在到达底部的时候自动刷新
     *
     * @param index
     *            倒数第几个触发
     */
    public void enableAutoFetchMore(boolean enable, int index) {
        if (enable) {
        } else {
            if(mIsNoMore){
                mFooterTextView.setText(mContext.getResources().getString(
                        R.string.load_no_more_comments_item_layout_1));
            }else{
                mFooterTextView.setText(mContext.getResources().getString(
                        R.string.load_more_item_layout_1));
            }
            mFooterLoadingView.setVisibility(View.GONE);
        }
    }
}
