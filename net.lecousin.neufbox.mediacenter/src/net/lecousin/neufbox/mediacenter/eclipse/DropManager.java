package net.lecousin.neufbox.mediacenter.eclipse;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.lecousin.framework.eclipse.extension.EclipsePluginExtensionUtil;
import net.lecousin.framework.files.FileType;
import net.lecousin.framework.files.TypedFile;
import net.lecousin.framework.files.TypedFileDetector;
import net.lecousin.framework.files.audio.AudioFile;
import net.lecousin.framework.files.image.ImageFile;
import net.lecousin.framework.files.video.VideoFile;
import net.lecousin.framework.log.Log;
import net.lecousin.neufbox.mediacenter.Folder;
import net.lecousin.neufbox.mediacenter.Item;
import net.lecousin.neufbox.mediacenter.Media;
import net.lecousin.neufbox.mediacenter.MediaCenter;
import net.lecousin.neufbox.mediacenter.eclipse.internal.EclipsePlugin;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.widgets.TreeItem;

public class DropManager {
	
	public static interface Plugin {
		public Collection<Transfer> getTransfers();
		public TransferData getTransfer(DropTargetEvent event);
		public boolean drop(DropTargetEvent event, TreeItem item, MediaCenter mc, DropManager manager);
	}
	
	public DropManager() {
		for (IConfigurationElement ext : EclipsePluginExtensionUtil.getExtensionsSubNode(EclipsePlugin.ID, "tree", "drop")) {
			try {
				Plugin pi = EclipsePluginExtensionUtil.createInstance(Plugin.class, ext, "class", new Object[][] { new Object[] { } });
				plugins.add(pi);
			} catch (Throwable t) {
				if (Log.error(this))
					Log.error(this, "Unable to instantiate Drop plugin", t);
			}
		}
	}
	
	private List<Plugin> plugins = new LinkedList<Plugin>();

	protected Transfer[] getTransfers() {
		Set<Transfer> list = new HashSet<Transfer>();
		list.add(FileTransfer.getInstance());
		list.add(URLTransfer.getInstance());
		for (Plugin pi : plugins)
			list.addAll(pi.getTransfers());
		return list.toArray(new Transfer[list.size()]);
	}
	protected void dragEnter(DropTargetEvent event) {
		TransferData support = null;
		for (Plugin pi : plugins)
			if ((support = pi.getTransfer(event)) != null)
				break;
		if (support == null && FileTransfer.getInstance().isSupportedType(event.currentDataType))
			support = event.currentDataType;
		if (support == null) {
			for (TransferData d : event.dataTypes)
				if (FileTransfer.getInstance().isSupportedType(d)) {
					support = d;
					break;
				}
			if (URLTransfer.getInstance().isSupportedType(event.currentDataType))
				support = event.currentDataType;
			else {
				for (TransferData d : event.dataTypes)
					if (URLTransfer.getInstance().isSupportedType(d)) {
						support = d;
						break;
					}
			}
		}
		if (support != null)
			event.currentDataType = support;
		event.detail = support != null ? DND.DROP_LINK : DND.DROP_NONE;
	}
	protected void dragLeave(DropTargetEvent event) {
	}
	protected void dragOperationChanged(DropTargetEvent event) {
		dragEnter(event);
	}
	protected void dragOver(DropTargetEvent event) {
	}
	protected void dropAccept(DropTargetEvent event) {
	}
	protected void drop(DropTargetEvent event, TreeItem item, MediaCenter mc) {
		for (Plugin pi : plugins)
			if (pi.drop(event, item, mc, this))
				return;
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
			String[] paths = (String[])event.data;
			for (String path : paths)
				addFile(mc, item, new File(path));
		} else if (URLTransfer.getInstance().isSupportedType(event.currentDataType)) {
			URI uri;
			try { uri = ((URL)event.data).toURI(); }
			catch (URISyntaxException e) { return; }
			File file = new File(uri);
			addFile(mc, item, file);
		}
	}
	
	private Folder getParent(MediaCenter mc, TreeItem item) {
		if (item == null)
			return mc.getRoot();
		Item i = (Item)item.getData();
		if (i instanceof Folder)
			return (Folder)i;
		return ((Media)i).getParent();
	}
	
	public Folder addFolder(MediaCenter mc, TreeItem item, String name) {
		return getParent(mc, item).newSubFolder(name);
	}
	
	public void addFile(MediaCenter mc, TreeItem item, File file) {
		addFile(getParent(mc, item), file);
	}
	
	public void addFile(Folder folder, File file) {
		List<FileType> types = new LinkedList<FileType>();
		types.add(AudioFile.FILE_TYPE);
		types.add(VideoFile.FILE_TYPE);
		types.add(ImageFile.FILE_TYPE);
		TypedFile filetype;
		try { filetype = TypedFileDetector.detect(file, types); }
		catch (Throwable t) { 
			if (Log.warning(this))
				Log.warning(this, "Unable to determine file type: " + file.getAbsolutePath(), t);
			return;
		}
		int type;
		if (filetype instanceof AudioFile)
			type = Media.TYPE_MUSIC;
		else if (filetype instanceof VideoFile)
			type = Media.TYPE_MOVIE;
		else if (filetype instanceof ImageFile)
			type = Media.TYPE_IMAGE;
		else return;
		folder.newMedia(file.getName(), type, file, null);
	}
	
}
