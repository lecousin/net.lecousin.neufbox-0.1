package net.lecousin.neufbox.mediacenter.eclipse.bar;

import net.lecousin.framework.ui.eclipse.EclipseImages;
import net.lecousin.neufbox.mediacenter.eclipse.Local;
import net.lecousin.neufbox.mediacenter.eclipse.SharedDataView;
import net.lecousin.neufbox.mediacenter.eclipse.internal.EclipsePlugin;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class BarContribution extends ContributionItem {

	public static final String ID = "net.lecousin.neufbox.mediacenter.bar";
	
	public BarContribution() {
		this(ID);
	}

	public BarContribution(String id) {
		super(id);
	}

	@Override
	public void fill(ToolBar parent, int index) {
		ToolItem item = new ToolItem(parent, SWT.PUSH);
		item.setImage(EclipseImages.getImage(EclipsePlugin.ID, "images/icon.bmp"));
		item.setToolTipText(Local.Open_Media_Center.toString());
		item.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				SharedDataView.show();
			}
		});
	}
	
}
