package net.lecousin.neufbox.mediacenter;

import net.lecousin.framework.strings.StringUtil;

public class Coder {

	public static String encode(String s) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < s.length(); ++i) {
			char c  = s.charAt(i);
			if (c == '/' || c == '&' || c == '%' || c >= 128)
				str.append('%').append(StringUtil.toStringHex(c, 2));
			else
				str.append(c);
		}
		return str.toString();
	}
	
	public static String encodeURL(String s) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < s.length(); ++i) {
			char c  = s.charAt(i);
			if (c == '/' || c == '&' || c == '%' || c == ' ' || c >= 128)
				str.append('%').append(StringUtil.toStringHex(c, 2));
			else
				str.append(c);
		}
		return str.toString();
	}
	
	public static String decode(String s) {
		StringBuilder str = new StringBuilder();
		int i = 0;
		int j;
		while ((j = s.indexOf('%', i)) >= 0) {
			int a = StringUtil.decodeHexa(s.charAt(j+1));
			int b = StringUtil.decodeHexa(s.charAt(j+2));
			a = a*16+b;
			str.append((char)a);
			i = j+3;
		}
		if (i < str.length()-1)
			str.append(s.substring(i));
		return str.toString();
	}
}
