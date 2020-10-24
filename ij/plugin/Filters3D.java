package ij.plugin;

import ij.*;
import ij.process.*;
import ij.gui.GenericDialog;
import ij.util.ThreadUtil;
import ij.plugin.RGBStackMerge;
import ij.gui.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * This plugin implements most of the 3D filters in the Process/Filters submenu.
 * @author Thomas Boudier
 */
public class Filters3D implements PlugIn {
    public final static int MEAN=10, MEDIAN=11, MIN=12, MAX=13, VAR=14, MAXLOCAL=15;
	private static float xradius = 2, yradius = 2, zradius = 2;
	private static boolean doAllFrms=true;

	public void run(String arg) {
		String name = null;
		int filter = 0;
		if (arg.equals("mean")) {
			name = "3D Mean";
			filter = MEAN;
		} else if (arg.equals("median")) {
			name = "3D Median";
			filter = MEDIAN;
		} else if (arg.equals("min")) {
			name = "3D Minimum";
			filter = MIN;
		} else if (arg.equals("max")) {
			name = "3D Maximum";
			filter = MAX;
		} else if (arg.equals("var")) {
			name = "3D Variance";
			filter = VAR;
		} else
			return;
		ImagePlus imp = IJ.getImage();
		if (imp.isComposite() && imp.getNChannels()==imp.getStackSize()) {
			IJ.error(name, "Composite color images not supported");
			return;
		}
		if (!showDialog(name))
			return;
		imp.startTiming();
		run(imp, filter, xradius, yradius, zradius);
		IJ.showTime(imp, imp.getStartTime(), "", imp.getStackSize());
	}

