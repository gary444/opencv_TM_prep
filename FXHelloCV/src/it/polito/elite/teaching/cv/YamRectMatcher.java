package it.polito.elite.teaching.cv;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class YamRectMatcher implements SignFinder {
	

	private Mat dummyMat = new Mat(1,1,CvType.CV_8UC1);
	private Rect dummyRect = new Rect(new Point(0,0), new Point(5,5));
	
	private Rect[] template;
	private final int templateSize = 40;
	private Mat integ_img = new Mat();
	private Mat rectMatchResponse = new Mat();
	
	private Mat result;
	private Mat thresResult;
	private Mat thresResultInt;
	private Mat coords = new Mat();
	
	
	YamRectMatcher(){
		
	}
	YamRectMatcher(int img_rows, int img_cols){
		integ_img = new Mat(img_rows + 1, img_cols + 1, CvType.CV_32FC1);
		rectMatchResponse = new Mat(img_rows, img_cols, CvType.CV_32FC1);
		result =  new Mat(img_rows, img_cols, CvType.CV_32FC1);
		thresResult = new Mat(img_rows, img_cols, CvType.CV_32FC1);
		thresResultInt = new Mat(img_rows, img_cols, CvType.CV_8UC1);
		this.template = createTemplate(templateSize);
	}
	
	private Rect[] createTemplate(int templateSize){
		
		//define template as a set of rectangles described by floats
		//alternating black and white: B1, W1, B2, W2,...
		DoubleRect[] doubleTemplate = new DoubleRect[16];
		
		doubleTemplate[0] = new DoubleRect (0.25, 0.0, 0.25, 0.125);//B1
		doubleTemplate[1] = new DoubleRect (0.25, 0.125, 0.25, 0.125); //W1
		doubleTemplate[2] = new DoubleRect (0.5, 0.0, 0.25, 0.125);//B2
		doubleTemplate[3] = new DoubleRect (0.5, 0.125, 0.25, 0.125);//W2
		
		doubleTemplate[4] = new DoubleRect (0.875, 0.25, 0.125, 0.25);//B3
		doubleTemplate[5] = new DoubleRect (0.75, 0.25, 0.125, 0.25); //W3
		doubleTemplate[6] = new DoubleRect (0.875, 0.5, 0.125, 0.25);//B4
		doubleTemplate[7] = new DoubleRect (0.75, 0.5, 0.125, 0.25);//W4
		
		doubleTemplate[8] = new DoubleRect (0.5, 0.875, 0.25, 0.125);//B5
		doubleTemplate[9] = new DoubleRect (0.5, 0.75, 0.25, 0.125); //W5
		doubleTemplate[10] = new DoubleRect (0.25, 0.875, 0.25, 0.125);//B6
		doubleTemplate[11] = new DoubleRect (0.25, 0.75, 0.25, 0.125);//W6

		doubleTemplate[12] = new DoubleRect (0.0, 0.5, 0.125, 0.25);//B7
		doubleTemplate[13] = new DoubleRect (0.125, 0.5, 0.125, 0.25); //W7
		doubleTemplate[14] = new DoubleRect (0.0, 0.25, 0.125, 0.25);//B8
		doubleTemplate[15] = new DoubleRect (0.125, 0.25, 0.125, 0.25);//W8
		
		//translate to pixel values depending on size of template
		Rect[] template = new Rect[16];
		for (int i = 0; i < 16; i++) {
			template[i] = doubleTemplate[i].cvtToRectWithScale(templateSize);
		}
		
		return template;
	}

	@Override
	public Rect findSign(Mat inputImage) {
		Rect r = findSign(inputImage, dummyMat);
		return r;
	}

	@Override
	public Rect findSign(Mat inputImage, Mat returnResult) {
	
		try {
			//only for colour input - convert to gray
			if (inputImage.channels() == 3) {
				Imgproc.cvtColor(inputImage, inputImage, Imgproc.COLOR_BGR2GRAY);
			}
			
			Imgproc.integral(inputImage, this.integ_img);
		
//			System.out.println("RR: rows = " + returnResult.rows() + " cols = " + returnResult.cols() + " chans = " + returnResult.channels() + " type= " + returnResult.type());
//			System.out.println("II: rows = " + this.integ_img.rows() + " cols = " + this.integ_img.cols() + " chans = " + this.integ_img.channels() + " type= " + this.integ_img.type());
			
			rectanglePatternMatching(returnResult);
			
		} 
		catch (Exception e) {
			System.err.println("findSign: " + e.getMessage());
		}
		
		return dummyRect;
	}
	
	//returns a series of rectangles that describe possible rectangle locations
	public ArrayList<Rect> findSigns(Mat inputImage) {
		ArrayList<Rect> rects = new ArrayList<>();
		final float THRESHOLD = 550.f;
		
		try {
			//only for colour input - convert to gray
			if (inputImage.channels() == 3) {
				Imgproc.cvtColor(inputImage, inputImage, Imgproc.COLOR_BGR2GRAY);
			}

			Imgproc.integral(inputImage, this.integ_img);  //compute integral
			rectanglePatternMatching(result);  				//get response to rectangle pattern
			
			//find locations where response is bigger than threshold and create rectangles at these points
			Imgproc.threshold(result, thresResult, THRESHOLD, 1, Imgproc.THRESH_BINARY);
			thresResult.convertTo(thresResultInt, 0);
			Core.findNonZero(thresResultInt, coords);
			
			
			for (int i = 0; i < coords.rows(); i++) {
				rects.add(new Rect((int)(coords.get(i, 0)[0]), (int)(coords.get(i, 0)[1]), templateSize, templateSize));
			}
			
//			print tests
//			System.out.println("num peaks: " + coords.rows());
			
		} catch (Exception e) {
			System.err.println("findSigns: " + e.getMessage());
		}
		
		return rects;
	}


	//returns a series of rectangles that describe possible rectangle locations
	//sets the 'funcResponse' mat as the result of the rectangle detection function
	public ArrayList<Rect> findSigns(Mat inputImage, Mat funcResponse) {
		result = funcResponse;
		return findSigns(inputImage);
	}
	
	//fills the response matrix 
	private void rectanglePatternMatching(Mat returnResult) {
		
		try {
			//for each position in matrix calculate response
			//TODO for now only calc when template is in image range
			float [] response = new float[1];

			for (int x = 0; x < this.integ_img.cols() - templateSize; x++) {
				for (int y = 0; y < this.integ_img.rows() - templateSize; y++) {

//					response[0] = 0.5f;
					response[0] = getResponseAt(x,y);
//					returnResult.put(y, x, response);
				}
			}
		} catch (Exception e) {
			System.err.println("rectanglePatternMatching: " + e.getMessage());
		}
		
	}
	
	//calculates the total differences between pairs of regions
	//where top let of template is given by x,y
	private float getResponseAt(int x, int y) {

		float totalDiffs = 0.0f;
		
		try {
			
			//for each pair
			for (int i = 0; i < 16; i+=2) {
				//B 
				Rect r = this.template[i];
				float bSum = sumInRect(new Rect(r.x+x, r.y+y, r.width, r.height) ,this.integ_img) / (float)r.area();
				//W
				
//				float wSum = 0.4f;
				r = this.template[i+1];
				float wSum = sumInRect(new Rect(r.x+x, r.y+y, r.width, r.height) ,this.integ_img) / (float)r.area();
				
				
				
				totalDiffs += (wSum-bSum);
			}
			
			//TODO - edge handling
		} catch (Exception e) {
			System.err.println("getResponseAt: " + e.getMessage());
		}
		
		return totalDiffs;
	}
	
	//return sum of pixels within a given rectangle, for the given matrix
	private float sumInRect (Rect r, Mat input_img){
		double a = 0.0, b = 0.0, c = 0.0, d = 0.0;
//		double a = input_img.get(r.y, r.x)[0];
//		double b = input_img.get(r.y, r.x+r.width)[0];
//		double c = input_img.get(r.y + r.height, r.x)[0];
//		double d = input_img.get(r.y + r.height, r.x+r.width)[0];
		return (float)(a + d - b - c);
	}

}
