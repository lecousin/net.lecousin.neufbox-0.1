package net.lecousin.neufbox.mediacenter.eclipse;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.eclipse.extension.EclipsePluginExtensionUtil;
import net.lecousin.framework.event.Event;
import net.lecousin.framework.event.Event.Listener;
import net.lecousin.framework.log.Log;
import net.lecousin.framework.ui.eclipse.SharedImages;
import net.lecousin.framework.ui.eclipse.UIUtil;
import net.lecousin.framework.ui.eclipse.control.LabelButton;
import net.lecousin.framework.ui.eclipse.dialog.ErrorDlg;
import net.lecousin.neufbox.mediacenter.Folder;
import net.lecousin.neufbox.mediacenter.Item;
import net.lecousin.neufbox.mediacenter.Media;
import net.lecousin.neufbox.mediacenter.MediaCenter;
import net.lecousin.neufbox.mediacenter.eclipse.internal.EclipsePlugin;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class SharedDataView extends ViewPart {

	public static final String ID = "net.lecousin.neufbox.mediacenter.eclipse.SharedDataView";
	
	public static SharedDataView show() {
		try { 
			return (SharedDataView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(SharedDataView.ID);
		}
		catch (PartInitException e) {
			ErrorDlg.exception("NeufBox Media Center", "Unable to open view", EclipsePlugin.ID, e);
			return null;
		}
	}
	
	public static Event<Media> mediaRead = new Event<Media>();
	
	public SharedDataView() {
		
	}

	private MediaCenter mc = null;
	private Composite parent = null;
	private TreeViewer viewer;
	private DropManager drop = new DropManager();
	
	public MediaCenter getMediaCenter() { return mc; }
	
	@Override
	public void dispose() {
		mc.close();
		super.dispose();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
		if (mc == null) {
			try { mc = new MediaCenter(); }
			catch (IOException e) {
				createError(e);
				return;
			}
		}
		createOk();
		mc.itemAdded().addListener(new Listener<Item>() {
			public void fire(Item event) {
				viewer.refresh(event.getParent(), false);
			}
		});
		mc.itemRemoved().addListener(new Listener<Item>() {
			public void fire(Item event) {
				viewer.refresh(event.getParent(), false);
			}
		});
		mc.mediaRead().addListener(new Listener<Media>() {
			public void fire(Media event) {
				mediaRead.fire(event);
			}
		});
		for (IConfigurationElement ext : EclipsePluginExtensionUtil.getExtensionsSubNode(EclipsePlugin.ID, "plugin", "listener")) {
			Listener<SharedDataView> listener;
			try {
				listener = (Listener<SharedDataView>)EclipsePluginExtensionUtil.createInstance(Listener.class, ext, "class", new Object[][] { new Object[] { } });
			} catch (Throwable t) {
				if (Log.error(this))
					Log.error(this, "Unable to instantiate NeufBoxMediaCenter listener", t);
				continue;
			}
			try {
				listener.fire(this);
			} catch (Throwable t) {
				if (Log.error(this))
					Log.error(this, "A NeufBoxMediaCenter listener thrown an exception", t);
			}
		}
	}
	
	private void createError(Throwable t) {
		UIUtil.newLabel(parent, "Error: " + t.getMessage());
	}
	
	private void createOk() {
		Composite panel = UIUtil.newGridComposite(parent, 0, 0, 1);
		
		Composite buttons = UIUtil.newGridComposite(panel, 2, 2, 2);
		LabelButton button;
		button = new LabelButton(buttons);
		button.setImage(SharedImages.getImage(SharedImages.icons.x16.file.NEW_FOLDER));
		button.setToolTipText(Local.Create_new_folder.toString());
		button.addClickListener(new NewFolder());
		button = new LabelButton(buttons);
		button.setImage(SharedImages.getImage(SharedImages.icons.x16.basic.DEL));
		button.setToolTipText(Local.Remove.toString());
		button.addClickListener(new Remove());
		
		
		viewer = new TreeViewer(panel);
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(mc.getRoot());
		viewer.getTree().setLayoutData(UIUtil.gridData(1, true, 1, true));
		viewer.addDropSupport(DND.DROP_LINK, drop.getTransfers(), new DropTargetListener() {
			public void dragEnter(DropTargetEvent event) {
				drop.dragEnter(event);
			}
			public void dragLeave(DropTargetEvent event) {
				drop.dragLeave(event);
			}
			public void dragOperationChanged(DropTargetEvent event) {
				drop.dragOperationChanged(event);
			}
			public void dragOver(DropTargetEvent event) {
				drop.dragOver(event);
			}
			public void dropAccept(DropTargetEvent event) {
				drop.dropAccept(event);
			}
			public void drop(DropTargetEvent event) {
				TreeItem item = viewer.getTree().getItem(viewer.getTree().toControl(new Point(event.x, event.y)));
				drop.drop(event, item, mc);
			}
		});
	}

	@Override
	public void setFocus() {
	}
	
	private static class ContentProvider implements IStructuredContentProvider, ITreeContentProvider {
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		public Object[] getElements(Object inputElement) {
			return ((Folder)inputElement).getChildren().toArray();
		}
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Folder)
				return ((Folder)parentElement).getChildren().toArray();
			return new Object[] {};
		}
		public Object getParent(Object element) {
			if (element instanceof Item)
				return ((Item)element).getParent();
			return null;
		}
		public boolean hasChildren(Object element) {
			if (element instanceof Media) return false;
			if (element instanceof Folder)
				return !((Folder)element).getChildren().isEmpty();
			return false;
		}
		public void dispose() {
		}
	}
	
	private static class LabelProvider implements ILabelProvider {
		public String getText(Object element) {
			if (element instanceof Folder)
				return ((Folder)element).getName();
			if (element instanceof Media)
				return ((Media)element).getName();
			return "Unknown type";
		}
		public Image getImage(Object element) {
			if (element instanceof Folder)
				return SharedImages.getImage(SharedImages.icons.x16.file.FOLDER_OPEN);
			if (element instanceof Media) {
				Media media = (Media)element;
				switch (media.getType()) {
				case Media.TYPE_IMAGE: return null;//return SharedImages.getImage(SharedImages.icons.x16.filetypes.PICTURE);
				case Media.TYPE_MOVIE: return SharedImages.getImage(SharedImages.icons.x16.filetypes.MOVIE);
				case Media.TYPE_MUSIC: return SharedImages.getImage(SharedImages.icons.x16.filetypes.AUDIO);
				case Media.TYPE_PLAYLIST: return SharedImages.getImage(SharedImages.icons.x16.filetypes.AUDIO);
				}
			}
			return null;
		}
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		public void addListener(ILabelProviderListener listener) {
		}
		public void removeListener(ILabelProviderListener listener) {
		}
		public void dispose() {
		}
	}

	private List<Item> getSelection() {
		IStructuredSelection sel = (IStructuredSelection)viewer.getSelection();
		if (sel == null || sel.isEmpty()) return null;
		List<Item> list = new LinkedList<Item>();
		for (Iterator<?> it = sel.iterator(); it.hasNext(); )
			list.add((Item)it.next());
		return list;
	}
	
	private Folder getParentFolderFromSelection() {
		List<Item> sel = getSelection();
		Folder parent;
		if (sel == null || sel.size() > 1)
			parent = mc.getRoot();
		else {
			Item i = sel.get(0);
			if (i instanceof Folder)
				parent = (Folder)i;
			else
				parent = i.getParent();
		}
		return parent;
	}
	
	private class NewFolder implements Listener<MouseEvent> {
		public void fire(MouseEvent event) {
			InputDialog dlg = new InputDialog(parent.getShell(), Local.Create_new_folder.toString(), Local.Enter_the_name_for_the_new_folder.toString(), "", new IInputValidator() {
				public String isValid(String newText) {
					if (newText.length() == 0) return Local.The_name_cannot_be_empty.toString();
					if (getParentFolderFromSelection().getSubFolder(newText) != null)
						return Local.This_folder_already_exists.toString();
					return null;
				}
			});
			if (dlg.open() == Window.OK) {
				getParentFolderFromSelection().newSubFolder(dlg.getValue());
			}
		}
	}
	private class Remove implements Listener<MouseEvent> {
		public void fire(MouseEvent event) {
			List<Item> sel = getSelection();
			if (sel == null) return;
			String text;
			if (sel.size() == 1)
				text = sel.get(0).getName();
			else
				text = Local.the_les+" "+sel.size()+" "+Local.selected_items;
			if (!MessageDialog.openConfirm(parent.getShell(), Local.Remove.toString(), Local.process(Local.MSG_remove_confirmation, text))) 
				return;
			for (Item i : sel)
				i.remove();
		}
	}
}
