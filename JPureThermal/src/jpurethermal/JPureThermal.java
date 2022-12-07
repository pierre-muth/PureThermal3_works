package jpurethermal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.Version;
import org.freedesktop.gstreamer.device.Device;
import org.freedesktop.gstreamer.device.DeviceMonitor;

import cern.jdve.Chart;
import cern.jdve.ChartInteractor;
import cern.jdve.graphic.CustomPalette;
import cern.jdve.renderer.ContourChartRenderer;
import cern.jdve.renderer.PolylineChartRenderer;
import cern.jdve.utils.DataRange;
import utils.Utils;

public class JPureThermal extends JPanel {

	private static Pipeline pipeline;
	private GstVideoComponentThermal videosComponent;

	public JPureThermal() {
		// mfvideosrc device.path="\\\\\?\\usb\#vid_1e4e\&pid_0100\&mi_00\#6\&d03da6\&0\&0000\#\{e5323777-f976-4f5b-9b55-b94699c46e44\}\\global"     -> PureThermal (fw:v1.3.0)
		
		
		videosComponent = new GstVideoComponentThermal();

//		Bin bin = Gst.parseBinFromDescription(
//				"mfvideosrc device-path=\"\\\\\\\\\\?\\\\usb\\#vid_1e4e\\&pid_0100\\&mi_00\\#6\\&d03da6\\&0\\&0000\\#\\{e5323777-f976-4f5b-9b55-b94699c46e44\\}\\\\global\""
//						+ " ! capsfilter caps=video/x-raw,width=160,height=120,framerate=10000000/1111111,format=GRAY16_LE",
//						true);

		Bin bin = Gst.parseBinFromDescription(
				"mfvideosrc device-path=\"\\\\\\\\\\?\\\\usb\\#vid_1e4e\\&pid_0100\\&mi_00\\#6\\&d03da6\\&0\\&0000\\#\\{e5323777-f976-4f5b-9b55-b94699c46e44\\}\\\\global\""
						+ " ! capsfilter caps=video/x-raw,width=160,height=120,framerate=10000000/1111111,format=GRAY16_LE",
						true);
		
		pipeline = new Pipeline();
		pipeline.addMany(bin, videosComponent.getElement());
		Pipeline.linkMany(bin, videosComponent.getElement());

		pipeline.play();

		initGUI();
	}

	private void initGUI() {
		setLayout(new BorderLayout());

		Chart histoChart = new Chart();
		histoChart.setPreferredSize(new Dimension(600, 150));
		PolylineChartRenderer plr = new PolylineChartRenderer();
		plr.setDataSet(videosComponent.getHistogramDataSet());
		histoChart.addRenderer(plr);
		histoChart.addInteractor(ChartInteractor.ZOOM);
		histoChart.getXAxis().setRange(new DataRange(-10.0, 50.0));
		//		histoChart.getYScale().setLogarithmic(2);
		add(histoChart, BorderLayout.SOUTH);

		Chart imageChart = new Chart();
		imageChart.setPreferredSize(new Dimension(600, 450));
		CustomPalette cp = new CustomPalette();
		cp.setCustomPalette(new Color[] {Color.white, Color.black, Color.red, Color.yellow, Color.white});
		cp.setValueRange(-10, 50);
		ContourChartRenderer cr = new ContourChartRenderer(cp);
		cr.setDataSet(videosComponent.getImageDataSet());
		imageChart.addRenderer(cr);
		imageChart.addInteractor(ChartInteractor.DATA_PICKER);
		add(imageChart, BorderLayout.CENTER);
	}

	public static void main(String[] args) {

		Utils.configurePaths();
		Gst.init(Version.BASELINE, "JPureThermal", args);
		Utils.listDevices();

		JFrame f = new JFrame("Camera Test");
		f.add(new JPureThermal());
		f.pack();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		f.setVisible(true);

	}

}
