package it.polito.elite.teaching.cv;

import org.opencv.core.Rect;

public class DoubleRect {
	public double x;
	public double y;
	public double width;
	public double height;
	
	DoubleRect(double x, double y, double width, double height){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	//returns an open CV rectangle with integer values
	Rect cvtToRectWithScale(int scale) {
		return new Rect((int)(this.x * scale), (int)(this.y * scale), (int)(this.width * scale), (int)(this.height * scale));
	}
}