	private boolean showDialog(String name) {
		ImagePlus imp = IJ.getImage();
		GenericDialog gd = new GenericDialog(name);
		gd.addNumericField("X radius:", xradius, 1);
		gd.addNumericField("Y radius:", yradius, 1);
		gd.addNumericField("Z radius:", zradius, 1);
		if(imp.getNFrames()>1)gd.addCheckbox("Run on all frames?", doAllFrms);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}
		xradius = (float) gd.getNextNumber();
		yradius = (float) gd.getNextNumber();
		zradius = (float) gd.getNextNumber();
		if(imp.getNFrames()>1)doAllFrms=gd.getNextBoolean();
		return true;
	}

	private void run(ImagePlus imp, int filter, float radX, float radY, float radZ) {
		if (imp.isHyperStack()) {
			filterHyperstack(imp, filter, radX, radY, radZ);
			return;
		}
		ImageStack res = filter(imp.getStack(), filter, radX, radY, radZ);
		imp.setStack(res);
	}
	
	public static ImageStack filter(ImageStack stackorig, int filter, float vx, float vy, float vz) {
	
		if (stackorig.getBitDepth()==24)
			return filterRGB(stackorig, filter, vx, vy, vz);

		// get stack info
		final ImageStack stack = stackorig;
		final float voisx = vx;
		final float voisy = vy;
		final float voisz = vz;
		final int width= stack.getWidth();
		final int height= stack.getHeight();
		final int depth= stack.size();
		ImageStack res = null;
		
		if ((filter==MEAN) || (filter==MEDIAN) || (filter==MIN) || (filter==MAX) || (filter==VAR)) {
			if (filter==VAR)
				res = ImageStack.create(width, height, depth, 32);
			else
				res = ImageStack.create(width, height, depth, stackorig.getBitDepth());
			IJ.showStatus("3D filtering...");
			// PARALLEL 
			final ImageStack out = res;
			final AtomicInteger ai = new AtomicInteger(0);
			final int n_cpus = Prefs.getThreads();

			final int f = filter;
			final int dec = (int) Math.ceil((double) stack.size() / (double) n_cpus);
			Thread[] threads = ThreadUtil.createThreadArray(n_cpus);
			for (int ithread = 0; ithread < threads.length; ithread++) {
				threads[ithread] = new Thread() {
					public void run() {
						StackProcessor processor = new StackProcessor(stack);
						for (int k = ai.getAndIncrement(); k < n_cpus; k = ai.getAndIncrement()) {
							processor.filter3D(out, voisx, voisy, voisz, dec * k, dec * (k + 1), f);
						}
					}
				};
			}
			ThreadUtil.startAndJoin(threads);
		}
		return res;
	}
	
	private static void filterHyperstack2(final ImagePlus imp, int filter, float vx, float vy, float vz) {
		
		// get stack info
		final ImageStack stack = imp.getStack();
		if (stack.getBitDepth()==24) filterRGB(stack, filter, vx, vy, vz);
		final float voisx = vx;
		final float voisy = vy;
		final float voisz = vz;
		final int width= stack.getWidth();
		final int height= stack.getHeight();
		
		if ((filter==MEAN) || (filter==MEDIAN) || (filter==MIN) || (filter==MAX) || (filter==VAR)) {
			IJ.showStatus("3D filtering...");
			// PARALLEL 
			int bitdepth=stack.getBitDepth();
			if(filter==VAR) bitdepth=32;
			final ImageStack out = ImageStack.create(width, height, stack.getSize(), bitdepth);
			final int frms=imp.getNFrames(), chs=imp.getNChannels();
			final int n_cpus = Prefs.getThreads();
			final int f = filter;
			final int dec = (int) Math.ceil((double) imp.getNSlices() / (double) n_cpus);
			Thread[] threads = ThreadUtil.createThreadArray(n_cpus);
			for(int fr=1; fr<=frms; fr++) {
				for(int ch=1; ch<chs; ch++) {
					final int chf=ch, frf=fr;
					final AtomicInteger ai = new AtomicInteger(0);
					for (int ithread = 0; ithread < threads.length; ithread++) {
						threads[ithread] = new Thread() {
							public void run() {
								StackProcessor processor = new StackProcessor(stack);
								for (int k = ai.getAndIncrement(); k < n_cpus; k = ai.getAndIncrement()) {
									processor.filter3D(out, imp, chf, frf, voisx, voisy, voisz, dec * k, dec * (k + 1), f);
								}
							}
						};
					}
					ThreadUtil.startAndJoin(threads);
				}
			}
			imp.setStack(out, imp.getNChannels(), imp.getNSlices(), imp.getNFrames());
		}
		
	}
	
	private static void filterHyperstack(ImagePlus imp, int filter, float vx, float vy, float vz) {
		//if (imp.getNDimensions()>4) {
		//	IJ.error("5D hyperstacks are currently not supported");
		//	return;
		//}
		int frms=imp.getNFrames(), chs=imp.getNChannels(), sls=imp.getNSlices(), firstfr=0, lastfr=frms;
		if(!doAllFrms) {firstfr=imp.getT()-1; lastfr=imp.getT();}
		for(int fr=firstfr;fr<lastfr;fr++) {
			ImagePlus curimp=imp;
			imp.setT(fr+1);
			if(frms>1) {
				Duplicator dup=new Duplicator();
				curimp=dup.run(imp, 1, chs, 1, sls, fr+1, fr+1);
			}
			if (imp.getNChannels()==1) {
				ImageStack stack = filter(curimp.getStack(), filter, vx, vy, vz);
				curimp.setStack(stack);
				return;
			}
	        ImagePlus[] channels = ChannelSplitter.split(curimp);
	        int n = channels.length;
	        for (int i=0; i<n; i++) {
				ImageStack stack = filter(channels[i].getStack(), filter, vx, vy, vz);
				channels[i].setStack(stack);
			}
			curimp.setImage(RGBStackMerge.mergeChannels(channels, false));
			if(frms>1) {
				for(int sl=0;sl<sls;sl++)
					for(int c=1;c<=chs;c++)
						imp.getStack().setProcessor(curimp.getStack().getProcessor(sl*chs+c), sl*chs+c+fr*sls*chs);
			}
		}
		
	}

	private static ImageStack filterRGB(ImageStack rgb_in, int filter, float vx, float vy, float vz) {
        ImageStack[] channels = ChannelSplitter.splitRGB(rgb_in, false);
		ImageStack red = filter(channels[0], filter, vx, vy, vz);
		ImageStack green = filter(channels[1], filter, vx, vy, vz);
		ImageStack blue = filter(channels[2], filter, vx, vy, vz);
        return RGBStackMerge.mergeStacks(red, green, blue, false);
	}

}
