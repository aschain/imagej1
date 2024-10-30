package ij.plugin;
import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.measure.Calibration;

/** This plugin implements the Image/Transform/Flip Z and
	Image/Stacks/Tools/Reverse commands. */
public class StackReverser implements PlugIn {
	
	public static final int CHANNELS=0, SLICES=1, FRAMES=2;
	
	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		if (imp.getStackSize()==1) {
			IJ.error("Flip Z", "This command requires a stack");
			return;
		}
		if (imp.isHyperStack()) {
			GenericDialog gd = new GenericDialog("Which Dimension");
			gd.addChoice("Reverse which dimension?", new String[] {"Channels","Slices","Frames"}, "Slices");
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			int dimension=gd.getNextChoiceIndex();
			flipHyperStack(imp,dimension);
			return;
		}
		flipStack(imp);
	}
	
	public void flipStack(ImagePlus imp) {
		ImageStack stack = imp.getStack();
		int n = stack.size();
		if (n==1)
			return;
		Calibration cal = imp.getCalibration();
		double min = cal.getCValue(imp.getDisplayRangeMin());
		double max = cal.getCValue(imp.getDisplayRangeMax());
 		ImageStack stack2 = new ImageStack(imp.getWidth(), imp.getHeight(), n);
 		for (int i=1; i<=n; i++) {
 			stack2.setPixels(stack.getPixels(i), n-i+1);
 			stack2.setSliceLabel(stack.getSliceLabel(i), n-i+1);
 		}
 		stack2.setColorModel(stack.getColorModel());
		imp.setStack(stack2);
		if (imp.isComposite()) {
			((CompositeImage)imp).reset();
			imp.updateAndDraw();
		}
		IJ.setMinAndMax(imp, min, max);
	}
	
	public void flipHyperStack(ImagePlus imp, int dimension) {
		ImageStack stack = imp.getStack();
		int slices=imp.getNSlices(), channels=imp.getNChannels(), frames=imp.getNFrames(), n=stack.size();
		if(dimension != CHANNELS && dimension!= FRAMES && dimension!=SLICES)dimension=SLICES;
		if((dimension==CHANNELS && channels==1) || (dimension==FRAMES && frames==1) || (dimension==SLICES && slices==1))
			return;
		ImageStack stack2 = new ImageStack(imp.getWidth(), imp.getHeight(), n);
		for(int fr=1; fr<=frames; fr++) {
			for(int sl=1; sl<=slices; sl++) {
				for(int ch=1; ch<=channels; ch++) {
					int tch=ch, tsl=sl, tfr=fr;
					if(dimension==CHANNELS)tch=channels-ch+1;
					else if(dimension==FRAMES)tfr=frames-fr+1;
					else tsl=slices-sl+1;
					stack2.setPixels(stack.getPixels(imp.getStackIndex(ch, sl, fr)),imp.getStackIndex(tch, tsl, tfr));
					stack2.setSliceLabel(stack.getSliceLabel(imp.getStackIndex(ch, sl, fr)),imp.getStackIndex(tch, tsl, tfr));
				}
			}
		}
		stack2.setColorModel(stack.getColorModel());
		if(imp.isComposite()) {
			LUT[] luts=((CompositeImage)imp).getLuts();
			imp.setStack(stack2);
			((CompositeImage)imp).setLuts(luts);
		}else
			imp.setStack(stack2);
	}

}
