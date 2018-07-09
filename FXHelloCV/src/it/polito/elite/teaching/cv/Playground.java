package it.polito.elite.teaching.cv;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class Playground {
	public Playground(){
		
		testIntegralImage();
	}
	
	private void testIntegralImage(){
		
		
		
		int cols = 10;
		int rows = 10;
		
		Mat testMat = new Mat (rows, cols, CvType.CV_8UC1);
		//fill mat
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				testMat.put(y,x,y+x);
			}
		}
		
		System.out.println("original matrix");
		System.out.println(testMat.dump());
		
		Mat integral_img = new Mat(rows+1,cols+1,CvType.CV_8UC1);
		Imgproc.integral(testMat, integral_img);
		
		
		System.out.println(sumInRect(new Rect(2,2,2,2), integral_img));
		System.out.println(sumInRect(new Rect(0,8,2,2), integral_img));
		System.out.println(sumInRect(new Rect(0,0,3,3), integral_img));
		
		
		
		System.out.println("integral image matrix");
		System.out.println(integral_img.dump());
		
		
		
		
	}
	
	private double sumInRect (Rect r, Mat integral_img){
		double a = integral_img.get(r.y, r.x)[0];
		double b = integral_img.get(r.y, r.x+r.width)[0];
		double c = integral_img.get(r.y + r.height, r.x)[0];
		double d = integral_img.get(r.y + r.height, r.x+r.width)[0];
		return a + d - b - c;
	}
	
	
}
