package it.polito.elite.teaching.cv;

import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

//converter class for OpenCV native MinMaxLocResult class
public class MaxLocScale {
	// manual port
//	public double minVal;
    public double maxVal;
//    public Point minLoc;
    public Point maxLoc;
    public float scale;


    public MaxLocScale() {
//        minVal=0; 
        maxVal=0;
//        minLoc=new Point();
        maxLoc=new Point();
        scale=1.0f;
    }
    
    public MaxLocScale(Core.MinMaxLocResult mm, float scale, int match_method) {
//        minVal=mm.minVal; 
    	if(match_method  == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
    		maxVal=mm.minVal;
    		maxLoc=mm.minLoc;
    		
    	}
		else {
			maxVal=mm.maxVal;
			maxLoc=mm.maxLoc;
		}
    	
//        maxVal=mm.maxVal;
//        minLoc=mm.minLoc;
//        maxLoc=mm.maxLoc;
        this.scale=scale;
    }

}
