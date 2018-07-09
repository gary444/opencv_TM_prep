package it.polito.elite.teaching.cv;

import java.util.ArrayList;
import java.util.Collections;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;


public class TemplateMatcher implements SignFinder {
	
	private Mat templateImg = new Mat();
	private Mat warpedTemplate = new Mat();
	private Mat grey_testImg = new Mat();
	private Mat scaledImg = new Mat();
	private Mat result = new Mat();
	private Mat dummyMat = new Mat(1,1,CvType.CV_8UC1);
	
	private Mat templates[] = {new Mat(), new Mat()};
	
	public TemplateMatcher(){
		loadTemplate();
		createWarpedTemplate();
	}
	
	public TemplateMatcher(int img_rows, int img_cols) {
		loadTemplate();
		createWarpedTemplate();
		
		grey_testImg = new Mat(img_rows, img_cols,CvType.CV_8UC1);
		result = new Mat(img_rows, img_cols,CvType.CV_32FC1);
		
	}
	
	public Mat loadTemplate() {
		Mat templLoad = new Mat();
		templLoad = Imgcodecs.imread("resources/temp3s.jpg");
		Imgproc.cvtColor(templLoad, this.templateImg, Imgproc.COLOR_BGR2GRAY);
		if (this.templateImg == null) {
			System.out.println("could not load template");
		}
		else {
			System.out.println("Template loaded succesfully");
			//update array
			this.templateImg.copyTo(templates[0]);
		}
		return this.templateImg;
	}
	
	private Mat createWarpedTemplate() {
		
		//4 start points...
		ArrayList<Point> srcPoints = new ArrayList<Point>();
		srcPoints.add(new Point(0,0));
		srcPoints.add(new Point(this.templateImg.width()-1,0));
		srcPoints.add(new Point(this.templateImg.width()-1,this.templateImg.height()-1));
		srcPoints.add(new Point(0,this.templateImg.height()-1));
		
		//4 end points to map the start points to
		double warpWidth = (this.templateImg.width()-1) * 0.8;
		double warpHeight = (this.templateImg.height()-1) * 1.0;
		ArrayList<Point> dstPoints = new ArrayList<Point>();
		dstPoints.add(new Point(0,0));
		dstPoints.add(new Point(warpWidth,0));
		dstPoints.add(new Point(warpWidth,warpHeight));
		dstPoints.add(new Point(0,warpHeight));
		
		//convert to matrix
		Mat srcMat = Converters.vector_Point2f_to_Mat(srcPoints);
        Mat dstMat = Converters.vector_Point2f_to_Mat(dstPoints);
        
        //get transform matrix
        Mat perspectiveTransformMat = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        
        //transform
        this.warpedTemplate = new Mat((int)warpHeight, (int)warpWidth, CvType.CV_8UC1);
        Imgproc.warpPerspective(this.templateImg, warpedTemplate, perspectiveTransformMat, new Size(warpWidth, warpHeight));
        
		//update array
		warpedTemplate.copyTo(templates[1]);

		return this.warpedTemplate;
	}
	

	public Rect findSign(Mat inputImage) {
		return findSign(inputImage, dummyMat);
	}
	
	public Rect findSign(Mat inputImage, Mat returnResult) {
		
//		long startTime = System.nanoTime();
		int match_method = Imgproc.TM_CCOEFF;
	
		
		//create grey version of color input
		Imgproc.cvtColor(inputImage, this.grey_testImg , Imgproc.COLOR_BGR2GRAY);
		
		
		//scale down until good match found or too small
		//threshold...1300000?
		final double threshold = 3000000;
		boolean matchFound = false;
		ArrayList<MaxLocScale> matches = new ArrayList<>();
		float scale = 1.f;
		float scaleStep = 0.1f;
		for (scale = 1.f; scale >= 0.5 && matchFound == false; scale -= scaleStep) {
			for(int t = 0; t < this.templates.length; t++) {
				
				//calc scaled size
				Size matSize = new Size(this.grey_testImg.width() * scale, this.grey_testImg.height() * scale);
				//resize
				Imgproc.resize(this.grey_testImg, scaledImg, matSize);
				//create result matrix
				result.create(scaledImg.rows() - this.templates[t].rows() + 1, scaledImg.cols() -  this.templates[t].cols() + 1, CvType.CV_8U);
				
				//template matching
				Imgproc.matchTemplate(scaledImg,  this.templates[t], result, match_method);
				MaxLocScale minMaxLocResult = new MaxLocScale (Core.minMaxLoc(result, new Mat()), scale, match_method);
				matches.add(minMaxLocResult);
				
				//compare max to threshold
				if (minMaxLocResult.maxVal > threshold) {
					matchFound = true;//will break out of for loop
				}
				
				//output response for max scale - to visualize only
				if (scale == 1.0) {
					//check if returnResult matrix is a dummy - dont return if so
					if(returnResult.rows() == inputImage.rows() && returnResult.cols() == inputImage.cols()) {
						Core.normalize(result, result, 0, 1, Core.NORM_MINMAX);
						//multiply result for viewing
						Core.multiply(result, new Scalar(255), returnResult);
					}
				}
				
			}
		}
		
		//if a match is found, then match is last element in array
		MaxLocScale bestMatch;
		if(matchFound) {
			bestMatch = matches.get(matches.size()-1);
		}
		else {
			//sort array
			Collections.sort(matches, (a, b) -> a.maxVal < b.maxVal ? 1 : a.maxVal == b.maxVal ? 0 : -1); 
			//best match is first element in array
			bestMatch = matches.get(0);
		}
		
		//convert match location to full size coordinates
		Point match = scalePoint(bestMatch.maxLoc,1/bestMatch.scale);
		
		//update text fields
//		match_output.setText(String.format("Match Rating = %13.3f%s     Time per frame: %d ", bestMatch.maxVal/1000000, matchFound ? "*" : " ", (System.nanoTime() - startTime)/1000000));
// 		scale_output.setText(String.format("Scale = %5.2f", bestMatch.scale));
		
		
		return new Rect(match, new Point(match.x + templateImg.cols()/bestMatch.scale, match.y + templateImg.rows()/bestMatch.scale));
	}

	
	private Point scalePoint(Point p, double mult) {
		return new Point(p.x * mult, p.y * mult);
	}

}
