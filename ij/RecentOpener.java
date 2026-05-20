package ij;
import ij.io.*;
import java.awt.*;
import java.io.*;

/** Opens, in a separate thread, files selected from the File/Open Recent submenu.*/
public class RecentOpener implements Runnable {
	private String path;

	RecentOpener(String path) {
		this.path = path;
		Thread thread = new Thread(this, "RecentOpener");
		thread.start();
	}

	/** Open the file and move the path to top of the submenu. */
	public void run() {
		Opener o = new Opener();
		Object result = IJ._hooks.interceptOpenRecent(path);
		if (result==null)
			o.open(path);
		else if (result instanceof String)
			o.open((String)result);
		else if (!(result instanceof ImagePlus))
			return;
		Menu menu = Menus.getOpenRecentMenu();
		int n = menu.getItemCount();
		int index = 0;
		for (int i=0; i<n; i++) {
			if (menu.getItem(i).getLabel().equals(path)) {
				index = i;
				break;
			}
		}
		if (index>0) {
			MenuItem item = menu.getItem(index);
			menu.remove(index);
			menu.insert(item, 0);
		}
	}

}

