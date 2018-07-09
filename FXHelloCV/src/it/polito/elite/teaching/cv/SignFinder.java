package it.polito.elite.teaching.cv;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface SignFinder {

	Rect findSign(Mat inputImage);
	
	Rect findSign(Mat inputImage, Mat returnResult);
}
