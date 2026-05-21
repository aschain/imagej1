package ij;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hook extension points for IJ runtime behavior.
 *
 * <p>The default implementation is intentionally no-op so ImageJ behavior is
 * unchanged unless callers install a custom hooks instance via IJ._hooks(...).</p>
 */
public class LegacyHooks {
	private Map menuStructure = new LinkedHashMap();

	public boolean isLegacyMode() {
		return true;
	}

	public Object getContext() {
		return null;
	}

	public boolean quit() {
		return true;
	}

	public void installed() {
	}

	public void initialized() {
	}

	public void dispose() {
	}

	public Object interceptRunPlugIn(String className, String arg) {
		return null;
	}

	public void showProgress(double progress) {
	}

	public void showProgress(int currentIndex, int finalIndex) {
	}

	public void showStatus(String status) {
	}

	public void log(String message) {
	}

	public void registerImage(Object image) {
	}

	public void unregisterImage(Object image) {
	}

	public void debug(String string) {
		System.err.println(string);
	}

	public void error(Throwable t) {
	}

	public boolean interceptKeyPressed(java.awt.event.KeyEvent e) {
		return false;
	}

	public boolean handleNoSuchMethodError(NoSuchMethodError error) {
		return false;
	}

	public Object interceptFileOpen(String path) {
		return null;
	}

	public Object interceptOpenImage(String path, int sliceIndex) {
		return null;
	}

	public Object interceptOpenRecent(String path) {
		return null;
	}

	public Object interceptDragAndDropFile(File file) {
		return null;
	}

	public boolean interceptCloseAllWindows() {
		return true;
	}

	public void interceptImageWindowClose(Object window) {
	}

	public boolean openInEditor(String path) {
		return false;
	}

	public boolean createInEditor(String title, String text) {
		return false;
	}

	public String getAppVersion() {
		return null;
	}

	public String getAppName() {
		return "ImageJ";
	}

	public URL getIconURL() {
		return null;
	}

	public Iterable getThreadAncestors() {
		return null;
	}

	public Iterable handleExtraPluginJars() {
		return Collections.EMPTY_LIST;
	}

	public String[] addPluginDirectory(File directory, String[] list) {
		return list;
	}

	public void newPluginClassLoader(ClassLoader classLoader) {
	}

	public InputStream autoGenerateConfigFile(File directory) {
		return null;
	}

	public void runAfterRefreshMenus() {
	}

	public void addMenuItem(String menuPath, String command) {
		if (menuPath==null)
			menuStructure.clear();
		else if (menuPath.startsWith("Help>Examples>"))
			return;
		else if (menuPath.endsWith(">-")) {
			int i = 1;
			while (menuStructure.containsKey(menuPath+i))
				i++;
			menuStructure.put(menuPath+i, command);
		}
		else
			menuStructure.put(menuPath, command);
	}

	public Map getMenuStructure() {
		return Collections.unmodifiableMap(menuStructure);
	}
}
