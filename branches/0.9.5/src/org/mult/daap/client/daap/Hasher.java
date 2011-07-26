/*
 * Created on Mar 1, 2004
 * Algorithm:
 * Copyright (c) 2004 David Hammerton
 * 
 * port to java:
 * Copyright (c) 2004 Joseph Barnett
 * 
 * updated for GIT:
 * Greg Jordan, August 2004
 */
package org.mult.daap.client.daap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mult.daap.client.daap.request.Request;

/** @author jbarnett */
public class Hasher {
	// taken from calculations in libopendaap
	// private static String staticHash = "98814088228B81E0AB021433618EC27B";
	// private static String calcHash = "";
	// private static boolean staticCalculated = false;
	private static final String hexchars = "0123456789ABCDEF";
	// private static final String appleCopyright =
	// "Copyright 2003 Apple Computer, Inc.";
	private static MessageDigest md;
	// private static MD5 nmd;
	// private static double hashVersion = -1;
	// private static final int HASH_SIZE = 32;
	static {
		try {
			md = MessageDigest.getInstance("md5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private static String DigestToString(byte[] digest) {
		String string = "";
		// int length = digest.length;
		for (byte tmp : digest) {
			string += hexchars.charAt((tmp >> 4) & 0x0f);
			string += hexchars.charAt(tmp & 0x0f);
		}
		// for (int i = 0; i < length; i++)
		// {
		// byte tmp = digest[i];
		// string += hexchars.charAt((tmp >> 4) & 0x0f);
		// string += hexchars.charAt(tmp & 0x0f);
		// }
		return string;
	}

	// private static synchronized void calculate45StaticHash() {
	// nmd = new MD5();
	// for (int i = 0; i < 256; i++) {
	// if ((i & 0x40) != 0)
	// nmd.Update("eqwsdxcqwesdc".getBytes());
	// else
	// nmd.Update("op[;lm,piojkmn".getBytes());
	//
	// if ((i & 0x20) != 0)
	// nmd.Update("876trfvb 34rtgbvc".getBytes());
	// else
	// nmd.Update("=-0ol.,m3ewrdfv".getBytes());
	//
	// if ((i & 0x10) != 0)
	// nmd.Update("87654323e4rgbv ".getBytes());
	// else
	// nmd.Update("1535753690868867974342659792".getBytes());
	//
	// if ((i & 0x08) != 0)
	// nmd.Update("Song Name".getBytes());
	// else
	// nmd.Update("DAAP-CLIENT-ID:".getBytes());
	//
	// if ((i & 0x04) != 0)
	// nmd.Update("111222333444555".getBytes());
	// else
	// nmd.Update("4089961010".getBytes());
	//
	// if ((i & 0x02) != 0)
	// nmd.Update("playlist-item-spec".getBytes());
	// else
	// nmd.Update("revision-number".getBytes());
	//
	// if ((i & 0x01) != 0)
	// nmd.Update("session-id".getBytes());
	// else
	// nmd.Update("content-codes".getBytes());
	//
	// if ((i & 0x80) != 0)
	// nmd.Update("IUYHGFDCXWEDFGHN".getBytes());
	// else
	// nmd.Update("iuytgfdxwerfghjm".getBytes());
	//
	// // calcHash = DigestToString(nmd.Final());
	// // calcHash = DigestToString(nmd.Final(), calcHash);
	//
	// for (byte tmp : nmd.Final()) {
	// calcHash += hexchars.charAt((tmp >> 4) & 0x0f);
	// calcHash += hexchars.charAt(tmp & 0x0f);
	// }
	// nmd.Init();
	// }
	// staticCalculated = true;
	//
	// }
	// private static synchronized void calculate40StaticHash() {
	//
	// for (int i = 0; i < 256; i++) {
	// if ((i & 0x80) != 0)
	// md.update("Accept-Language".getBytes());
	// else
	// md.update("user-agent".getBytes());
	//
	// if ((i & 0x40) != 0)
	// md.update("max-age".getBytes());
	// else
	// md.update("Authorization".getBytes());
	//
	// if ((i & 0x20) != 0)
	// md.update("Client-DAAP-Version".getBytes());
	// else
	// md.update("Accept-Encoding".getBytes());
	//
	// if ((i & 0x10) != 0)
	// md.update("daap.protocolversion".getBytes());
	// else
	// md.update("daap.songartist".getBytes());
	//
	// if ((i & 0x08) != 0)
	// md.update("daap.songcomposer".getBytes());
	// else
	// md.update("daap.songdatemodified".getBytes());
	//
	// if ((i & 0x04) != 0)
	// md.update("daap.songdiscnumber".getBytes());
	// else
	// md.update("daap.songdisabled".getBytes());
	//
	// if ((i & 0x02) != 0)
	// md.update("playlist-item-spec".getBytes());
	// else
	// md.update("revision-number".getBytes());
	//
	// if ((i & 0x01) != 0)
	// md.update("session-id".getBytes());
	// else
	// md.update("content-codes".getBytes());
	//
	// // String newHash = DigestToString(md.digest());
	// // calcHash += newHash;
	// // calcHash = calcHash + DigestToString(md.digest());
	//
	// // int length = digest.length;
	// for (byte tmp : md.digest()) {
	// calcHash += hexchars.charAt((tmp >> 4) & 0x0f);
	// calcHash += hexchars.charAt(tmp & 0x0f);
	// }
	// }
	// staticCalculated = true;
	// }
	// private static synchronized void calculateStaticHash(DaapHost host) {
	//
	// if (staticCalculated && (host.getDaapVersion() == hashVersion))
	// return;
	//
	// calcHash = "";
	// // System.out.println("Regenerating static hash");
	//
	// if (host.getDaapVersion() >= DaapHost.ITUNES_45)
	// calculate45StaticHash();
	// else
	// calculate40StaticHash();
	//
	// hashVersion = host.getDaapVersion();
	//
	// }
	public static String GenerateHash(String url, Request request,
			boolean getRequest) {
		md.update(url.getBytes());
		return DigestToString(md.digest());
		// int start = HASH_SIZE * request.getAccessIndex();
		// int end = start + HASH_SIZE;
		// DaapHost host = request.getHost();
		// calculateStaticHash(host);
		// // System.out.println("Creating hash, reqid is " + reqid);
		// if (host.getDaapVersion() >= DaapHost.ITUNES_45) {
		// // System.out.println("Generating 4.5 Hash");
		// nmd = new MD5();
		// nmd.Update(url.getBytes());
		// nmd.Update(appleCopyright.getBytes());
		// nmd.Update(calcHash.substring(start, end).getBytes());
		// if (getRequest) {
		// nmd.Update((new Integer(host.getThisRequestNumber())).toString()
		// .getBytes());
		// // System.out.println("Adding this compontent");
		// }
		// return DigestToString(nmd.Final());
		// } else {
		//
		// md.update(url.getBytes());
		// md.update(appleCopyright.getBytes());
		// md.update(calcHash.substring(start, end).getBytes());
		// return DigestToString(md.digest());
		// }
	}
}