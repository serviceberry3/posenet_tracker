package weiner.noah.noshake.posenet.test.ctojavaconnector;

public class CircBuffer {
    //JAVA C++ INTERFACE FUNCTION PROTOTYPES
    public static native void circular_buffer(long sz, int axis);

    public static native void circular_buffer_destroy(int axis);

    //reset the circular buffer to empty, head == tail
    public static native void circular_buf_reset(int axis);

    public static native void retreat_pointer(int axis);

    public static native void advance_pointer(int axis);

    //add data to the queue; old data is overwritten if buffer is full
    public static native int circular_buf_put(float data, int axis);

    //retrieve a value from the buffer
    //returns 0 on success, -1 if the buffer is empty
    public static native float circular_buf_get(int axis);

    //returns true if the buffer is empty
    public static native boolean circular_buf_empty(int axis);

    //returns true if the buffer is full
    public static native boolean circular_buf_full(int axis);

    //returns the maximum capacity of the buffer
    public static native int circular_buf_capacity(int axis);

    //returns the current number of elements in the buffer
    public static native int circular_buf_size(int axis);

    public static native float aggregate_last_n_entries(int n, int axis);

    public static native int circular_buf_get_head(int axis);

    public static native long circular_buf_address(int axis);
}
