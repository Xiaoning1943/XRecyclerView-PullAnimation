package scroll.xrecyclerviewdemo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class RefreshHeader extends LinearLayout implements IRefreshHeader {
    private static final String TAG = "RefreshHeader";
    private LinearLayout mContainer;


    /** 刷新成功后平滑收起的动画时间 */
    private static final int UPDATE_SUCCESS_ANIMATION_DURATION = 500;
    /** 平滑收起的时间 */
    private static final int SMOOTH_SCROLL_DURATION = 300;
	private ImageView mProgressView;
    /** 状态 **/
    public volatile int mCurrentState;
    private Context mContext;

    private AnimationDrawable mPulldownDrawable;

    private AnimationDrawable mOnceAnimDrawable;

    private AnimationDrawable mRepeatAnimDrawable;

    private AnimationDrawable mFadeOutAnimDrawable;

    public int mAnimStartHeight;

    private float mAnimStepHeight;

    public int mPullDownFrameCount;

    private int mOnceAnimDuration;

    /** 刷新和更多的事件接口 **/
    XRecyclerView2.OnPullDownListener mOnPullDownListener = null;


    /** 头部高度 **/
    public int headContentHeight;

    public boolean mCanRefleash = false;


	public RefreshHeader(Context context) {
		super(context);
		initView(context);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public RefreshHeader(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	private void initView(Context context) {

        mContext = context;
        headContentHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.vc_0_0_1_newsfeed_refresh_head_height);// headView.getMeasuredHeight();

        mContainer = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.vc_0_0_1_newsfeed_pull_down_head, null);

        mProgressView = (ImageView) mContainer
                .findViewById(R.id.vc_0_0_1_newsfeed_refresh_progress);

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 0);
        this.setLayoutParams(lp);
        this.setPadding(0, 0, 0, 0);

        addView(mContainer, new LayoutParams(LayoutParams.MATCH_PARENT, 0));
        setGravity(Gravity.BOTTOM);

        initPullDownAnim();
        mCurrentState = DONE;

    }

    private void initPullDownAnim() {

        try {
            mPulldownDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.vc_0_0_1_newsfeed_loading_anim_pull) ;
            mOnceAnimDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.vc_0_0_1_newsfeed_loading_anim_once);
            mRepeatAnimDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.vc_0_0_1_newsfeed_loading_anim_repeat);
            mFadeOutAnimDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.vc_0_0_1_newsfeed_loading_anim_fade_out);

            mProgressView.setImageDrawable(mPulldownDrawable.getFrame(0));

            mPullDownFrameCount = mPulldownDrawable.getNumberOfFrames();

            mAnimStartHeight = headContentHeight;

            mAnimStepHeight = 1.0f * (headContentHeight * 0.4f)
                    / (mPullDownFrameCount - 1);

            int onceAnimFrameCount = mOnceAnimDrawable.getNumberOfFrames();
            mOnceAnimDuration = 0;
            for (int i = 0; i < onceAnimFrameCount; i++) {
                mOnceAnimDuration += mOnceAnimDrawable.getDuration(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public void changeHeaderViewByState(final int targetState) {

        switch (targetState) {

            case PULL_To_REFRESH:
                Log.d(TAG, "====PULL_To_REFRESH====");
                mProgressView.setVisibility(View.VISIBLE);
                mCurrentState = targetState;
                mProgressView.setImageDrawable(mPulldownDrawable
                        .getFrame(mPullDownFrameCount - 1));
                // 当前状态，下拉刷新
                break;

            case RELEASE_To_REFRESH:
                Log.d(TAG, "====RELEASE_To_REFRESH====");

                mProgressView.setVisibility(View.VISIBLE);
                mProgressView.setImageDrawable(mPulldownDrawable
                        .getFrame(mPullDownFrameCount - 1));
                mCurrentState = targetState;
                // 当前状态，松开刷新
                break;

            case AUTO_FLING:
                Log.d(TAG, "====AUTO_FLING====");
                mProgressView.setVisibility(View.VISIBLE);


                mCurrentState = targetState;
                smoothScrollTo(headContentHeight, SMOOTH_SCROLL_DURATION);
                post(new Runnable() {
                    @Override
                    public void run() {
                        changeHeaderViewByState(REFRESHING);
                    }
                });
                break;
            case REFRESHING:
                Log.d(TAG, "====REFRESHING====");
                mProgressView.setVisibility(View.VISIBLE);
                mProgressView.setImageDrawable(mOnceAnimDrawable);
                mOnceAnimDrawable.stop();
                mOnceAnimDrawable.start();
                mProgressView.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        startRepeatAnim(targetState);
                    }
                }, mOnceAnimDuration);

                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentState = targetState;
                        if (mCanRefleash) {
                            mCanRefleash = false;
                            if (mOnPullDownListener != null) {
                                //ImageLoader.mIsDownLoad = true;
                                mOnPullDownListener.onRefresh();
                            }
                        }
                    }
                }, 0);

                // 当前状态,正在刷新...
                break;
            case DONE:
                Log.d(TAG, "====DONE====");
                mProgressView.setVisibility(View.VISIBLE);

                smoothScrollTo(0, UPDATE_SUCCESS_ANIMATION_DURATION);
                mProgressView.setImageDrawable(mFadeOutAnimDrawable);
                mFadeOutAnimDrawable.start();

                post(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentState = targetState;
                    }
                });

                // 当前状态，done
                break;
            case ERROR:
                Log.d(TAG, "====ERROR====");
                mProgressView.setVisibility(View.GONE);
                mRepeatAnimDrawable.stop();
                mProgressView.setImageDrawable(null);
                reset();

                mCurrentState = targetState;
                break;

        }
    }



    public int getState() {
        return mCurrentState;
    }

    @Override
    public void onRefreshComplete() {
        Methods.log("onRefreshComplete");
        changeHeaderViewByState(DONE);
    }

    @Override
    public void onRefreshError() {
        changeHeaderViewByState(ERROR);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                changeHeaderViewByState(DONE);
            }
        }, 2000);
    }

    @Override
    public void setPullDownListener(XRecyclerView2.OnPullDownListener pullDownListener) {
        mOnPullDownListener = pullDownListener;
    }

    public void setVisiableHeight(int height) {
		if (height < 0)
			height = 0;
		LayoutParams lp = (LayoutParams) mContainer
				.getLayoutParams();
		lp.height = height;
		mContainer.setLayoutParams(lp);
	}

	public int getVisiableHeight() {
        int height = 0;
        LayoutParams lp = (LayoutParams) mContainer
                .getLayoutParams();
        height = lp.height;
		return height;
	}

    public void reset() {
        smoothScrollTo(0, SMOOTH_SCROLL_DURATION);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                changeHeaderViewByState(DONE);
            }
        }, 500);
    }

    private void smoothScrollTo(int destHeight, int during) {
        ValueAnimator animator = ValueAnimator.ofInt(getVisiableHeight(), destHeight);
        animator.setDuration(during).start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                setVisiableHeight((int) animation.getAnimatedValue());
            }
        });
        animator.start();
    }

    private void startRepeatAnim(int state) {
        if (state == REFRESHING) {
                mProgressView.setImageDrawable(mRepeatAnimDrawable);
                mRepeatAnimDrawable.stop();
                mRepeatAnimDrawable.start();
        }
    }

    public void refreshComplete() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mCurrentState != ERROR) {
                    onRefreshComplete();
                }
            }
        }, 500);
    }

    public void refreshError() {
        post(new Runnable() {
            @Override
            public void run() {
                onRefreshError();
            }
        });
    }

    public void setPullingImage(int height) {
        int frameIndex = (int) ((height - mAnimStartHeight)
                / mAnimStepHeight) + 1;
        if (frameIndex >= mPullDownFrameCount)
            frameIndex = mPullDownFrameCount - 1;
        mProgressView.setImageDrawable(mPulldownDrawable
                .getFrame(frameIndex));
        if (frameIndex == mPullDownFrameCount - 1) {
            changeHeaderViewByState(RELEASE_To_REFRESH);

        }
    }
    public void onMove(float delta) {
        if(getVisiableHeight() > 0 || delta > 0) {
            setVisiableHeight((int) delta + getVisiableHeight());
            if ( mCurrentState == PULL_To_REFRESH) { // 未处于刷新状态，更新箭头
                if (getVisiableHeight() > headContentHeight) {
                    changeHeaderViewByState(RELEASE_To_REFRESH);
                }
            }
        }
    }

}
