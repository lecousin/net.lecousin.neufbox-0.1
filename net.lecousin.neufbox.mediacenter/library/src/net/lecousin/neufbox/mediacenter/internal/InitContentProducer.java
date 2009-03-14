package net.lecousin.neufbox.mediacenter.internal;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.entity.ContentProducer;

public class InitContentProducer implements ContentProducer {

	public void writeTo(OutputStream out) throws IOException {
		StringBuilder str = new StringBuilder();
		str.append("<html xmlns='http://www.w3.org/1999/xhtml'>\r\n")
			.append("<head>\r\n")
				.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />")
				.append("<meta hostname=\"DataOrganizer\" />")
				.append("<meta os=\"linux\" />")
				.append("<meta version=\"1.00\" />")
				.append("<meta interface=\"http://172.16.255.254:26180/interface/prototype\" name=\"prototype\" />")
				.append("<Title>MediaCenter - Neuf TV HD</title>")
			.append("</head>")
			.append("<body>")
			.append("</body>")
			.append("</html>")
			;
		out.write(str.toString().getBytes());
	}
	
}
