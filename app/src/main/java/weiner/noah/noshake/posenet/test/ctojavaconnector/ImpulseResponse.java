package weiner.noah.noshake.posenet.test.ctojavaconnector;

public class ImpulseResponse {
    public static native void impulse_resp_arr(long sz, float e, float k);

    public static native void impulse_response_arr_populate();

    public static native float impulse_response_arr_get_value(int index);

    public static native float impulse_response_arr_get_sum();
}
