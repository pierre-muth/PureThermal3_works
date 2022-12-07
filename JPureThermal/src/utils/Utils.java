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

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.Version;
import org.freedesktop.gstreamer.device.Device;
import org.freedesktop.gstreamer.device.DeviceMonitor;

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
    
    public static void listDevices() {
		DeviceMonitor dm = new DeviceMonitor();
		dm.start();
		List<Device> dlist = dm.getDevices();
		dm.stop();
		dm.close();
		dm.dispose();
		System.out.println(dlist.size()+" devices:");
		for (Device device : dlist) {
			if (device.getDeviceClass().contains("Video")){
				System.out.print("a: "+device.getDeviceClass()+", ");
				System.out.print("b: "+device.getDisplayName()+", ");
				System.out.print("c: "+device.getName()+", ");
				System.out.print("d: "+device.getRefCount()+", ");
				System.out.println("e: "+device.getTypeName()+":");
				
				
				System.out.println("caps: ");
				Caps caps = device.getCaps();
				String[] capss = caps.toString().split("; ");
				
				for (String cap : capss) {
					System.out.println("   > "+cap);
				}

				System.out.println("Properties:");
				Structure struct = device.getProperties();
				if (struct == null) {
					System.out.println("");
					continue;
				}
				String[] structs = struct.toString().split(", ");
				
				for (String str : structs) {
					System.out.println("   > "+str);
				}
				
				System.out.println("");


			}
		}
	}

    public static void main(String[] args) {
    	configurePaths();
		Gst.init(Version.BASELINE, "utils");
    	listDevices();
    	
    }

}
