/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Neil C Smith - Codelerity Ltd.
 *
 * Copying and distribution of this file, with or without modification,
 * are permitted in any medium without royalty provided the copyright
 * notice and this notice are preserved. This file is offered as-is,
 * without any warranty.
 *
 */
package utils;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.Version;
import org.freedesktop.gstreamer.device.Device;
import org.freedesktop.gstreamer.device.DeviceMonitor;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;

/**
 * Utility methods for use in examples.
 */
public class Utils {

	public Utils() {
	}

	/**
	 * Configures paths to the GStreamer libraries. On Windows queries various
	 * GStreamer environment variables, and then sets up the PATH environment
	 * variable. On macOS, adds the location to jna.library.path (macOS binaries
	 * link to each other). On both, the gstreamer.path system property can be
	 * used to override. On Linux, assumes GStreamer is in the path already.
	 */
	public static void configurePaths() {
		if (Platform.isWindows()) {
			String gstPath = System.getProperty("gstreamer.path", findWindowsLocation());
			if (!gstPath.isEmpty()) {
				String systemPath = System.getenv("PATH");
				if (systemPath == null || systemPath.trim().isEmpty()) {
					Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath);
				} else {
					Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath
							+ File.pathSeparator + systemPath);
				}
			}
		} else if (Platform.isMac()) {
			String gstPath = System.getProperty("gstreamer.path",
					"/Library/Frameworks/GStreamer.framework/Libraries/");
			if (!gstPath.isEmpty()) {
				String jnaPath = System.getProperty("jna.library.path", "").trim();
				if (jnaPath.isEmpty()) {
					System.setProperty("jna.library.path", gstPath);
				} else {
					System.setProperty("jna.library.path", jnaPath + File.pathSeparator + gstPath);
				}
			}

		}
	}

	/**
	 * Query over a stream of possible environment variables for GStreamer
	 * location, filtering on the first non-null result, and adding \bin\ to the
	 * value.
	 *
	 * @return location or empty string
	 */
	public static String findWindowsLocation() {
		if (Platform.is64Bit()) {
			return Stream.of("GSTREAMER_1_0_ROOT_MSVC_X86_64",
					"GSTREAMER_1_0_ROOT_MINGW_X86_64",
					"GSTREAMER_1_0_ROOT_X86_64")
					.map(System::getenv)
					.filter(p -> p != null)
					.map(p -> p.endsWith("\\") ? p + "bin\\" : p + "\\bin\\")
					.findFirst().orElse("");
		} else {
			return "";
		}
	}

	public static String getPureThermalDescriptionForGst() {
		// mfvideosrc device.path="\\\\\?\\usb\#vid_1e4e\&pid_0100\&mi_00\#6\&d03da6\&0\&0000\#\{e5323777-f976-4f5b-9b55-b94699c46e44\}\\global"     -> PureThermal (fw:v1.3.0)
		
		DeviceMonitor dm = new DeviceMonitor();
		dm.start();
		List<Device> dlist = dm.getDevices();
		dm.stop();
		dm.close();
		dm.dispose();
		String description = "mfvideosrc device-path=";

		for (Device device : dlist) {
			Structure propertiesStruct = device.getProperties();
			if (propertiesStruct == null) continue;

			if (device.getDeviceClass().contains("Video") &&
					device.getTypeName().contains("GstMFDevice") &&
					propertiesStruct.getString("device.name").contains("PureThermal")) {
				
				description += "\""+propertiesStruct.getString("device.path") +"\"";
				description += " ! capsfilter caps=video/x-raw,width=160,height=120,framerate=10000000/1111111,format=GRAY16_LE";
				

			}

		}

		System.out.println(description);
		return description;
	}

	public static void printDevicesList() {
		DeviceMonitor dm = new DeviceMonitor();
		dm.start();
		List<Device> dlist = dm.getDevices();
		dm.stop();
		dm.close();
		dm.dispose();
		System.out.println(dlist.size()+" devices:");

		for (Device device : dlist) {
			if (device.getDeviceClass().contains("Video") && device.getTypeName().contains("GstMFDevice")){
				System.out.print("Class: "+device.getDeviceClass()+", ");
				System.out.print("DisplayName: "+device.getDisplayName()+", ");
				System.out.print("Name: "+device.getName()+", ");
				System.out.print("RefCount: "+device.getRefCount()+", ");
				System.out.println("TypeName: "+device.getTypeName()+":");

				System.out.println("Caps: ");
				Caps caps = device.getCaps();
				String[] capsStringArray = caps.toString().split("; ");

				for (String capString : capsStringArray) {
					System.out.println("   > "+capString);
				}

				for ( int i=0; i<caps.size(); i++ ){
					Structure s = caps.getStructure(i);
					System.out.print("  getName(): "+s.getName());
					System.out.print("  getString(\"format\"): "+s.getString("format"));
					System.out.print("  getFraction(\"framerate\"): "+s.getFraction("framerate").numerator+"/"+s.getFraction("framerate").denominator );
					System.out.print("   getInteger(\"width\"): "+s.getInteger("width"));
					System.out.println("   getInteger(\"height\"): "+s.getInteger("height"));
				}

				System.out.println("Properties:");
				Structure propertiesStruct = device.getProperties();
				if (propertiesStruct == null) {
					System.out.println("");
					continue;
				}

				String[] structStingArray = propertiesStruct.toString().split(", ");
				for (String str : structStingArray) {
					System.out.println("   > "+str);
				}

				System.out.println("  getName(): "+propertiesStruct.getName());
				System.out.println("  getString(\"device.path\"): "+propertiesStruct.getString("device.path"));
				System.out.println("  getString(\"device.name\"): "+propertiesStruct.getString("device.name"));

				System.out.println("");


			}
		}

	}

	public static void main(String[] args) {
		configurePaths();
		Gst.init(Version.BASELINE, "utils");
		printDevicesList();
		getPureThermalDescriptionForGst();
	}

}
