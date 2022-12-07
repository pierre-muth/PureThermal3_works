package jpurethermal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import cern.jdve.Style;
import cern.jdve.graphic.CustomPalette;
import cern.jdve.interactor.CursorInteractor;
import cern.jdve.renderer.BarChartRenderer;
import cern.jdve.renderer.ContourChartRenderer;
import cern.jdve.renderer.PolylineChartRenderer;
import cern.jdve.utils.DataRange;
import utils.Utils;

public class JPureThermal extends JPanel {

	private static Pipeline pipeline;
	private GstVideoComponentThermal videosComponent;
	private CursorInteractor verticalCursorInteractor;
	private CursorInteractor horizontalCursorInteractor;
	
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
		setLayout(new GridBagLayout());

		Chart histoChart = new Chart();
		histoChart.setPreferredSize(new Dimension(200, 200));
		BarChartRenderer bcr = new BarChartRenderer();
		bcr.setStyle(0, new Style(Color.black));
		bcr.setDataSet(videosComponent.getHistogramDataSet());
		histoChart.addRenderer(bcr);
		histoChart.getYScale().setLogarithmic(10);
		add(histoChart, new GridBagConstraints(2, 0, 1, 1, 0.1, 0.1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		
		Chart horizontalProjectionChart = new Chart();
		horizontalProjectionChart.setPreferredSize(new Dimension(100, 200));
		CustomPalette cphp = new CustomPalette();
		cphp.setCustomPalette(new Color[] {Color.white, Color.black, Color.magenta.darker(), Color.red, Color.yellow, Color.white});
		ContourChartRenderer crhp = new ContourChartRenderer(cphp);
		crhp.setDataSet(videosComponent.gethorizontalProjectionDataSet());
		horizontalProjectionChart.addRenderer(crhp);
		horizontalProjectionChart.setYScaleVisible(false);
//		verticalCursorInteractor = new CursorInteractor(true);
//		horizontalProjectionChart.addInteractor(verticalCursorInteractor);
		add(horizontalProjectionChart, new GridBagConstraints(0, 0, 1, 1, 0.1, 0.1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		Chart verticalProjectionChart = new Chart();
		verticalProjectionChart.setPreferredSize(new Dimension(300, 100));
		CustomPalette cpvp = new CustomPalette();
		cpvp.setCustomPalette(new Color[] {Color.white, Color.black, Color.magenta.darker(), Color.red, Color.yellow, Color.white});
		ContourChartRenderer crvp = new ContourChartRenderer(cpvp);
		crvp.setDataSet(videosComponent.getverticalProjectionDataSet());
		verticalProjectionChart.addRenderer(crvp);
		verticalProjectionChart.setXScaleVisible(false);
//		horizontalCursorInteractor = new CursorInteractor(false);
//		verticalProjectionChart.addInteractor(horizontalCursorInteractor);
		add(verticalProjectionChart, new GridBagConstraints(1, 1, 1, 1, 0.1, 0.1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		Chart imageChart = new Chart();
		imageChart.setPreferredSize(new Dimension(300, 200));
		CustomPalette cp = new CustomPalette();
		cp.setCustomPalette(new Color[] {Color.white, Color.black, Color.magenta.darker(), Color.red, Color.yellow, Color.white});
		ContourChartRenderer cr = new ContourChartRenderer(cp);
		cr.setDataSet(videosComponent.getImageDataSet());
		imageChart.addRenderer(cr);
//		DataPickerInteractor dpi = new DataPickerInteractor(false, false, true);
//		ValueInteractor vi = new ValueInteractor(verticalCursorInteractor, horizontalCursorInteractor);
//		dpi.addChartInteractionListener(vi);
//		imageChart.addInteractor(dpi);
		add(imageChart, new GridBagConstraints(1, 0, 1, 1, 0.1, 0.1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

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
