package net.lecousin.neufbox.mediacenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.Pair;
import net.lecousin.framework.event.Event;
import net.lecousin.framework.log.Log;
import net.lecousin.framework.net.http.HttpURI;
import net.lecousin.framework.net.http.server.HttpServer;
import net.lecousin.framework.net.http.server.HttpServerUtil;
import net.lecousin.neufbox.mediacenter.internal.FolderContentProducer;
import net.lecousin.neufbox.mediacenter.internal.InitContentProducer;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerResolver;

public class MediaCenter {

	public MediaCenter() throws IOException {
		handlerResolver = new HandlerResolver();
		handler = new Handler();
		server = HttpServer.open(26180, handlerResolver);
	}
	
	public void close() {
		server.close();
	}
	
	private HttpServer server;
	private HttpRequestHandlerResolver handlerResolver;
	private HttpRequestHandler handler;
	private Folder root = new Folder(this);
	private Streaming currentStreaming = null;
	private Event<Item> itemAdded = new Event<Item>();
	private Event<Item> itemRemoved = new Event<Item>();
	private Event<Media> mediaRead = new Event<Media>();
	
	public Event<Item> itemAdded() { return itemAdded; }
	public Event<Item> itemRemoved() { return itemRemoved; }
	public Event<Media> mediaRead() { return mediaRead; }
	
	public Folder getRoot() { return root; }
	
	private class HandlerResolver implements HttpRequestHandlerResolver {
		public HttpRequestHandler lookup(String uri) {
			if (uri.startsWith("/mp9ctl/") || uri.startsWith("/__mp9ctl_share_2/"))
				return handler;
			return HttpServerUtil.getErrorRequestHandler(HttpStatus.SC_NOT_FOUND, "Invalid request");
		}
	}
	
	private class Handler implements HttpRequestHandler {
		public void handle(HttpRequest req, HttpResponse resp, HttpContext ctx)
		throws HttpException, IOException {
			HttpURI uri = new HttpURI(req.getRequestLine().getUri());
			if (uri.getPath().startsWith("/__mp9ctl_share_2/"))
				handleDownload(uri, req, resp, ctx);
			else {
				if (currentStreaming != null) {
					currentStreaming.free();
					currentStreaming = null;
				}
				if (uri.getPath().equals("/mp9ctl/")) {
					String cmd = uri.getQuery().get("cmd");
					if (cmd == null)
						handleInitRequest(req, resp, ctx);
					else if (cmd.equals("toplist"))
						handleTopList(req, resp, ctx);
					else if (cmd.equals("browse"))
						handleBrowse(uri, req, resp, ctx);
					else
						HttpServerUtil.handleError(req, resp, HttpStatus.SC_NOT_FOUND, "Invalid command: " + cmd);
				} else 
					HttpServerUtil.handleError(req, resp, HttpStatus.SC_NOT_FOUND, "Invalid request");
			}
		}
	}
	
	private void handleInitRequest(HttpRequest req, HttpResponse resp, HttpContext ctx)
	throws HttpException, IOException {
		resp.setStatusCode(HttpStatus.SC_OK);
		resp.setEntity(new EntityTemplate(new InitContentProducer()));
	}
	
	private void handleTopList(HttpRequest req, HttpResponse resp, HttpContext ctx)
	throws HttpException, IOException {
		Header[] headers = req.getHeaders("Host");
		String myself = headers.length > 0 ? headers[0].getValue() : null;
		resp.setStatusCode(HttpStatus.SC_OK);
		resp.setEntity(new EntityTemplate(new FolderContentProducer(root, myself)));
	}
	
	private void handleBrowse(HttpURI uri, HttpRequest req, HttpResponse resp, HttpContext ctx)
	throws HttpException, IOException {
		String loc = uri.getQuery().get("location");
		Folder f = root.getFolderFromPath(URLDecoder.decode(loc));
		if (f == null)
			HttpServerUtil.handleError(req, resp, HttpStatus.SC_NOT_FOUND, "folder '" + loc + "' doesn't exist");
		else {
			Header[] headers = req.getHeaders("Host");
			String myself = headers.length > 0 ? headers[0].getValue() : null;
			resp.setStatusCode(HttpStatus.SC_OK);
			resp.setEntity(new EntityTemplate(new FolderContentProducer(f, myself)));
		}
	}
	
