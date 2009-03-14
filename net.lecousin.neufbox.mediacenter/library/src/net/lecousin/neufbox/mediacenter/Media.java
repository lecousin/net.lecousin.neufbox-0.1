package net.lecousin.neufbox.mediacenter;

import java.io.File;

public class Media implements Item {

	public static final int TYPE_MOVIE = 1;
	public static final int TYPE_MUSIC = 2;
	public static final int TYPE_IMAGE = 4;
	public static final int TYPE_PLAYLIST = 128;
	
	Media(Folder parent, String name, int type, File file, Object data) {
		this.parent = parent;
		this.name = name;
		this.type = type;
		this.file = file;
		this.data = data;
	}
	
	private Folder parent;
	private String name;
	private int type;
	private File file;
	private Object data;
	
	public Folder getParent() { return parent; }
	public String getName() { return name; }
	public int getType() { return type; }
	public File getFile() { return file; }
	public Object getData() { return data; }
	public String getPath() { 
		String s = parent.getPath();
		if (s.length() > 0) s += '/';
		return s + name;
	}
	public MediaCenter getMediaCenter() {
		return getParent().getMediaCenter();
	}
	
	public void remove() {
		parent.removeMedia(this);
	}
}
