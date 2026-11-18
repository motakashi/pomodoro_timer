package motakashi.androidapp.pomodorotimer.e2e;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public final class AndroidDriverFactory {
    private static final String DEFAULT_SERVER_URL = "http://127.0.0.1:4723";
    private static final String APP_PACKAGE = "motakashi.androidapp.pomodorotimer";
    private static final String APP_ACTIVITY = ".MainActivity";

    private AndroidDriverFactory() {
    }

    public static AndroidDriver create() throws URISyntaxException, java.net.MalformedURLException {
        String appPath = System.getProperty("appium.app");
        if (appPath == null || appPath.isBlank()) {
            throw new IllegalStateException("The appium.app system property must point to the debug APK.");
        }

        String serverUrl = System.getProperty("appium.serverUrl", DEFAULT_SERVER_URL);
        String deviceName = System.getProperty("appium.deviceName", "Android Emulator");

        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setAutomationName("UiAutomator2")
                .setDeviceName(deviceName)
                .setApp(Path.of(appPath).toAbsolutePath().normalize().toString())
                .setAppPackage(APP_PACKAGE)
                .setAppActivity(APP_ACTIVITY)
                .setAutoGrantPermissions(true);

        return new AndroidDriver(new URI(serverUrl).toURL(), options);
    }
}
