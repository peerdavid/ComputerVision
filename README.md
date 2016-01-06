# ComputerVision

This app contains some basic computer vision samples using open cv for android (http://opencv.org/platforms/android.html). 
There are 2 different sections available: Feature detection and motion. 

## Image formation
All kernels, feature detectors etc. can be convoluted with the image separately.
So you can see, how every filter, detector etc. changes the result.
The following filter, feature detectors etc. are available:

 - Gabor filter
 - Energy of gabor
 - Gaussian blur
 - Canny edge detector
 - Harries corner detector

## Feature matching (search image in other images)
 - ORB feature detector
 - ORB feature descriptor
 - Hamming distance to match features

## Motion
Contains motion analysis using x/t transformation and one of the following filter:
 - Energy of gabor (left, right, static or flicker)
 - 9-Tap filter  