package jpurethermal;

/* 
 * Copyright (c) 2019 Neil C Smith
 * Copyright (c) 2007 Wayne Meissner
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.elements.AppSink;

import cern.jdve.data.DataSet;
import cern.jdve.data.DataSet3D;
import cern.jdve.data.DefaultDataSet;
import cern.jdve.data.DefaultDataSet3D;

/**
 * A Swing component for displaying video from a GStreamer pipeline.
 */
public class GstVideoComponentThermal {

	private final AppSink videosink;

	private int bufferWidth, bufferHeight;
	private DefaultDataSet histogramDataSet = new DefaultDataSet("Histogram", new double[] {0,1}, new double[] {0,1});
	private DefaultDataSet3D imageDataSet = new DefaultDataSet3D("Image");
	
	/**
	 * Create a GstVideoComponent. A new AppSink element will be created that
	 * can be accessed using {@link #getElement()} and added to a pipeline.
	 */
	public GstVideoComponentThermal() {
		this(new AppSink("GstVideoComponentTimeScan"));
	}

	/**
	 * Create a GstVideoComponent wrapping the provided AppSink element.
	 */
	public GstVideoComponentThermal(AppSink appsink) {
		this.videosink = appsink;
		videosink.set("emit-signals", true);
		AppSinkListener listener = new AppSinkListener();
		videosink.connect((AppSink.NEW_SAMPLE) listener);
		videosink.connect((AppSink.NEW_PREROLL) listener);
		StringBuilder caps = new StringBuilder("video/x-raw,pixel-aspect-ratio=1/1,");
		videosink.setCaps(new Caps(caps.toString()));
	}

	/**
	 * Get the wrapped AppSink element.
	 * @return sink element
	 */
	public Element getElement() {
		return videosink;
	}
	
	public DefaultDataSet getHistogramDataSet() {
		return histogramDataSet;
	}
	public DefaultDataSet3D getImageDataSet() {
		return imageDataSet;
	}
	
	private class AppSinkListener implements AppSink.NEW_SAMPLE, AppSink.NEW_PREROLL {
		long lastTimeMs = java.lang.System.currentTimeMillis();
		long nowTimeMs = lastTimeMs;
		int fps = 0;
		double[] histogramX = new double[512];
		double[] histogramY = new double[512];
		double[][] image = new double[8][8];
		
		public AppSinkListener() {
			
			// fill X histogram values
			double stepSize = 60.0/histogramX.length;
			for (int i = 0; i < histogramX.length; i++) {
//				histogramX[i] = ((i*256)-27315)*0.01;
				histogramX[i] = -10.0+(stepSize*i);
			}
		}

		@Override
		public FlowReturn newSample(AppSink elem) {
			Sample sample = elem.pullSample();
			Structure capsStruct = sample.getCaps().getStructure(0);
			int w = capsStruct.getInteger("width");
			int h = capsStruct.getInteger("height");
			int shortValue;
			
			if( h!=bufferHeight || w!=bufferWidth) { 
				bufferHeight = h;
				bufferWidth = w;
				image = new double[bufferHeight][bufferWidth];
			}

			Buffer buffer = sample.getBuffer();
			ByteBuffer bb = buffer.map(false);
			ShortBuffer sb = bb.asShortBuffer();

			if (sb != null) {
				for (int i = 0; i < sb.capacity(); i++) {
					shortValue = Short.toUnsignedInt(sb.get(i));
					image[i/bufferWidth][i%bufferWidth] = (shortValue-27315)*0.01;
				}
				imageDataSet.set(image, false, false);
				
				Arrays.fill(histogramY, 1);
				
				for (int i = 0; i < bufferHeight; i++) {
					for (int j = 0; j < bufferWidth; j++) {
						for (int k = 0; k < histogramX.length-1; k++) {
							if (image[i][j] >= histogramX[k] && image[i][j] < histogramX[k+1] )
								histogramY[k]++;
						}
					}
				}
				histogramDataSet.set(histogramX, histogramY);
				
				buffer.unmap();
			}
			sample.dispose();

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

		@Override
		public FlowReturn newPreroll(AppSink elem) {
			Sample sample = elem.pullPreroll();
			Structure capsStruct = sample.getCaps().getStructure(0);
			int w = capsStruct.getInteger("width");
			int h = capsStruct.getInteger("height");

			if( h!=bufferHeight || w!=bufferWidth) { 
				bufferHeight = h;
				bufferWidth = w;
				image = new double[bufferHeight][bufferWidth];
			}

			Buffer buffer = sample.getBuffer();
			ByteBuffer bb = buffer.map(false);
			ShortBuffer sb = bb.asShortBuffer();

			if (sb != null) {

				buffer.unmap();
			}
			sample.dispose();

			return FlowReturn.OK;
		}


	}
}
