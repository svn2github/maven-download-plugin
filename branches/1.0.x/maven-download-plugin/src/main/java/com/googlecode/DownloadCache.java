/**
 * Copyright 2012, Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

/**
 * A class representing a download cache
 * @author Mickael Istria (Red Hat Inc)
 *
 */
public class DownloadCache {

	private static final String INDEX_FILENAME = "index.ser";
	
	private File basedir;
	private File indexFile;
	private Map<String, CachedFileEntry> index;

	public DownloadCache(File cacheDirectory) {
		this.basedir = cacheDirectory;
		this.indexFile = new File(this.basedir, INDEX_FILENAME);
	}

	private CachedFileEntry getEntry(String url, String md5, String sha1) throws Exception {
		loadIndex();
		CachedFileEntry res = this.index.get(url);
		if (res == null) {
			return null;
		}
		if (md5 != null && !md5.equals(res.md5)) {
			return null;
		}
		if (sha1 != null && !sha1.equals(res.sha1)) {
			return null;
		}
		return res;
	}
	
	/**
	 * Get a File in the download cache. If no cache for this URL, or
	 * if expected signatures don't match cached ones, returns null.
	 * available in cache, 
	 * @param url URL of the file
	 * @param md5 MD5 signature to verify file. Can be null => No check
	 * @param sha1 Sha1 signature to verify file. Can be null => No check
	 * @return A File when cache is found, null if no available cache
	 */
	public File getArtifact(String url, String md5, String sha1) throws Exception {
		CachedFileEntry res = getEntry(url, md5, sha1);
		if (res != null) {
			return new File(this.basedir, res.fileName);
		}
		return null;
	}

	public void install(String url, File outputFile, String md5, String sha1) throws Exception {
		loadIndex();
		if (md5 == null) {
			md5 = SignatureUtils.computeSignatureAsString(outputFile, MessageDigest.getInstance("MD5"));
		}
		if (sha1 == null) {
			sha1 = SignatureUtils.computeSignatureAsString(outputFile, MessageDigest.getInstance("SHA1"));
		}
		CachedFileEntry entry = getEntry(url, md5, sha1);
		if (entry != null) {
			return; // entry already here
		}
		entry = new CachedFileEntry();
		entry.fileName = outputFile.getName();
		entry.md5 = md5;
		entry.sha1 = sha1;
		this.index.put(url, entry);
		FileUtils.copyFile(outputFile, new File(this.basedir, entry.fileName));
		saveIndex();
	}
	
	

	private void loadIndex() throws Exception {
		if (this.indexFile.isFile()) {
			FileInputStream input = new FileInputStream(this.indexFile);
			ObjectInputStream deserialize = new ObjectInputStream(input);
			this.index = (Map<String, CachedFileEntry>)deserialize.readObject();
			deserialize.close();
		} else {
			this.index = new HashMap<String, CachedFileEntry>();
		}
		
	}
	
	private void saveIndex() throws Exception {
		if (this.basedir.exists() && !this.basedir.isDirectory()) {
			throw new Exception("Cannot use " + this.basedir + " as cache directory: file exists");
		}
		if (!this.basedir.exists()) {
			this.basedir.mkdirs();
		}
		if (!this.indexFile.exists()) {
			this.indexFile.createNewFile();
		}
		FileOutputStream out = new FileOutputStream(this.indexFile);
		ObjectOutputStream res = new ObjectOutputStream(out);
		res.writeObject(index);
		res.close();
	}

}
