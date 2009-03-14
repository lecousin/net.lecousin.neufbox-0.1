package net.lecousin.neufbox.mediacenter.eclipse;

import net.lecousin.framework.ui.eclipse.EclipseImages;
import net.lecousin.neufbox.mediacenter.eclipse.internal.EclipsePlugin;

import org.eclipse.swt.graphics.Image;

public class NeufBoxMediaCenter {

	public static Image getIcon() {
		return EclipseImages.getImage(EclipsePlugin.ID, "images/icon.bmp");		
	}
	
}
