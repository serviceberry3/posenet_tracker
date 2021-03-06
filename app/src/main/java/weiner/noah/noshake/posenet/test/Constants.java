/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package weiner.noah.noshake.posenet.test;

public class Constants {
    //Request camera and external storage permission.
    public static final int REQUEST_CAMERA_PERMISSION = 1;

    //From https://blog.paperspace.com/posenet-keypoint-detection-android-app/: "The posenet model accepts an image size of (257, 257).
    //If an image is to be passed to the posenet model, then its size must be (257, 257); otherwise, an exception will be thrown
    //For example, if an image read from a gallery is a different size, it must be resized before being passed to posenet

    //required input image width
    public static final int MODEL_WIDTH = 257;

    //required input image height
    public static final int MODEL_HEIGHT= 257;

    //average pupillary distance for adults? use this *for now* to estimate drone's distance from person
    public static final float PD = (float) 0.063; //63 mm

    //91 cm away -> ~90 pixel pupillary distance.
    //Thus experimental focal len of camera = (P x D) / W = (90px x 0.91m) / 0.063m = 1300
    public static final float focalLenExpAdjusted = 1300f;

    //Focal length based on the scaled-down 257x257 image
    public static final float focalLenExp = 219f;

    //Meanwhile, using opencv chessboard calibration, claimed focal length x is 524.1, y is 523.9


    //an array containing ratios between (dist from rt shoulder to rt eye) and (dist from left shoulder to left eye), for
    //each of 0-90 degrees
    public static final double[] angleRatios = {0.6712, };


    public static final int RUN_PNET_EVERY_N_FRAMES = 3;
}
