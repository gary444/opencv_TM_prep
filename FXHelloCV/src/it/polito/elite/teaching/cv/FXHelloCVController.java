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
	
	TemplateMatcher tempMatcher = new TemplateMatcher();
	
	public void initialise() {
		
		//static test image
		Mat testImg = tempMatcher.loadTestImage();
		System.out.println("img dims: " + testImg.cols() + " x " + testImg.rows());
		processSingleImage(testImg);
		
		//start camera
		startCamera();
	}
	
	private void processSingleImage(Mat img) {
		long startTime = System.nanoTime();
		
		Mat response = new Mat(img.rows(), img.cols(), CvType.CV_8U);
		
		try {
			Rect matchRect = tempMatcher.findTemplate(img, response);
			//draw max rectangle
			Imgproc.rectangle(img, matchRect.tl(), matchRect.br(), new Scalar(0,0,255));
			
			
			
			//show result
			Image imageToShow = Utils.mat2Image(img);
			updateImageView(inputImgPanel, imageToShow);

			//convert response to image and show
			MatOfByte byteMat = new MatOfByte();
			Imgcodecs.imencode(".bmp", response, byteMat);
			imageToShow = new Image(new ByteArrayInputStream(byteMat.toArray()));
			updateImageView(outputImgPanel, imageToShow);
			
			System.out.println("Execution of single image processing: " + (System.nanoTime() - startTime)/1000000 + " ms");
		}
		catch (Exception e) {
			System.err.println("procesSingleImage: " + e.getMessage());
		}


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
						Rect templateMatch = tempMatcher.findTemplate(frame);
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
	

	
}
