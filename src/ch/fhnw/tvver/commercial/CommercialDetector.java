package ch.fhnw.tvver.commercial;

import java.io.*;
import java.util.*;

import ch.fhnw.ether.image.Frame;
import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.ether.video.VideoFrame;

public class CommercialDetector extends AbstractDetector {
	double start;
	double duration;
	public double time = 0;
	public int frameRate = 50; // Approx. check every 1.5 Seconds
	private int[][] comparisonColor = new int[80][3];
	private int[] comparisonRating = new int[80];
	private int[][] comparisonPosition = new int[80][2];

	private Logo logo;
	private File storageDirectory;

	private List<Integer> feature = new ArrayList<Integer>();

	public Map segments = new HashMap<Integer, Segment>();

	public int frameCounter = 0;
	private boolean training;
	
	@Override
	protected void init(URLVideoSource videoSource, File storageDirectory, boolean training) {
		this.training = training;
		this.storageDirectory = storageDirectory;
		if(!training){
			try {
				FileInputStream fout = new FileInputStream(storageDirectory.getAbsolutePath() + "/logoFeatures");
				ObjectInputStream ois = new ObjectInputStream(fout);
				logo = (Logo)ois.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	@Override
	protected void process(VideoFrame videoFrame, float[] audio, List<Segment> result) {
		// process video frame

		frameCounter++;
		if(training){
			if(frameCounter <= 1){
				// Init
				Frame rightCorner = extractCorner(videoFrame);

				int i = 0;
				for(int row=1; row<=8; row++){
					for(int cell=1; cell<=10; cell++){
						int[] color = new int[3];
						rightCorner.getRGBUnsigned( (rightCorner.width - cell*5), (rightCorner.height - row*5), color);

						comparisonColor[i] = color;
						comparisonPosition[i][0] = (rightCorner.width - cell*5);
						comparisonPosition[i][1] = (rightCorner.height - row*5);

						System.out.println("Color "+i + " is red "+color[0] +" green "+color[1] + "blue "+ color[2]);
						i++;
					}
				}
			}else if(frameCounter < 1000){
				Frame rightCorner = extractCorner(videoFrame);
				int i = 0;
				int maxRating = 0;
				for(int row=1; row<=8; row++) {
					for (int cell = 1; cell <= 10; cell++) {
						int[] color = new int[3];
						rightCorner.getRGBUnsigned((rightCorner.width - cell * 5), (rightCorner.height - row * 5), color);
						if (Math.abs((comparisonColor[i][0] - color[0])) < 10) {
							comparisonRating[i]++;
						}
						if (Math.abs((comparisonColor[i][1] - color[1])) < 10) {
							comparisonRating[i]++;
						}
						if (Math.abs((comparisonColor[i][2] - color[2])) < 10) {
							comparisonRating[i]++;
						}

						if (comparisonRating[i] > maxRating) {
							maxRating = comparisonRating[i];
						}
						i++;
					}
				}

			}else {
				if (frameCounter == 1000 || videoFrame.isLast()) {
					int[] sortedRating = comparisonRating.clone();
					Arrays.sort(sortedRating);

					for (int i = 0; i < sortedRating.length; i++) {
						if (sortedRating[0] == comparisonRating[i]) {
							feature.add(i);
							System.out.println("Feature added " + comparisonColor[i][0] + "" + comparisonColor[i][1] + "" + comparisonColor[i][2]);
						} else if (sortedRating[1] == comparisonRating[i]) {
							feature.add(i);
							System.out.println("Feature added " + comparisonColor[i][0] + "" + comparisonColor[i][1] + "" + comparisonColor[i][2]);
						} else if (sortedRating[2] == comparisonRating[i]) {
							feature.add(i);
							System.out.println("Feature added " + comparisonColor[i][0] + "" + comparisonColor[i][1] + "" + comparisonColor[i][2]);
						}
					}

					// Create Logo and save for future use
					logo = new Logo();
					logo.comparisonColor = comparisonColor;
					logo.comparisonPosition = comparisonPosition;
					logo.feature = feature;
					try {
						FileOutputStream fout = new FileOutputStream(storageDirectory.getAbsolutePath() + "/logoFeatures");
						ObjectOutputStream oos = new ObjectOutputStream(fout);
						oos.writeObject(logo);

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		// Training end
		else{
			// Detect if its commercial
			if( (frameCounter % frameRate) <= 0 ){ // check every frameRate
				Frame rightCorner = extractCorner(videoFrame);

				// create and add segment when commercial/non-commercial block is detected
				/*Segment segment = new Segment(start, duration);
				result.add(segment);
				if(videoFrame.isLast()) {
					// do some post-processing on segments here
				}*/

				// ToDo: Add segments with correct start time
				if(isCommercial(rightCorner)) {
					// ToDo: Some false positive exists, but with the neighbour frames it could be estimated if it's commercial
					try {
						// Saves corners to disk for debuging
						rightCorner.write(new File("Frame" + frameCounter), Frame.FileFormat.JPEG);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Finds out, if the given frame could potentially be a commercial
	 * @param frame to analyze
	 * @return true, if commercial
	 */
	private boolean isCommercial(Frame frame){
		// Try best match
		int[] color = new int[3];


		double colorDiff = 0;
		for(Integer currentFeature : logo.feature){
			frame.getRGBUnsigned(logo.comparisonPosition[currentFeature][0], logo.comparisonPosition[currentFeature][1], color);
			colorDiff += Math.sqrt( Math.pow(logo.comparisonColor[currentFeature][0]-color[0], 2) + Math.pow(logo.comparisonColor[currentFeature][1]-color[1], 2) + Math.pow(logo.comparisonColor[currentFeature][2]-color[2], 2) );
		}

		System.out.println(frameCounter + " color " +colorDiff);

		if(colorDiff > logo.feature.size()*125){
			return true;
		}else{
			return false;
		}
	}

	private Frame extractCorner(VideoFrame video){
		return video.getFrame().getSubframe(video.getFrame().width-80, video.getFrame().height-60, 80, 60);
	}

}
