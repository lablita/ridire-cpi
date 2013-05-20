/*******************************************************************************
 * Copyright 2013 Università degli Studi di Firenze
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/**
 * 
 */
package it.drwolf.ridire.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Marco
 * 
 */
public class MD5DigestCreator {

	public static String getMD5Digest(File f) throws NoSuchAlgorithmException,
			IOException {
		StringBuffer returnDigest = new StringBuffer();
		MessageDigest md = MessageDigest.getInstance("MD5");
		InputStream fis = new FileInputStream(f);
		byte[] buf = new byte[(int) f.length()];
		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < buf.length
				&& (numRead = fis.read(buf, offset, buf.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < buf.length) {
			throw new IOException("Could not completely read file "
					+ f.getName());
		}
		// Close the input stream and return bytes
		fis.close();
		md.update(buf);
		byte[] digest = md.digest();
		for (byte element : digest) {
			if ((element & 0xFF) < 16) {
				returnDigest.append("0");
			}
			returnDigest.append(Integer.toHexString(element & 0xFF));
		}
		return returnDigest.toString();
	}

	public static String getMD5Digest(InputStream is)
			throws NoSuchAlgorithmException, IOException {
		StringBuffer returnDigest = new StringBuffer();
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] buffer = new byte[20000];
		int numRead;
		do {
			numRead = is.read(buffer);
			if (numRead > 0) {
				md.update(buffer, 0, numRead);
			}
		} while (numRead != -1);
		is.close();
		byte[] digest = md.digest();
		for (byte element : digest) {
			if ((element & 0xFF) < 16) {
				returnDigest.append("0");
			}
			returnDigest.append(Integer.toHexString(element & 0xFF));
		}
		return returnDigest.toString();
	}

	@Deprecated
	public static String getMD5Digest(String plaintext)
			throws NoSuchAlgorithmException {
		StringBuffer returnDigest = new StringBuffer();
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(plaintext.getBytes());
		byte[] digest = md.digest();
		for (byte element : digest) {
			if ((element & 0xFF) < 16) {
				returnDigest.append("0");
			}
			returnDigest.append(Integer.toHexString(element & 0xFF));
		}
		return returnDigest.toString();
	}
}
