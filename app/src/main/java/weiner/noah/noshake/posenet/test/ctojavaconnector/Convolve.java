package weiner.noah.noshake.posenet.test.ctojavaconnector;

public class Convolve {
    public static native void convolver(long bufferAddy, int axis);

    public static native void convolver_destroy(int axis);

    public static native float convolve(int axis, int current_head);

    public static native float getYMember(int index, int axis);

    public static native float getHMember(int index, int axis);

    public static native float getXMember(int index, int axis);

    public static native float getTempXMember(int index, int axis);

    public static native long getYSize(int axis);
}
