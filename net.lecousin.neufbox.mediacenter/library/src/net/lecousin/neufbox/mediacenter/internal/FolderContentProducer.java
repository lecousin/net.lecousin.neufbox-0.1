package net.lecousin.neufbox.mediacenter.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

import net.lecousin.neufbox.mediacenter.Coder;
import net.lecousin.neufbox.mediacenter.Folder;
import net.lecousin.neufbox.mediacenter.Media;

import org.apache.http.entity.ContentProducer;

public class FolderContentProducer implements ContentProducer {

	public FolderContentProducer(Folder folder, String myself) {
		this.folder = folder;
		this.myself = myself;
	}
	
	private Folder folder;
	private String myself;
	
	public void writeTo(OutputStream out) throws IOException {
		StringBuilder str = new StringBuilder();
		str.append("<browse>\r\n");
		if (folder.getParent() != null) {
			str.append("<parentPath>");
			String s = folder.getParent().getPath();
			str.append(s.length() == 0 ? "." : Coder.encode(s));
			str.append("</parentPath>\r\n");
		} else {
			str.append("<childDevices>\r\n");
            str.append("</childDevices>\r\n");
		}
		str.append("<folders>\r\n");
		for (Folder f : folder.getSubFolders()) {
			str.append("<folder>\r\n");
			str.append("<name>").append(f.getName()).append("</name>\r\n");
			str.append("<path>").append(URLEncoder.encode(f.getPath())).append("</path>");
			str.append("</folder>\r\n");
		}
		str.append("</folders>\r\n");
		str.append("<medias>\r\n");
		for (Media m : folder.getMedias()) {
			str.append("<media type=\"").append(m.getType()).append("\">\r\n");
			str.append("<name>").append(m.getName()).append("</name>\r\n");
			str.append("<url type=\"http\">http://").append(myself).append("/__mp9ctl_share_2/").append(URLEncoder.encode(m.getPath())).append("</url>");
			str.append("</media>\r\n");
		}
		str.append("</medias>\r\n");
		str.append("</browse>\r\n");

    	//ByteBuffer buffer = Charset.forName("UTF-8").encode(str.toString());
    	//String s = new String(buffer.array(), buffer.position(), buffer.limit());
		String s = str.toString();
		out.write(("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + s).getBytes());
	}
	
}