	private void handleDownload(HttpURI uri, HttpRequest req, HttpResponse resp, HttpContext ctx)
	throws HttpException, IOException {
		String loc = uri.getPath().substring(18);
		Media m = root.getMediaFromPath(URLDecoder.decode(loc));
		if (m == null) {
			if (currentStreaming != null) {
				currentStreaming.free();
				currentStreaming = null;
			}
			HttpServerUtil.handleError(req, resp, HttpStatus.SC_NOT_FOUND, "media '" + loc + "' doesn't exist");
			return;
		}
		resp.setStatusCode(HttpStatus.SC_OK);
		File file = m.getFile();
		List<Pair<Long,Long>> ranges = new LinkedList<Pair<Long,Long>>();
		for (Header h : req.getHeaders("Range")) {
			String s = h.getValue();
			if (!s.startsWith("bytes=")) { if (Log.debug(this)) Log.debug(this, "Invalid range: " + s); continue; }
			s = s.substring(6);
			int i = s.indexOf('-');
			if (i < 0) { if (Log.debug(this)) Log.debug(this, "Invalid range: " + s); continue; }
			long start, end;
			if (i == 0)
				start = 0;
			else
				try { start = Long.parseLong(s.substring(0,i)); }
				catch (NumberFormatException e) { if (Log.debug(this)) Log.debug(this, "Invalid range start: " + s, e); continue; }
			if (i == s.length()-1)
				end = file.length();
			else
				try { end = Long.parseLong(s.substring(i+1)); }
				catch (NumberFormatException e) { if (Log.debug(this)) Log.debug(this, "Invalid range end: " + s, e); continue; }
			if (Log.debug(this)) Log.debug("Range=" + start + "-" + end + " (" + s + ")");
			ranges.add(new Pair<Long,Long>(start,end));
		}
		if (currentStreaming != null) {
			if (!currentStreaming.file.equals(file))
				currentStreaming = null;
		}
		if (currentStreaming == null) {
			currentStreaming = new Streaming(file);
			mediaRead.fire(m);
		}
		long length = file.length();
		long start = 0;
		if (!ranges.isEmpty()) {
			long startR = ranges.get(0).getValue1();
			long endR = ranges.get(0).getValue2();
			length = endR;
			if (startR > 0) {
				start = startR;
				length -= startR;
			}
		}
		currentStreaming.setPosition(start);
		AbstractHttpEntity entity = new InputStreamEntity(currentStreaming, length);
		entity.setContentType("video/x-msvideo");
		resp.setEntity(entity);
		resp.addHeader("Accept-Ranges", "bytes");
	}
	
	private static class Streaming extends InputStream {
		public Streaming(File file) throws IOException {
			this.file = file;
			stream = new FileInputStream(file);
			startLen = stream.read(start);
		}
		private File file;
		private FileInputStream stream;
		private long pos = 0;
		private static final int START_SIZE = 65536;
		private byte[] start = new byte[START_SIZE];
		private int startLen = 0;
		
		public void setPosition(long position) {
			if (position > startLen) {
				if (pos < startLen) pos = startLen;
				try {
					if (position > pos) {
						needStream();
						stream.skip(position-pos);
					} else {
						reinit(position);
					}
				} catch (IOException e) {
					if (Log.error(this))
						Log.error(this, "Read error", e);
				}
			} else {
				if (pos > startLen) {
					try { stream.close(); } catch (IOException e) {}
					stream = null;
				}
			}
			pos = position;
		}
		public void free() {
			try { stream.close(); }
			catch (IOException e) {}
		}
		
		private void reinit(long position) throws IOException {
			if (Log.debug(this))
				Log.debug("reinit stream");
			stream.close();
			stream = new FileInputStream(file);
			stream.skip(position);
		}
		private void needStream() {
			if (stream == null) {
				try { 
					stream = new FileInputStream(file);
					stream.skip(startLen);
				} 
				catch (IOException e) {
					if (Log.error(this))
						Log.error(this, "Read error", e);
				}
			}
		}
		
		@Override
		public int read() throws IOException {
			if (pos < startLen)
				return (int)start[(int)pos++];
			needStream();
			int result = stream.read();
			if (result == -1) return -1;
			pos++;
			return result;
		}
		@Override
		public int read(byte[] b) throws IOException {
			if (pos < startLen) {
				int nb = (int)(startLen - pos);
				if (nb > b.length) nb = b.length;
				System.arraycopy(start, (int)pos, b, 0, nb);
				pos += nb;
				return nb;
			}
			needStream();
			int nb = stream.read(b);
			pos += nb;
			return nb;
		}
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (pos < startLen) {
				int nb = (int)(startLen - pos);
				if (nb > len) nb = len;
				System.arraycopy(start, (int)pos, b, off, nb);
				pos += nb;
				return nb;
			}
			needStream();
			int nb = stream.read(b, off, len);
			pos += nb;
			return nb;
		}
	}
}
