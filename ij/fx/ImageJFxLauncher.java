package ij.fx;

import ij.IJ;
import ij.ImageJ;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Reflection-based JavaFX launcher used by the hybrid migration path.
 *
 * This class avoids compile-time JavaFX dependencies so builds continue to work
 * on JDKs without JavaFX modules installed. Menu actions dispatch through
 * IJ.doCommand(String) so existing command execution remains authoritative.
 */
public final class ImageJFxLauncher {
	private static final Object STARTUP_LOCK = new Object();
	private static volatile boolean fxStarted;
	private static final String[][] FILE_MENU = {
		{"Open...", "Open..."},
		{"Open Recent", "Open Recent"},
		{"-", null},
		{"Quit", "Quit"}
	};
	private static final String[][] IMAGE_MENU = {
		{"Type", "Type"},
		{"Adjust", "Adjust"},
		{"Color", "Color"},
		{"Overlay", "Overlay"},
		{"Properties...", "Properties..."}
	};
	private static final String[][] PROCESS_MENU = {
		{"Smooth", "Smooth"},
		{"Find Edges", "Find Edges"},
		{"Gaussian Blur...", "Gaussian Blur..."},
		{"Subtract Background...", "Subtract Background..."}
	};
	private static final String[][] ANALYZE_MENU = {
		{"Measure", "Measure"},
		{"Set Measurements...", "Set Measurements..."},
		{"Analyze Particles...", "Analyze Particles..."}
	};
	private static final String[][] HELP_MENU = {
		{"About ImageJ...", "About ImageJ..."},
		{"Documentation...", "Documentation..."},
		{"Find Commands...", "Find Commands..."}
	};

	private ImageJFxLauncher() {}

	public static ImageJ launch(int mode) {
		if (!isJavaFxPresent())
			return null;
		if (!ensurePlatformStarted())
			return null;
		ImageJ ij = new ImageJ(null, ImageJ.NO_SHOW);
		if (mode!=ImageJ.NO_SHOW)
			showHybridShell(ij);
		return ij;
	}

