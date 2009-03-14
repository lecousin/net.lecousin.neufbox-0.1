package net.lecousin.neufbox.mediacenter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.event.Event;

public class Folder implements Item {

	Folder(String name, Folder parent) {
		this.name = name;
		this.parent = parent;
	}
	Folder(MediaCenter mc) {
		this.name = "";
		this.mc = mc;
	}
	
	private String name;
	private Folder parent;
	private MediaCenter mc;
	private List<Folder> subFolders = new LinkedList<Folder>();
	private List<Media> medias = new LinkedList<Media>();
	private Event<Item> itemAdded = new Event<Item>();
	private Event<Item> itemRemoved = new Event<Item>();
	
	public Event<Item> itemAdded() { return itemAdded; }
	public Event<Item> itemRemoved() { return itemRemoved; }
	
	public String getName() { return name; }
	public Folder getParent() { return parent; }
	public String getPath() {
		if (parent == null) return "";
		String s = parent.getPath();
		if (s.length() > 0) s += '/';
		return s + name;
	}
	public MediaCenter getMediaCenter() {
		return mc != null ? mc : getParent().getMediaCenter();
	}
	
	public Folder newSubFolder(String name) {
		Folder f = new Folder(name, this);
		subFolders.add(f);
		itemAdded.fire(f);
		getMediaCenter().itemAdded().fire(f);
		return f;
	}
	public Media newMedia(String name, int type, File file, Object data) {
		Media m = new Media(this, name, type, file, data);
		medias.add(m);
		itemAdded.fire(m);
		getMediaCenter().itemAdded().fire(m);
		return m;
	}
	
	public List<Folder> getSubFolders() { return new ArrayList<Folder>(subFolders); }
	public List<Media> getMedias() { return new ArrayList<Media>(medias); }
	public List<Item> getChildren() { ArrayList<Item> result = new ArrayList<Item>(subFolders.size()+medias.size()); result.addAll(subFolders); result.addAll(medias); return result; } 
	
	public Folder getSubFolder(String name) {
		for (Folder f : subFolders)
			if (f.getName().equals(name))
				return f;
		return null;
	}
	public Media getMedia(String name) {
		for (Media m : medias)
			if (m.getName().equals(name))
				return m;
		return null;
	}
	
	public Folder getFolderFromPath(String path) {
		int i = path.indexOf('/');
		if (i < 0) return getSubFolder(path);
		Folder sf = getSubFolder(path.substring(0, i));
		if (sf == null) return null;
		return sf.getFolderFromPath(path.substring(i+1));
	}
	public Media getMediaFromPath(String path) {
		int i = path.indexOf('/');
		if (i < 0) return getMedia(path);
		Folder sf = getSubFolder(path.substring(0, i));
		if (sf == null) return null;
		return sf.getMediaFromPath(path.substring(i+1));
	}
	
	public void remove() {
		if (parent == null) return;
		parent.removeSubFolder(this);
	}
	
	public void removeSubFolder(Folder folder) {
		if (subFolders.remove(folder)) {
			itemRemoved.fire(folder);
			getMediaCenter().itemRemoved().fire(folder);
		}
	}
	public void removeMedia(Media media) {
		if (medias.remove(media)) {
			itemRemoved.fire(media);
			getMediaCenter().itemRemoved().fire(media);
		}
	}
}
