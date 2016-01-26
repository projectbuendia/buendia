/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync.serialization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.sync.SyncConstants;

/**
 * 
 */
public class ZipPackage {
	private static Log log = LogFactory.getLog(FilePackage.class);
	static final int BUFFER = 2048;
	private String rootName;
	private String targetName;
	private FileOutputStream dest;
	private CheckedOutputStream checksum;
	private ZipOutputStream out;
	private byte data[];

	public ZipPackage(File root, String target) {
		try {
			this.rootName = root.getAbsolutePath();
			this.targetName = target;
			File archiveFolder = new File(root, "archive");
			archiveFolder.mkdir();
			File f = new File(archiveFolder, targetName);
			f.mkdir();
			if (!f.exists())
				f.mkdirs();
			File outputStreamDir = new File(f, targetName + "_"
			        + SyncConstants.SYNC_FILENAME_MASK.format(new Date()) + ".zip");
			this.dest = new FileOutputStream(outputStreamDir);
			this.checksum = new CheckedOutputStream(dest, new Adler32());
			this.out = new ZipOutputStream(new BufferedOutputStream(checksum));
			this.data = new byte[BUFFER];
		} catch (Exception e) {
			log.error(e.toString());
		}
	}

	public boolean zip(boolean clearDir) {
		if (this.addFile(this.targetName)) {
			try {
				this.out.close();
				if (clearDir) {
					File dir = new File(this.rootName + "archive/" + this.targetName);
					this.clearPath(dir);
				}
			} catch (Exception e) {
				log.error(e.toString());
				return false;
			}
			log.info("ZIP FILE: " + this.rootName + "archive/" + this.targetName
			        + ".zip archived successifully");
			return true;
		} else {
			try {
				this.out.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			log.error("ERROR while archiving folder " + this.rootName + "archive/"
			        + this.targetName);
			return false;
		}
	}

	public void clearPath(File path) throws IOException {
		File[] files = path.listFiles();

		for (int i = 0; i < files.length; ++i) {
			if (files[i].isDirectory()) {
				clearPath(files[i]);
			}
			files[i].delete();
		}
	}

	public boolean addFile(String rootRelativeName) {
		try {

			File currentFile = new File(rootName + "archive/" + rootRelativeName);

			if (currentFile.isFile()) {
				BufferedInputStream origin = new BufferedInputStream(new FileInputStream(currentFile),
				                                                     BUFFER);
				ZipEntry entry = new ZipEntry(rootRelativeName);
				this.out.putNextEntry(entry);
				int count;
				while ((count = origin.read(this.data, 0, BUFFER)) != -1) {
					this.out.write(this.data, 0, count);
				}
				origin.close();
				return true;
			} else if (currentFile.isDirectory()) {
				File[] fileList = currentFile.listFiles();
				for (int i = 0; i < fileList.length; i++) {
					if (!addFile(rootRelativeName + "/" + fileList[i].getName())) {
						return false;
					}
				}
				return true;
			}
		} catch (Exception e) {
			log.error(e.toString());
			return false;
		}
		return false;
	}
}