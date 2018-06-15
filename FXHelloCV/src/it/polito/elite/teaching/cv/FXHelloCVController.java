package it.polito.elite.teaching.cv;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import it.polito.elite.teaching.cv.utils.Utils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
	private Mat grey_testImg = new Mat();
	private Mat result = new Mat();
	

	
	
	public void loadAndShowImage() {
		this.testImg = Imgcodecs.imread("resources/scene4.jpg");
//		Imgproc.cvtColor(imgLoad, this.testImg, Imgproc.COLOR_BGR2GRAY);
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
		templLoad = Imgcodecs.imread("resources/temp4.jpg");
		Imgproc.cvtColor(templLoad, this.templateImg, Imgproc.COLOR_BGR2GRAY);
		if (this.templateImg == null) {
			System.out.println("could not load template");
		}
		else {
			System.out.println("Template loaded succesfully");
			
			Image imageToShow = Utils.mat2Image(this.templateImg);
			updateImageView(templatePanel, imageToShow);
		}
	}
	
	private void processImage(Mat img) {
		long startTime = System.nanoTime();
		
//		System.out.println("Image size = " + testImg.cols() + ", " + testImg.rows());
//		System.out.println("Template size = " + templateImg.cols() + ", " + templateImg.rows());
		
		//create grey versio of color input
		Imgproc.cvtColor(this.testImg, this.grey_testImg , Imgproc.COLOR_BGR2GRAY);
		
		//create result matrix
		result.create(this.grey_testImg.rows() - this.templateImg.rows() + 1, this.grey_testImg.cols() - this.templateImg.cols() + 1, CvType.CV_8U);
		
		//template matching
		Imgproc.matchTemplate(this.grey_testImg, this.templateImg, result, Imgproc.TM_CCOEFF);
		Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
		Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result, new Mat());
		System.out.println("Max val = " + minMaxLocResult.maxVal + ", Min = " + minMaxLocResult.minVal);
		
		
		//draw min and max rectangles
		Imgproc.rectangle(this.testImg, minMaxLocResult.maxLoc, new Point(minMaxLocResult.maxLoc.x + templateImg.cols(), minMaxLocResult.maxLoc.y + templateImg.rows()), new Scalar(255,0,0));
		Imgproc.rectangle(this.testImg, minMaxLocResult.minLoc, new Point(minMaxLocResult.minLoc.x + templateImg.cols(), minMaxLocResult.minLoc.y + templateImg.rows()), new Scalar(0,0,255));
		
		//multiply result for viewing
		Core.multiply(result, new Scalar(255), result);
		
		//convert to image and show
		MatOfByte byteMat = new MatOfByte();
		Imgcodecs.imencode(".bmp", result, byteMat);
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
					Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
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
