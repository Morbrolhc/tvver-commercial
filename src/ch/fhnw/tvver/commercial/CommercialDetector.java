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
	public int frameRate = 50; // Rate of frames to check for commercial
	private int[][] comparisonColor;
	private int[] comparisonRating;
	private int[][] comparisonPosition;
	private int RATINGDIFFERENCE = 50;

	private Logo logo;
	private File storageDirectory;

	private List<Integer> feature = new ArrayList<Integer>();

    private List<Integer> commercialFrames = new ArrayList<Integer>();
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
				Frame frame = videoFrame.getFrame(); //extractCorner(videoFrame);
				int arraySize = (frame.width / 2) * (frame.height / 2);
                System.out.println("Arraysize " + arraySize + " width: "+ frame.width + " height: " + frame.height);
				comparisonColor = new int[arraySize][3];
				comparisonRating = new int[arraySize];
				comparisonPosition = new int[arraySize][2];

				int i = 0;
				for(int row=1; row<=frame.height-1; row+=2){
					for(int cell=1; cell<=frame.width-1; cell+=2){
						int[] color = new int[3];
						frame.getRGBUnsigned( (frame.width - cell), (frame.height - row), color);

						comparisonColor[i] = color;
						comparisonPosition[i][0] = (frame.width - cell);
						comparisonPosition[i][1] = (frame.height - row);

						/*System.out.println("Color "+ i + " is red "+color[0] +" green "+color[1] + "blue "+ color[2] +
								" at position " + comparisonPosition[i][0] + " " + comparisonPosition[i][1]);*/
						i++;
					}
				}
                try {
                    // Saves corners to disk for debuging
                    frame.write(new File("Comparison" + frameCounter), Frame.FileFormat.JPEG);
                } catch (IOException e) {
                    e.printStackTrace();
                }
			// Learn over 1000 Frames
			}else if(frameCounter < 1000){
				Frame frame = videoFrame.getFrame(); // extractCorner(videoFrame);
				int i = 0;
				int maxRating = 0;
				for(int row=1; row<=frame.height-1; row+=2) {
					for (int cell=1; cell <= frame.width-1; cell+=2) {
						int[] color = new int[3];
						frame.getRGBUnsigned((frame.width - cell), (frame.height - row), color);
						/*if (Math.abs((comparisonColor[i][0] - color[0])) < RATINGDIFFERENCE &&
								Math.abs((comparisonColor[i][1] - color[1])) < RATINGDIFFERENCE &&
								Math.abs((comparisonColor[i][2] - color[2])) < RATINGDIFFERENCE) {
							comparisonRating[i]++;
						}*/
                        if(Math.sqrt( Math.pow(comparisonColor[i][0]-color[0], 2) +
                                Math.pow(comparisonColor[i][1]-color[1], 2) + Math.pow(comparisonColor[i][2]-color[2], 2) ) < RATINGDIFFERENCE){
                            comparisonRating[i]++;
                        }

                        /*System.out.println("Diff of " + i + " is " + Math.sqrt( Math.pow(comparisonColor[i][0]-color[0], 2) +
                                Math.pow(comparisonColor[i][1]-color[1], 2) + Math.pow(comparisonColor[i][2]-color[2], 2) ));*/
						// Check if its the best rating
						if (comparisonRating[i] > maxRating) {
							maxRating = comparisonRating[i];
                            //System.out.println("max Rating is" + maxRating);
						}
						i++;
					}
				}

			}else {
				if (frameCounter == 2000 || videoFrame.isLast()) {
					int[] sortedRating = comparisonRating.clone();
					Arrays.sort(sortedRating);
                    System.out.println("Sorted max is "+ sortedRating[sortedRating.length-1]);
					for (int i = 0; i < sortedRating.length; i++) {
						if (sortedRating[sortedRating.length-1] == comparisonRating[i]) {
							feature.add(sortedRating.length-1);
							System.out.println(comparisonRating[i]);
							System.out.println("Position "+ comparisonPosition[i][0] + "," + comparisonPosition[i][1]);
							System.out.println("Feature"+i+": rgb(" + comparisonColor[i][0] + "," + comparisonColor[i][1] + "," + comparisonColor[i][2] + ")" );
						}
                        else if (sortedRating[sortedRating.length-2] == comparisonRating[i]) {
                            feature.add(i);
							System.out.println(comparisonRating[i]);
                            System.out.println("Position "+ comparisonPosition[i][0] + "," + comparisonPosition[i][1]);
                            System.out.println("Feature"+i+": rgb(" + comparisonColor[i][0] + "," + comparisonColor[i][1] + "," + comparisonColor[i][2] + ")" );
                        }
                        else if (sortedRating[sortedRating.length-3] == comparisonRating[i]) {
                            feature.add(i);
							System.out.println(comparisonRating[i]);
							System.out.println("Position "+ comparisonPosition[i][0] + "," + comparisonPosition[i][1]);
                            System.out.println("Feature"+i+": rgb(" + comparisonColor[i][0] + "," + comparisonColor[i][1] + "," + comparisonColor[i][2] + ")" );
                        }
						else if (sortedRating[sortedRating.length-4] == comparisonRating[i]) {
							feature.add(i);
							System.out.println(comparisonRating[i]);
							System.out.println("Position "+ comparisonPosition[i][0] + "," + comparisonPosition[i][1]);
							System.out.println("Feature"+i+": rgb(" + comparisonColor[i][0] + "," + comparisonColor[i][1] + "," + comparisonColor[i][2] + ")" );
						}
						else if (sortedRating[sortedRating.length-5] == comparisonRating[i]) {
							feature.add(i);
							System.out.println(comparisonRating[i]);
							System.out.println("Position "+ comparisonPosition[i][0] + "," + comparisonPosition[i][1]);
							System.out.println("Feature"+i+": rgb(" + comparisonColor[i][0] + "," + comparisonColor[i][1] + "," + comparisonColor[i][2] + ")" );
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
                        System.out.println("Logo created, learning phase completed. Restart without learning");
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
				Frame frame = videoFrame.getFrame(); //extractCorner(videoFrame);

				// create and add segment when commercial/non-commercial block is detected
				/*Segment segment = new Segment(start, duration);
				result.add(segment);
				*/

                if(isCommercial(frame)) {
                    commercialFrames.add(frameCounter);
				}
			}
            if( (frameCounter % 50000) <=0 || videoFrame.isLast() ) {
                int commercialCounter = 0;
                int startCommercial = commercialFrames.get(0);
                int lastCommercial = commercialFrames.get(0);
                System.out.println("CommercialFrames = " + commercialFrames.size());
                for(Integer commercialFrame : commercialFrames){

                    if( (commercialFrame - lastCommercial) < 500){
                        commercialCounter++;
                    }else{
                        if(commercialCounter > 81){
                            double start =(double) (startCommercial / 25 ) - 5;
                            double duration = (double)(lastCommercial - startCommercial)/ 25 - 60;
                            System.out.println("Add Segment Start "+start + " for duration + " +duration + "="+ (start+duration));
                            result.add(new Segment(start, duration, true));
                        }
                        commercialCounter = 0;
                        startCommercial = commercialFrame;
                    }
                    lastCommercial = commercialFrame;

                }
                // do some post-processing on segments here
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
		    // 4:3 detection
            if( color[0] == 0 && color[1] == 0 && color[2] == 0){
                colorDiff += 0;
            }else{
                colorDiff += Math.sqrt( Math.pow(logo.comparisonColor[currentFeature][0]-color[0], 2) + Math.pow(logo.comparisonColor[currentFeature][1]-color[1], 2) + Math.pow(logo.comparisonColor[currentFeature][2]-color[2], 2) );
            }
        }

		if(colorDiff > logo.feature.size()*75){
            return true;
		}else{
			return false;
		}
	}

	private Frame extractCorner(VideoFrame video){
		return video.getFrame().getSubframe(video.getFrame().width-80, video.getFrame().height-60, 80, 60);
	}
}
