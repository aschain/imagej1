/*
 * 2017
 *
 */
 
package ij.io;
 
import ij.IJ;
import javax.swing.*;
import java.beans.*;
import java.awt.*;
import java.io.File;
 
/* PrevFolderSelector.java */
public class PrevFolderSelector extends JComponent
                          implements PropertyChangeListener {
	JFileChooser fc;
 
    public PrevFolderSelector(JFileChooser fc) {
		this.fc=fc;
        fc.addPropertyChangeListener(this);
    }
 
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
 
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
            File oldDir = (File) e.getOldValue();
			File newDir = (File) e.getNewValue();
            if(newDir.getPath().equals(oldDir.getParent())) {
				if(UIManager.getLookAndFeel().getName().equals("Windows") || UIManager.getLookAndFeel().getName().equals("Windows Classic")){
					Component c=fc.findComponentAt(106,40);
					if(c!=null && c instanceof JComponent){
						c.requestFocusInWindow();
					}
				}
					fc.setSelectedFile(oldDir);
			}
 
        //If a file became selected, find out which one.
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
            //file = (File) e.getNewValue();
        }
    }
	
}