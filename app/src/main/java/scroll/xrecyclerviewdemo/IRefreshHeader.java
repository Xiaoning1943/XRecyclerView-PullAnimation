package scroll.xrecyclerviewdemo;


/**
 * Created by xiaoning.wang on 16/7/6.
 */
interface IRefreshHeader {
    //public void onMove(float delta) ;
    void onRefreshComplete();

    void onRefreshError();
//    public final static int STATE_NORMAL = 0;
//    public final static int STATE_RELEASE_TO_REFRESH = 1;
//    public final static int STATE_REFRESHING = 2;
//    public final static int STATE_DONE = 3;


    /** 下拉更新 **/
    int PULL_To_REFRESH = 0;
    /** 松开更新 **/
    int RELEASE_To_REFRESH = 1;

    /** 自动弹出或下拉松开后自动滑动到刷新位置 */
    int AUTO_FLING = 2;
    /** 更新中 **/
    int REFRESHING = 3;
    /** 无 **/
    int DONE = 4;
    /** 加载中 **/
    int LOADING = 5;
    /** 更新失败 */
    int ERROR = 6;


    void setPullDownListener(XRecyclerView2.OnPullDownListener pullDownListener);
}
