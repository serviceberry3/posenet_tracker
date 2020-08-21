#include "impulse_response.hh"

#define HZ 211
#define TAU 0

//redeclaration
float impulse_resp_arr::kValue = 0;
float impulse_resp_arr::eValue = 0;
size_t impulse_resp_arr::size = 0;
float* impulse_resp_arr::responseArray = NULL;

impulse_resp_arr::impulse_resp_arr(size_t sz, float e, float k) {
    eValue = e;
    kValue = k;
    size = sz;
    responseArray = (float*) memalign(16, sizeof(float)*sz);
}

impulse_resp_arr::~impulse_resp_arr() {
    free(responseArray);
}

//fill the array with the appropriate H(t) result based on spring constant k selected
void impulse_resp_arr::impulse_response_arr_populate() {

    //divide 4.0 seconds by the size of the array
    float sqrtK = sqrt(kValue);
    /*
    float timeIncrement = 4.0f/size;


    for (int i=0; i<size; i++) {
        float currTime = timeIncrement * i;
        //fill in this spot in the array with the appropriate H(t) value
        responseArray[i] = ((float)(currTime) * pow(eValue, -currTime * sqrtK));
    }
     */

    for (int t=0; t<size; t++) {
        responseArray[size-1-t] = (float)(t+TAU)/HZ * pow(eValue, -(float)(t+TAU)/HZ * sqrtK);
    }
}

float impulse_resp_arr::impulse_response_arr_get_value(int index) {
    return responseArray[index];
}

float impulse_resp_arr::impulse_response_arr_get_sum() {
    assert(responseArray!=NULL);
    assert(size>0);

    float sum = 0;

    for (int i = 0; i<size; i++) {
        sum+=responseArray[i];
    }
    return sum;
}

//Java interface functions
extern "C" {
    JNIEXPORT void Java_weiner_noah_noshake_posenet_test_ctojavaconnector_ImpulseResponse_impulse_1resp_1arr(JNIEnv *javaEnvironment, jclass __unused obj, jlong sz, jfloat e, jfloat k) {
        impulseResponses = new impulse_resp_arr(sz, e, k);
    }

    JNIEXPORT void Java_weiner_noah_noshake_posenet_test_ctojavaconnector_ImpulseResponse_impulse_1response_1arr_1populate(JNIEnv *javaEnvironment, jclass __unused obj) {
        impulseResponses->impulse_response_arr_populate();
    }

    JNIEXPORT jfloat Java_weiner_noah_noshake_posenet_test_ctojavaconnector_ImpulseResponse_impulse_1response_1arr_1get_1sum(JNIEnv *javaEnvironment, jclass __unused obj) {
        return impulseResponses->impulse_response_arr_get_sum();
    }

    JNIEXPORT jfloat Java_weiner_noah_noshake_posenet_test_ctojavaconnector_ImpulseResponse_impulse_1response_1arr_1get_1value(JNIEnv *javaEnvironment, jclass __unused obj, jint index) {
        return impulseResponses->impulse_response_arr_get_value(index);
    }
}

