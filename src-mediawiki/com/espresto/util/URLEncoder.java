package com.espresto.util;


import java.io.UnsupportedEncodingException;

/**
 * small wrapper to ignore the UnsupportedEncodingException when URL-encoding in UTF-8,
 * and also to make it usable for URL-Path segments, not only parameter
 */
public class URLEncoder {

    public static String encode(String str) {
	try {
	    return java.net.URLEncoder.encode(str, "UTF-8").replaceAll("\\+", "%20");
	} catch (UnsupportedEncodingException e) {
	    throw new RuntimeException("UTF-8 not found: this should not happen");
	}
    }

    public static String decode(String str) {
    try {
        return java.net.URLDecoder.decode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("UTF-8 not found: this should not happen");
    }
    }

}
