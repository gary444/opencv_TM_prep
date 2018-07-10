package it.polito.elite.teaching.cv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class Playground {
	
	private Mat testMat;
	private Mat coords = new Mat();
	
	private Mat convMat;
	
	public Playground(){
		
		//create float matrix
		int cols = 10;
		int rows = 10;
		testMat = new Mat (rows, cols, CvType.CV_32FC1);
		//fill mat
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				testMat.put(y,x,(float)(y+x)*100.f);
			}
		}
		System.out.println("original matrix");
		System.out.println(testMat.dump());
		System.out.println(testMat.toString());
		
		
		testThresholder();
		
		
		
		
		
//		coords = new Mat(cols*rows, 1, CvType.CV_32SC2);
//		System.out.println(coords.dump());
		
		
//		testThresholder();
	}
	private void testThresholder() {
		
		//threshold float matrix
		Mat thresMat = new Mat();
		Imgproc.threshold(testMat, thresMat, 1000, 1, Imgproc.THRESH_BINARY);
		
		System.out.println("thresholded matrix:");
		System.out.println(thresMat.dump());
		System.out.println(thresMat.toString());
		
		//convert matrix
		convMat = new Mat (testMat.rows(), testMat.cols(), CvType.CV_8UC1);
		thresMat.convertTo(convMat, 0);
		
		System.out.println("converted matrix");
		System.out.println(convMat.dump());
		System.out.println(convMat.toString());
		
		
		
//		Core.findNonZero(thresMat, coords);
		

		

//		System.out.println("non zero matrix:");
//		System.out.println(coords.dump());

		
	}

	
}
