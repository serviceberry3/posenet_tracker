# An Enhanced Posenet Sandbox #

Implementing Tensorflow Posenet such that an Android device can use a single camera to calculate the angle of a person's torso (which direction they are facing) and then locate its own position and velocity relative to their shoulders.

UPDATE(April 2021): for some more features, look at the ```control/PosenetStats.java``` file in my ![crazyflie_receiver](https://github.com/serviceberry3/crazyflie_receiver/) repo:  
* Trig-based estimation of torso angle (models head and shoulders and two intersecting ellipses based on real measurements taken)
* Estimation of human's angular and x, y, z velocities
* Calculation of offset between center of frame and human's torso center
* Calculation of psi, the yaw angle from the camera to the human's torso center, can be found in ```control/HumanFollower.java``` (based on center offset and distance calculations)

UPDATE: The app implements Posenet and then draws x, y, and z axes through the person's chest area, indicating the angle of their torso. I'm working on figuring out why I'm getting bogus data from solvePnP() sometimes (I blocked the incorrect data, which is responsible for the flickering of the axes).

Here are some previews (newest version first):  
![Posenet w/dist and angle preview](https://github.com/serviceberry3/posenet_tracker/blob/master/img/humdisttest.gif?raw=true)  

![Posenet PnP preview](https://github.com/serviceberry3/posenet_tracker/blob/master/img/pose_est.gif?raw=true)  


UPDATE: I'm working on using OpenCV to estimate the actual pose (position and orientation in 3D space) of the person based on the relative locations of their eyes, nose, and shoulders. Doing this by using OpenCV's solvePnP fxn to solve for the rotation and translation matrices when projecting a point in the world coordinate system onto the camera coordinate system. Application: be able to extract rough model of a human in 3D space, allowing an onlooking drone to swing around when a human turns their body.
