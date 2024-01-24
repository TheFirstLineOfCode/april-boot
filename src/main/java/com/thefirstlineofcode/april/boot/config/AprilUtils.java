package com.thefirstlineofcode.april.boot.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AprilUtils {
	private static final Logger logger = LoggerFactory.getLogger(AprilUtils.class);
	
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static void close(InputStream in) {
		if (in != null) {
			try {
				in.close();
			} catch (Exception e) {
				logger.trace("Failed to close the stream.", e);
			}
		}
	}

	public static void close(OutputStream out) {
		if (out != null) {
			try {
				out.close();
			} catch (Exception e) {
				 logger.trace("Failed to close the stream.", e);
			}
		}
	}

	public static void deleteDirectoryRecursively(File directory) {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Not a directory.");
		} else {
			for (File subFile : directory.listFiles()) {
				if (!subFile.isDirectory())
					subFile.delete();
				else
					deleteDirectoryRecursively(subFile);
			}
		}
	}

	public static void close(Writer writer) {
		if (writer != null) {
			try {
				writer.close();
			} catch (Exception e) {
				logger.warn("Failed to close the writer.", e);
			}
		}
	}

	public static void close(Reader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (Exception e) {
				logger.warn("Failed to close the reader.", e);
			}
		}
	}

	public static void writeToFile(String content, Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createFile(path);
		}
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(path.toFile()));
			out.write(content);
		} finally {
			AprilUtils.close(out);
		}
	}

    public static void writeToFile(InputStream in, Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createFile(path);
		}
		
		BufferedInputStream BufferedIn = null;
    	BufferedOutputStream out = null;
		try {
			BufferedIn = new BufferedInputStream(in);
			out = new BufferedOutputStream(new FileOutputStream(path.toFile()));
			
			byte[] buf = new byte[2048];
			int size = -1;
			while ((size = BufferedIn.read(buf)) != -1) {
				out.write(buf, 0, size);
			}
			
		} finally {
			AprilUtils.close(BufferedIn);
			AprilUtils.close(out);
		}
    }
	
	public static String readFile(Path path) throws IOException {
		BufferedReader in = null;
		
		try {
			in = new BufferedReader(new FileReader(path.toFile()));
			
			char[] buf = new char[1024];
			int size = -1;
			StringBuilder sb = new StringBuilder();
			while ((size = in.read(buf, 0, buf.length)) != -1) {
				sb.append(buf, 0, size);
			}
			
			return sb.toString();
		} catch (IOException e) {
			throw e;
		} finally {
			AprilUtils.close(in);
		}
	}
	
	public static String[] stringToArray(String value) {
		if (value == null || value.isEmpty())
			return new String[0];
		
		StringTokenizer st = new StringTokenizer(value, ",");
		
		String[] array = new String[st.countTokens()];
		
		int i = 0;
		while (st.hasMoreTokens()) {
			array[i++] = st.nextToken().trim();
		}
		
		return array;
	}
	
	public static String arrayToString(String[] array) {
		if (array == null || array.length == 0)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		for (String string : array) {
			sb.append(string).append(',');
		}
		
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	
	public static void zipFolder(File sourceFolder, File targetFile) throws TargetExistsException, IOException {
		if (!sourceFolder.exists()) {
			throw new IllegalArgumentException(String.format("Source folder[%s] to zip doesn't exist."));
		}
		
		if (!sourceFolder.isDirectory()) {
			throw new IllegalArgumentException("Source folder[%s] isn't a folder.");
		}
		
		if (targetFile.exists()) {
			throw new TargetExistsException(String.format("Zip file[%s] has already existed.", targetFile.getPath()));
		}
		
		File[] files = getAllAescendantFiles(sourceFolder);
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
			
			for (File file : files) {
				writeFileToZip(zos, getEntryPath(sourceFolder, file), file);
			}
		} finally {
			AprilUtils.close(zos);
		}
	}
	
	private static File[] getAllAescendantFiles(File sourceFolder) {
		List<File> aescendants = new ArrayList<>();
		
		getAllAescendantFiles(sourceFolder, aescendants);
		
		return aescendants.toArray(new File[aescendants.size()]);
	}

	private static void getAllAescendantFiles(File folder, List<File> aescendants) {
		File[] children = folder.listFiles();
		for (File child : children) {
			if (child.isDirectory()) {
				getAllAescendantFiles(child, aescendants);
			} else {
				aescendants.add(child);
			}
		}
	}

	private static String getEntryPath(File sourceFolder, File file) {
		if (!file.getPath().startsWith(sourceFolder.getPath())) {
			return file.getPath();
		}
		
		String entryPath = file.getPath();
		entryPath = entryPath.substring(sourceFolder.getPath().length(), entryPath.length());
		
		
		if (entryPath.startsWith(FILE_SEPARATOR)) {
			entryPath = entryPath.substring(FILE_SEPARATOR.length(), entryPath.length());
		}
		
		return entryPath;
	}

	private static void writeFileToZip(ZipOutputStream zos, String entryPath, File file) throws IOException {
		zos.putNextEntry(new ZipEntry(entryPath));
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		try {
			byte[] buf = new byte[2048];
			
			int size = -1;
			while ((size = bis.read(buf)) != -1) {
				zos.write(buf, 0, size);
			}
		} finally {
			bis.close();
		}
		
		zos.closeEntry();
	}
	
	public static void unzip(File sourceFile) throws IOException {
		unzip(sourceFile, sourceFile.getParentFile());
	}
	
	public static void unzip(File sourceFile, File targetFolder) throws IOException {
		if (sourceFile.isDirectory()) {
			throw new IllegalArgumentException(String.format("%s is a directory.", sourceFile));
		}
		
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(sourceFile);
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				
				if (entry.isDirectory()) {
					// ignore
					continue;
				}
				
				File file = getFile(targetFolder, entry.getName());
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				
				if (file.exists()) {
					Files.delete(file.toPath());
				}
				
				writeFileToZip(zipFile, entry, file);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (zipFile != null)
				zipFile.close();
		}
		
	}

	public static void writeFileToZip(ZipFile zipFile, ZipEntry entry, File file) throws IOException {
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		
		try {
			in = new BufferedInputStream(zipFile.getInputStream(entry));
			out = new BufferedOutputStream(new FileOutputStream(file));
			
			byte[] buf = new byte[2048];
			int size = -1;
			while ((size = in.read(buf)) != -1) {
				out.write(buf, 0, size);
			}
		} finally {
			AprilUtils.close(in);
			AprilUtils.close(out);
		}
	}

	private static File getFile(File targetFolder, String entryPath) {
		return new File(targetFolder, entryPath);
	}
}
