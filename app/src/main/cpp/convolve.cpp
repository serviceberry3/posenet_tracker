#include "convolve.hh"
#include "circ_buffer.hh"
#include "impulse_response.hh"

static convolver* ySignalConvolverX = NULL;
static convolver* ySignalConvolverY = NULL;

convolver::convolver(long bufferAddy, int axis) {
    xArray = (float*) bufferAddy;
    //xLength = buff->circular_buf_size();
    xLength = 211;

    hArray = impulseResponses->responseArray;
    hLength = impulseResponses->size;

    yLength = xLength + hLength - 1;

    //allocated appropriately sized float array for the output signal (m+n-1), set entire array to 0 to begin accumulation
    yArray = (float*) calloc(sizeof(float), yLength);
}

convolver::~convolver() {
    //free(hArray);
    free(xArray);
    free(tempXArray);
    free(yArray);
}

float convolver::convolve(int current_head) {
    /*
    float* tempHArray = (float*) calloc(yLength, sizeof(float));
    memcpy(tempHArray, hArray, hLength);
*/
    tempXArray = (float*) malloc(sizeof(float) * xLength);

    int currHead = current_head;

    //we want to order the data from the circular buffer from oldest to newest, using the head as the break point
    memcpy(tempXArray, xArray + currHead, sizeof(float) * (xLength-currHead));
    memcpy(tempXArray + (xLength-currHead), xArray, sizeof(float) * currHead);

    // convolution operation
    for (int i = 0; i < xLength; i++)
    {
        for (int j = 0; j < hLength; j++)
        {
            yArray[i+j] += tempXArray[i] * hArray[j];
        }
    }

    return tempXArray[84];
}

float convolver::getTempXMember(int index) {
    return tempXArray[index];
}

float convolver::getYMember(int index) {
    return yArray[index];
}

float convolver::getHMember(int index) {
    return hArray[index];
}

float convolver::getXMember(int index) {
    return xArray[index];
}

size_t convolver::getYSize() {
    return yLength;
}

extern "C" {
    JNIEXPORT void Java_weiner_noah_noshake_posenet_test_ctojavaconnector_Convolve_convolver(JNIEnv *javaEnvironment, jclass __unused obj, jlong bufferAddy, jint axis) {
        (axis==0 ? ySignalConvolverX : ySignalConvolverY) = new convolver(bufferAddy, axis);
    }

    JNIEXPORT void Java_weiner_noah_noshake_posenet_test_ctojavaconnector_Convolve_convolver_1destroy(JNIEnv *javaEnvironment, jclass __unused obj, jint axis) {
        (axis==0 ? delete ySignalConvolverX : delete ySignalConvolverY);
    }

    JNIEXPORT jfloat Java_weiner_noah_noshake_posenet_test_ctojavaconnector_Convolve_convolve(JNIEnv *javaEnvironment, jclass __unused obj, jint axis, jint current_head) {
        return (axis==0 ? ySignalConvolverX : ySignalConvolverY)->convolve(current_head);
    }

    JNIEXPORT jfloat Java_weiner_noah_noshake_posenet_test_ctojavaconnector_Convolve_getYMember(JNIEnv *javaEnvironment, jclass __unused obj, jint index, jint axis) {
        return (axis==0 ? ySignalConvolverX : ySignalConvolverY)->getYMember(index);
    }

    JNIEXPORT jfloat Java_weiner_noah_noshake_posenet_test_ctojavaconnector_Convolve_getHMember(JNIEnv *javaEnvironment, jclass __unused obj, jint index, jint axis) {
        return (axis==0 ? ySignalConvolverX : ySignalConvolverY)->getHMember(index);
    }

    JNIEXPORT jfloat Java_weiner_noah_noshake_posenet_test_ctojavaconnector_Convolve_getXMember(JNIEnv *javaEnvironment, jclass __unused obj, jint index, jint axis) {
        return (axis==0 ? ySignalConvolverX : ySignalConvolverY)->getXMember(index);
    }

    JNIEXPORT jlong Java_weiner_noah_noshake_posenet_test_ctojavaconnector_Convolve_getYSize(JNIEnv *javaEnvironment, jclass __unused obj, jint axis) {
        return (axis==0 ? ySignalConvolverX : ySignalConvolverY)->getYSize();
    }

    JNIEXPORT jfloat Java_weiner_noah_noshake_posenet_test_ctojavaconnector_Convolve_getTempXMember(JNIEnv *javaEnvironment, jclass __unused obj, jint index, jint axis) {
        return (axis==0 ? ySignalConvolverX : ySignalConvolverY)->getTempXMember(index);
    }
}