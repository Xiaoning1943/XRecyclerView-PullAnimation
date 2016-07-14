package scroll.xrecyclerviewdemo;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Created by xiaoning.wang on 16/7/12.
 */
public class Methods {


    private static final String TAG = "estate";

    /**
     * 输出info日志
     *
     * @param tag 调用者
     * @param msg 信息
     */

    public static void logInfo(Object tag, String msg) {


        if (tag != null) {
            if (tag instanceof String) {
                Log.i(TAG, tag + ":" + msg);
            } else {
                Log.i(TAG, tag.getClass().getSimpleName() + ":" + msg);
            }
        } else {
            Log.i(TAG, msg);
        }
        /* logOnFile(msg); */

    }

    /**
     * 调用系统的Log.d, tag为调用该方法的类名。同时会在msg前添加方法以及行号信息
     *
     * @param msg Log.d(tag, msg)中的msg
     */
    public static void log(String msg) {

        try {
            StackTraceElement caller = new Throwable().fillInStackTrace()
                    .getStackTrace()[1];
            if (caller == null)
                return;

            String className = new StringBuilder()
                    .append(caller.getClassName()).toString();
            className = className.substring(className.lastIndexOf(".") + 1);
            String methodName = new StringBuilder().append(
                    caller.getMethodName() + "->" + caller.getLineNumber()
                            + ": ").toString();
            Log.d(className, methodName + msg);
            // logOnFile(className + "->" + methodName + msg);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
