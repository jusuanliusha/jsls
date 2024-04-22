package com.jsls.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * 流工具类
 * 
 * @author YM10177
 *
 */
public class IOUtils {
	public static final Logger logger = LoggerFactory.getLogger(IOUtils.class);
	public static final int BUFFER_SIZE = 1024 * 8;

	public static BufferedReader useTextReader(File file, String charsetName) {
		return useTextReader(useInputStream(file), charsetName);
	}

	public static BufferedReader useTextReader(InputStream in, String charsetName) {
		try {
			return new BufferedReader(new InputStreamReader(in, charsetName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static InputStream useTextInputStream(String text, String charsetName) {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(text.getBytes(charsetName));
			return new BufferedInputStream(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static FileInputStream useInputStream(File file) {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			logger.error("{} 文件不存在", file);
			throw new RuntimeException(e);
		}
	}

	public static FileOutputStream useOutputStream(File file) {
		File dirFile = file.getParentFile();
		if (!dirFile.exists()) {
			dirFile.mkdirs();
		}
		try {
			return new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			logger.error("{} 文件不存在", file);
			throw new RuntimeException(e);
		}
	}

	public static FileInputStream useInputStream(String dir, String fileName) {
		return useInputStream(new File(dir, fileName));
	}

	public static FileOutputStream useOutputStream(String dir, String fileName) {
		return useOutputStream(new File(dir, fileName));
	}

	public static void processText(File file, final Consumer<String> lineConsumer) {
		processText(file, "UTF-8", lineConsumer);
	}

	public static void processText(File file, String charsetName, final Consumer<String> lineConsumer) {
		processText(useInputStream(file), charsetName, lineConsumer);
	}

	public static void processText(InputStream is, String charsetName, final Consumer<String> lineConsumer) {
		BufferedReader reader = null;
		String line = null;
		try {
			reader = useTextReader(is, charsetName);
			while ((line = reader.readLine()) != null) {
				lineConsumer.accept(line);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException("读取文本异常：" + e.getMessage(), e);
		} finally {
			closeQuietly(is);
			closeQuietly(reader);
		}
	}

	/**
	 * 从文件读text UTF-8
	 * 
	 * @param file
	 * @return
	 */
	public static String readText(File file) {
		return readText(file, "UTF-8");
	}

	/**
	 * 从文件读text
	 * 
	 * @param file
	 * @param charsetName 编码
	 * @return
	 */
	public static String readText(File file, String charsetName) {
		return readText(useInputStream(file), charsetName);
	}

	/**
	 * 从输入流读text charsetName
	 * 
	 * @param is
	 * @param charsetName
	 * @return
	 */
	public static String readText(InputStream is) {
		return readText(is, "UTF-8");
	}

	/**
	 * 从输入流读text charsetName
	 * 
	 * @param is
	 * @param charsetName
	 * @return
	 */
	public static String readText(InputStream is, String charsetName) {
		BufferedReader reader = null;
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			reader = useTextReader(is, charsetName);
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException("读取文本异常：" + e.getMessage(), e);
		} finally {
			closeQuietly(is);
			closeQuietly(reader);
		}
		return sb.toString();
	}

	public static void save(MultipartFile mf, File file) {
		try {
			save(mf.getInputStream(), file);
		} catch (IOException e) {
			logger.error("保存文件失败：" + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public static void save(InputStream in, File file) {
		copy(in, useOutputStream(file));
	}

	public static void save(String text, File file) {
		OutputStream out = null;
		try {
			out = useOutputStream(file);
			writeText(out, text);
		} catch (IOException e) {
			logger.error("保存文件失败：" + e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			closeQuietly(out);
		}
	}

	public static void copy(InputStream in, OutputStream out) {
		try {
			write(in, out, BUFFER_SIZE);
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException("写入流异常：" + e.getMessage(), e);
		} finally {
			closeQuietly(in);
			closeQuietly(out);
		}
	}

	public static long writeText(OutputStream os, String text) throws IOException {
		return writeText(os, text, "UTF-8");
	}

	public static long writeText(OutputStream os, String text, String charsetName) throws IOException {
		InputStream in = useTextInputStream(text, charsetName);
		try {
			return write(in, os, BUFFER_SIZE);
		} finally {
			closeQuietly(in);
		}
	}

	public static long write(InputStream is, OutputStream os, int bufferSize) throws IOException {
		int read;
		long total = 0;
		byte[] buff = new byte[bufferSize];
		while ((read = is.read(buff)) != -1) {
			if (read > 0) {
				os.write(buff, 0, read);
				total += read;
			}
		}
		return total;
	}

	public static void write(final byte[] data, OutputStream os) throws IOException {
		if (data != null) {
			os.write(data);
		}
	}

	public static void writeInt(OutputStream os, int num) throws IOException {
		os.write(num >>> 24);
		os.write(num >>> 16);
		os.write(num >>> 8);
		os.write(num);
	}

	public static void closeQuietly(final Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException e) {
			// Ignore
		}
	}
}