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
					int[] windowsfilepane={2,1,0,0,0,0};
					Component c=getBuriedComponent(fc, windowsfilepane);
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
	
	private Component getBuriedComponent(Component comp, int[] tree){
		
		Component next=comp;
		
		for(int n:tree){
			if(next instanceof Container){
				if(n< ((Container) next).getComponentCount()){
					next=((Container) next).getComponent(n);
				}else {return null;}
			}else {return null;}
		}
		return next;
	}
	
}