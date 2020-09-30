Learning how to use PoseNet so that I can try to have an Android device locate it's position and velocity relative to a person's shoulders.

Currently calculates a rough instantaneous velocity estimate based on the average human's pupillary distance and the pixel displacement of the human's nose.

UPDATE: I'm working on using OpenCV to estimate the actual pose (position and orientation in 3D space) of the person based on the relative locations of their eyes, nose, and shoulders. Doing this by using OpenCV's solvePnP fxn to solve for the rotation and translation matrices when projecting a point in the world coordinate system onto the camera coordinate system. Application: be able to extract rough model of a human in 3D space, allowing an onlooking drone to swing around when a human turns their body.
