#ifndef NOSHAKE_CONVOLVE_HH
#define NOSHAKE_CONVOLVE_HH

#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include "impulse_response.hh"
#include "circ_buffer.hh"

class convolver {
public:
    convolver(long bufferAddy, int axis);

    ~convolver();

    float convolve(int current_head);

    float getYMember(int index);

    float getHMember(int index);

    float getXMember(int index);

    float getTempXMember(int index);

    size_t getYSize();

private:
    float* hArray;
    int hLength;
    float* xArray;
    int xLength;
    float* yArray;
    int yLength;

    float* tempXArray;
};

#endif //NOSHAKE_CONVOLVE_HH
