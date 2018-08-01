package it.polito.elite.teaching.cv;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface SignFinder {

	Rect findSign(Mat inputImage);
	
	Rect findSign(Mat inputImage, Mat returnResult);
	
	ArrayList<Rect> findSigns(Mat inputImage, Mat returnResult);
	
	ArrayList<Rect> findSigns(Mat inputImage);
}