	private static boolean isJavaFxPresent() {
		try {
			Class.forName("javafx.application.Platform");
			Class.forName("javafx.stage.Stage");
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	private static boolean ensurePlatformStarted() {
		synchronized (STARTUP_LOCK) {
			if (fxStarted)
				return true;
			try {
				Class<?> platformClass = Class.forName("javafx.application.Platform");
				Method startup = platformClass.getMethod("startup", Runnable.class);
				startup.invoke(null, new Runnable() {
					public void run() {}
				});
				fxStarted = true;
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
	}

	private static void showHybridShell(final ImageJ ij) {
		try {
			Class<?> platformClass = Class.forName("javafx.application.Platform");
			Method runLater = platformClass.getMethod("runLater", Runnable.class);
			runLater.invoke(null, new Runnable() {
				public void run() {
					createAndShowStage(ij);
				}
			});
		} catch (Throwable t) {
			if (ImageJ.DEBUG!=0) {
				// no-op; startup code handles fallback to AWT when this launcher returns null
			}
		}
	}

	private static void createAndShowStage(final ImageJ ij) {
		try {
			Class<?> stageClass = Class.forName("javafx.stage.Stage");
			Class<?> borderPaneClass = Class.forName("javafx.scene.layout.BorderPane");
			Class<?> menuBarClass = Class.forName("javafx.scene.control.MenuBar");
			Class<?> menuClass = Class.forName("javafx.scene.control.Menu");
			Class<?> menuItemClass = Class.forName("javafx.scene.control.MenuItem");
			Class<?> separatorMenuItemClass = Class.forName("javafx.scene.control.SeparatorMenuItem");
			Class<?> labelClass = Class.forName("javafx.scene.control.Label");
			Class<?> sceneClass = Class.forName("javafx.scene.Scene");
			Class<?> nodeClass = Class.forName("javafx.scene.Node");
			Class<?> parentClass = Class.forName("javafx.scene.Parent");
			Class<?> eventHandlerClass = Class.forName("javafx.event.EventHandler");

			Constructor<?> stageCtor = stageClass.getConstructor();
			Object stage = stageCtor.newInstance();
			Object pane = borderPaneClass.getConstructor().newInstance();
			Object menuBar = menuBarClass.getConstructor().newInstance();
			Object label = labelClass.getConstructor(String.class).newInstance(
				"ImageJ JavaFX Hybrid Mode\nMenus are wired to existing ImageJ commands.");

			addMenu(menuBar, menuBarClass, menuClass, menuItemClass, separatorMenuItemClass, eventHandlerClass, "File", FILE_MENU);
			addMenu(menuBar, menuBarClass, menuClass, menuItemClass, separatorMenuItemClass, eventHandlerClass, "Image", IMAGE_MENU);
			addMenu(menuBar, menuBarClass, menuClass, menuItemClass, separatorMenuItemClass, eventHandlerClass, "Process", PROCESS_MENU);
			addMenu(menuBar, menuBarClass, menuClass, menuItemClass, separatorMenuItemClass, eventHandlerClass, "Analyze", ANALYZE_MENU);
			addMenu(menuBar, menuBarClass, menuClass, menuItemClass, separatorMenuItemClass, eventHandlerClass, "Help", HELP_MENU);

			labelClass.getMethod("setStyle", String.class).invoke(label,
				"-fx-font-family: 'SansSerif'; -fx-font-size: 14px; -fx-padding: 16;");
			borderPaneClass.getMethod("setTop", nodeClass).invoke(pane, menuBar);
			borderPaneClass.getMethod("setCenter", nodeClass).invoke(pane, label);

			Object scene = sceneClass.getConstructor(parentClass, double.class, double.class)
				.newInstance(pane, Double.valueOf(620), Double.valueOf(240));

			stageClass.getMethod("setTitle", String.class).invoke(stage, "ImageJ (JavaFX Experimental)");
			stageClass.getMethod("setScene", sceneClass).invoke(stage, scene);

			InvocationHandler closeHandler = new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args) {
					if ("handle".equals(method.getName())) {
						Thread t = new Thread(new Runnable() {
							public void run() {
								ij.quit();
							}
						}, "ImageJ-FX-Close");
						t.setDaemon(true);
						t.start();
					}
					return null;
				}
			};
			Object handler = Proxy.newProxyInstance(
				eventHandlerClass.getClassLoader(),
				new Class[] {eventHandlerClass},
				closeHandler
			);
			stageClass.getMethod("setOnCloseRequest", eventHandlerClass).invoke(stage, handler);
			stageClass.getMethod("show").invoke(stage);
		} catch (Throwable t) {
			// If stage construction fails, AWT compatibility core remains available.
		}
	}

	private static void addMenu(Object menuBar, Class<?> menuBarClass, Class<?> menuClass,
		Class<?> menuItemClass, Class<?> separatorMenuItemClass, Class<?> eventHandlerClass,
		String name, String[][] entries) throws Exception {
		Object menu = menuClass.getConstructor(String.class).newInstance(name);
		Object menuItems = menuClass.getMethod("getItems").invoke(menu);
		Method addItem = menuItems.getClass().getMethod("add", Object.class);
		for (int i=0; i<entries.length; i++) {
			String label = entries[i][0];
			String command = entries[i][1];
			if ("-".equals(label)) {
				Object separator = separatorMenuItemClass.getConstructor().newInstance();
				addItem.invoke(menuItems, separator);
				continue;
			}
			Object item = menuItemClass.getConstructor(String.class).newInstance(label);
			menuItemClass.getMethod("setOnAction", eventHandlerClass).invoke(item,
				createCommandHandler(eventHandlerClass, command));
			addItem.invoke(menuItems, item);
		}
		Object menus = menuBarClass.getMethod("getMenus").invoke(menuBar);
		menus.getClass().getMethod("add", Object.class).invoke(menus, menu);
	}

	private static Object createCommandHandler(Class<?> eventHandlerClass, final String command) {
		InvocationHandler handler = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) {
				if ("handle".equals(method.getName()) && command!=null) {
					IJ.doCommand(command);
				}
				return null;
			}
		};
		return Proxy.newProxyInstance(
			eventHandlerClass.getClassLoader(),
			new Class[] {eventHandlerClass},
			handler
		);
	}
}
