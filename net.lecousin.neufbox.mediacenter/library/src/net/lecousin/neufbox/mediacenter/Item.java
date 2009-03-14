package net.lecousin.neufbox.mediacenter;

public interface Item {

	public String getName();
	public Folder getParent();
	public String getPath();
	public MediaCenter getMediaCenter();
	
	public void remove();
}
