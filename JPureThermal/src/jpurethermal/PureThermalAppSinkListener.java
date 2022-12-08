package jpurethermal;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.elements.AppSink;

import cern.jdve.data.DefaultDataSet;
import cern.jdve.data.DefaultDataSet3D;

public class PureThermalAppSinkListener implements AppSink.NEW_SAMPLE {
	public static final int HISTOGRAM_RESOLUTION = 128;
	private int bufferWidth, bufferHeight;
	private DefaultDataSet histogramDataSet = new DefaultDataSet("Histogram", new double[] {0,1}, new double[] {0,1});
	private DefaultDataSet3D imageDataSet = new DefaultDataSet3D("Image");
	private DefaultDataSet3D horizontalProjectionDataSet = new DefaultDataSet3D("Horizontal Projection");
	private DefaultDataSet3D verticalProjectionDataSet = new DefaultDataSet3D("Vertical Projection");
	private double pixMin = 0;
	private double pixMax = 10;
	private long lastTimeMs = java.lang.System.currentTimeMillis();
	private long nowTimeMs = lastTimeMs;
	private int fps = 0;
	private double[] histogramX = new double[HISTOGRAM_RESOLUTION];
	private double[] histogramY = new double[HISTOGRAM_RESOLUTION];
	private double[][] image = new double[8][8];
	private double[][] horProjection = new double[8][8];
	private double[][] vertProjection = new double[8][8];
	private double[] vertPixelScale = new double[8];
	private double[] horPixelScale = new double[8];
	private double min = Double.MAX_VALUE;
	private double max = Double.MIN_VALUE;

	public PureThermalAppSinkListener() {
		
	}

	@Override
	public FlowReturn newSample(AppSink elem) {
		Sample sample = elem.pullSample();
		Structure capsStruct = sample.getCaps().getStructure(0);
		int w = capsStruct.getInteger("width");
		int h = capsStruct.getInteger("height");
		double pixValue;

		// init arrays
		if( h!=bufferHeight || w!=bufferWidth) { 
			bufferHeight = h;
			bufferWidth = w;
			image = new double[bufferHeight][bufferWidth];
			vertProjection = new double[HISTOGRAM_RESOLUTION][bufferWidth];
			horProjection = new double[bufferHeight][HISTOGRAM_RESOLUTION];
			vertPixelScale = new double[bufferHeight];
			for(int i=0; i<vertPixelScale.length; i++) vertPixelScale[i] = i;
			horPixelScale = new double[bufferWidth];
			for(int i=0; i<horPixelScale.length; i++) horPixelScale[i] = i;
		}

		Buffer buffer = sample.getBuffer();
		ByteBuffer bb = buffer.map(false);
		ShortBuffer sb = bb.asShortBuffer();

		if (sb != null) {
			min = Double.MAX_VALUE;
			max = Double.MIN_VALUE;

			// convert to degrees, fill the image, find min/max
			for (int i = 0; i < sb.capacity(); i++) {
				pixValue = Short.toUnsignedInt(sb.get(i));
				pixValue = (pixValue-27315.0)*0.01;
				if (pixValue > max) max = pixValue;
				if (pixValue < min) min = pixValue;
				image[i/bufferWidth][i%bufferWidth] = pixValue;
			}

			// adapt ranges
			imageDataSet.set(horPixelScale, vertPixelScale, image, false, false);
			if(max > pixMax) pixMax+= (max-pixMax)/4;
			else pixMax -= (pixMax-max)/4;
			if (min < pixMin) pixMin -= (pixMin-min)/4;
			else pixMin += (min-pixMin)/4;

			// make X axis histogram values
			double stepSize = (pixMax-pixMin)/histogramX.length;
			for (int i = 0; i < histogramX.length; i++) {
				histogramX[i] = pixMin+(stepSize*i);
			}

			// init arrays
			Arrays.fill(histogramY, 0.0);
			for (int i = 0; i < horProjection.length; i++) {
				Arrays.fill(horProjection[i], 0.0);
			}
			for (int i = 0; i < vertProjection.length; i++) {
				Arrays.fill(vertProjection[i], 0.0);
			}

			// compute histogram and projections datasets
			for (int heightIdx = 0; heightIdx < bufferHeight; heightIdx++) {
				for (int widthIdx = 0; widthIdx < bufferWidth; widthIdx++) {
					for (int k = 0; k < histogramX.length-1; k++) {
						if (image[heightIdx][widthIdx] >= histogramX[k] && image[heightIdx][widthIdx] < histogramX[k+1] ) {
							histogramY[k]++;
							if(horProjection[heightIdx][k] < 16) horProjection[heightIdx][k]++;
							if (vertProjection[histogramX.length-k-1][widthIdx] < 16) vertProjection[histogramX.length-k-1][widthIdx]++;
						}
					}
				}
			}

			// set the data
			histogramDataSet.set(histogramX, histogramY);
			horizontalProjectionDataSet.set(histogramX, vertPixelScale, horProjection, false, false);
			verticalProjectionDataSet.set(horPixelScale, histogramX, vertProjection, false, false);

			buffer.unmap();
		}
		sample.dispose();

		// compute fps
		nowTimeMs = java.lang.System.currentTimeMillis();
		if (nowTimeMs > lastTimeMs+1000) {
			System.out.println(fps+" FPS");
			lastTimeMs = nowTimeMs;
			fps = 0;
		} else {
			fps++;
		}

		return FlowReturn.OK;
	}

	public DefaultDataSet getHistogramDataSet() {
		return histogramDataSet;
	}
	public DefaultDataSet3D getImageDataSet() {
		return imageDataSet;
	}
	public DefaultDataSet3D gethorizontalProjectionDataSet() {
		return horizontalProjectionDataSet;
	}
	public DefaultDataSet3D getverticalProjectionDataSet() {
		return verticalProjectionDataSet;
	}

}