package net.imagej.patcher;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compatibility hook API for IJ1 integrations expecting
 * net.imagej.patcher.LegacyHooks.
 */
public class LegacyHooks {
	private final Map<String, String> menuStructure = new LinkedHashMap<String, String>();
	private boolean enableIJ1PluginDirs = true;
	private final Collection<File> pluginClasspath = new LinkedHashSet<File>();

	public boolean isLegacyMode() { return true; }
	public Object getContext() { return null; }
	public boolean quit() { return true; }
	public void installed() {}
	public void initialized() {}
	public void dispose() {}
	public Object interceptRunPlugIn(final String className, final String arg) { return null; }
	public void showProgress(final double progress) {}
	public void showProgress(final int currentIndex, final int finalIndex) {}
	public void showStatus(final String status) {}
	public void log(final String message) {}
	public void registerImage(final Object image) {}
	public void unregisterImage(final Object image) {}
	public void debug(final String string) { System.err.println(string); }
	public void error(final Throwable t) {}
	public String getAppName() { return "ImageJ"; }
	public String getAppVersion() { return null; }
	public URL getIconURL() { return null; }
	public boolean openInEditor(final String path) { return false; }
	public boolean createInEditor(final String fileName, final String content) { return false; }
	public List<File> handleExtraPluginJars() {
		final List<File> result = new ArrayList<File>();
		result.addAll(pluginClasspath);
		if (!enableIJ1PluginDirs) return result;

		final String extraPluginDirs = System.getProperty("ij1.plugin.dirs");
		if (extraPluginDirs != null) {
			final String[] dirs = extraPluginDirs.split(File.pathSeparator);
			for (int i = 0; i < dirs.length; i++) {
				final File directory = new File(dirs[i]);
				if (directory.isDirectory()) {
					result.add(directory);
					handleExtraPluginJars(directory, result);
				}
			}
			return result;
		}

		final String userHome = System.getProperty("user.home");
		if (userHome != null) {
			final File dir = new File(userHome, ".plugins");
			if (dir.isDirectory()) {
				result.add(dir);
				handleExtraPluginJars(dir, result);
			}
		}
		return result;
	}

	private void handleExtraPluginJars(final File directory, final List<File> result) {
		final File[] list = directory.listFiles();
		if (list == null) return;
		for (int i = 0; i < list.length; i++) {
			if (list[i].isDirectory()) handleExtraPluginJars(list[i], result);
			else if (list[i].isFile() && list[i].getName().endsWith(".jar")) result.add(list[i]);
		}
	}

	protected void enableIJ1PluginDirs(final boolean enable) {
		enableIJ1PluginDirs = enable;
	}

	protected void addPluginClasspath(final File file) {
		pluginClasspath.add(file);
	}
	public void runAfterRefreshMenus() {}
	public boolean handleNoSuchMethodError(final NoSuchMethodError error) { return false; }
	public void newPluginClassLoader(final ClassLoader loader) {}
	public String[] addPluginDirectory(final File directory, final String[] names) { return names; }
	public InputStream autoGenerateConfigFile(final File directory) { return null; }

	public void addMenuItem(final String menuPath, final String command) {
		if (menuPath == null) menuStructure.clear();
		else if (menuPath.startsWith("Help>Examples>")) return;
		else if (menuPath.endsWith(">-")) {
			int i = 1;
			while (menuStructure.containsKey(menuPath + i)) i++;
			menuStructure.put(menuPath + i, command);
		}
		else menuStructure.put(menuPath, command);
	}

	public Map<String, String> getMenuStructure() {
		return Collections.unmodifiableMap(menuStructure);
	}

	@Deprecated
	public Object interceptOpen(final String path, final int planeIndex, final boolean display) { return null; }
	public Object interceptFileOpen(final String path) { return null; }
	public Object interceptOpenImage(final String path, final int planeIndex) { return null; }
	public Object interceptOpenRecent(final String path) { return null; }
	public Object interceptDragAndDropFile(final File f) { return null; }
	public boolean interceptKeyPressed(final KeyEvent e) { return false; }
	public Iterable<Thread> getThreadAncestors() { return null; }
	public boolean interceptCloseAllWindows() { return true; }
	public void interceptImageWindowClose(final Object window) {}
	public boolean disposing() { return true; }

	public static Collection<File> getClasspathElements(final ClassLoader fromClassLoader,
		final StringBuilder errors, final ClassLoader... excludeClassLoaders)
	{
		final Set<File> files = new LinkedHashSet<File>();

		// Java 9+ application loaders are often not URLClassLoader.
		for (ClassLoader loader = fromClassLoader; loader != null; loader = loader.getParent()) {
			if (!(loader instanceof URLClassLoader)) continue;
			final URL[] urls = ((URLClassLoader) loader).getURLs();
			for (int i = 0; i < urls.length; i++) {
				if (!"file".equals(urls[i].getProtocol())) continue;
				files.add(new File(urls[i].getPath()));
			}
		}

		// Fallback for modular/non-URL class loaders.
		final String classPath = System.getProperty("java.class.path");
		if (classPath != null && classPath.length() > 0) {
			final String[] entries = classPath.split(File.pathSeparator);
			for (int i = 0; i < entries.length; i++) {
				if (entries[i] == null || entries[i].length() == 0) continue;
				files.add(new File(entries[i]));
			}
		}

		return new ArrayList<File>(files);
	}
}
