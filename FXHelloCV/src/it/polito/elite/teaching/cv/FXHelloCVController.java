package it.polito.elite.teaching.cv;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import it.polito.elite.teaching.cv.utils.Utils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a> (minor fixes)
 * @version 2.0 (2016-09-17)
 * @since 1.0 (2013-10-20)
 *
 */
public class FXHelloCVController
{
	// the FXML image view
	@FXML
	private ImageView camPanel;
	@FXML
	private ImageView inputImgPanel;
	@FXML
	private ImageView outputImgPanel;
	@FXML
	private ImageView templatePanel;
	@FXML
	private ImageView warpedTemplatePanel;
	@FXML
	private TextArea match_output;
	@FXML
	private TextArea scale_output;
	
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// the id of the camera to be used
	private static int cameraId = 0;
	
	private Mat testImg = new Mat();
	private Mat templateImg = new Mat();
	private Mat warpedTemplate = new Mat();
	private Mat grey_testImg = new Mat();
	private Mat scaledImg = new Mat();
	private Mat result = new Mat();
	
	
	public void loadAndShowImage() {
		this.testImg = Imgcodecs.imread("resources/scene4.jpg");
		if (this.testImg == null) {
			System.out.println("could not load image");
		}
		else {
			System.out.println("Image loaded succesfully");
			processImage(this.testImg);
			// convert and show the frame
			Image imageToShow = Utils.mat2Image(this.testImg);
			updateImageView(inputImgPanel, imageToShow);
		}
		
	}
	
	public void loadAndShowTemplate() {
		Mat templLoad = new Mat();
		templLoad = Imgcodecs.imread("resources/temp1s.jpg");
		Imgproc.cvtColor(templLoad, this.templateImg, Imgproc.COLOR_BGR2GRAY);
		if (this.templateImg == null) {
			System.out.println("could not load template");
		}
		else {
			System.out.println("Template loaded succesfully");
			Image imageToShow = Utils.mat2Image(this.templateImg);
			updateImageView(templatePanel, imageToShow);
			
			createWarpedTemplate();
		}
	}
	
	private void createWarpedTemplate() {
		
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
        
        Mat perspectiveTransformMat = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        
        warpedTemplate = new Mat((int)warpHeight, (int)warpWidth, CvType.CV_8UC1);
        
        Imgproc.warpPerspective(this.templateImg, warpedTemplate, perspectiveTransformMat, new Size(warpWidth, warpHeight));
        
		
		

		Image imageToShow = Utils.mat2Image(warpedTemplate);
		updateImageView(warpedTemplatePanel, imageToShow);
	}
	
	private Rect findTemplate(Mat inputImage, Mat template, Mat returnResult) {
		
		long startTime = System.nanoTime();
		int match_method = Imgproc.TM_CCOEFF;
		
		//create grey version of color input
		Imgproc.cvtColor(inputImage, this.grey_testImg , Imgproc.COLOR_BGR2GRAY);
		
		//scale down until good match found or too small
		//threshold...1300000?
		final double threshold = 3000000;
		double peak;
		boolean matchFound = false;
		ArrayList<MaxLocScale> matches = new ArrayList<>();
		float scale = 1.f;
		float scaleStep = 0.1f;
		for (scale = 1.f; scale >= 0.5 && matchFound == false; scale -= scaleStep) {
			
			//calc scaled size
			Size matSize = new Size(this.grey_testImg.width() * scale, this.grey_testImg.height() * scale);
			//resize
			Imgproc.resize(this.grey_testImg, scaledImg, matSize);
			//create result matrix
			result.create(scaledImg.rows() - template.rows() + 1, scaledImg.cols() - template.cols() + 1, CvType.CV_8U);
			
			//template matching
			Imgproc.matchTemplate(scaledImg, template, result, match_method);
			MaxLocScale minMaxLocResult = new MaxLocScale (Core.minMaxLoc(result, new Mat()), scale, match_method);
			matches.add(minMaxLocResult);
			
			//compare max to threshold
			if (minMaxLocResult.maxVal > threshold) {
				matchFound = true;//will break out of for loop
			}
			
			//output response for max scale - to visualize only
			if (scale == 1.0) {
				Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
				//multiply result for viewing
				Core.multiply(result, new Scalar(255), returnResult);
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
		match_output.setText(String.format("Match Rating = %13.3f%s     Time per frame: %d ", bestMatch.maxVal/1000000, matchFound ? "*" : " ", (System.nanoTime() - startTime)/1000000));
 		scale_output.setText(String.format("Scale = %5.2f", bestMatch.scale));
		
		return new Rect(match, new Point(match.x + templateImg.cols()/bestMatch.scale, match.y + templateImg.rows()/bestMatch.scale));
	}
	
	private Point scalePoint(Point p, double mult) {
		return new Point(p.x * mult, p.y * mult);
	}
	
	private void processImage(Mat img) {
		long startTime = System.nanoTime();
		
		Mat response = new Mat();
		Rect matchRect = findTemplate(this.testImg, this.templateImg, response);

		//draw max rectangle
		Imgproc.rectangle(this.testImg, matchRect.tl(), matchRect.br(), new Scalar(0,0,255));

		//convert response to image and show
		MatOfByte byteMat = new MatOfByte();
		Imgcodecs.imencode(".bmp", response, byteMat);
		Image imageToShow = new Image(new ByteArrayInputStream(byteMat.toArray()));
		updateImageView(outputImgPanel, imageToShow);
		
		System.out.println("Execution of image processing: " + (System.nanoTime() - startTime)/1000000 + " ms");
	}

	
	
	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame()
	{
		// init everything
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
//					Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
				}
				
			}
			catch (Exception e)
			{
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return frame;
	}
	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			// release the camera
			this.capture.release();
		}
	}
	
	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}
	
	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}
	

	public void startCamera()
	{
		if (!this.cameraActive)
		{
			// start the video capture
			this.capture.open(cameraId);
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						// effectively grab and process a single frame
						Mat frame = grabFrame();
						
						//find template in frame
						
						Rect templateMatch = findTemplate(frame, templateImg, new Mat());
//						draw max rectangle
						Imgproc.rectangle(frame, templateMatch.tl(), templateMatch.br(), new Scalar(0,0,255));
						
						
						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(camPanel, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
			}
			else
			{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// stop the timer
			this.stopAcquisition();
		}
	}
	
}
