package com.redmancometh.configcore.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

/**
 * 
 * @author Redmancometh
 *
 */
public class DirectoryWatcher {
	private String monitored;
	private Consumer<File> onChangedCallback;
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private Map<String, String> hashMap = new ConcurrentHashMap<>();
	private ScheduledFuture<?> future;
	private String[] extensions;

	/**
	 * Create a file watcher to watch for changes on the given file, and execute the
	 * given callback. Until start is called the underlying scheduler will not run
	 * and this will not work, so make sure to call start!
	 * 
	 * @param onChanged
	 * @param monitored
	 */
	public DirectoryWatcher(Consumer<File> onChanged, String monitored, String... extensions) {
		this.onChangedCallback = onChanged;
		this.monitored = monitored;
		this.extensions = extensions;
	}

	/**
	 * Turn it on.
	 */
	public void start() {
		this.future = scheduler.scheduleAtFixedRate(() -> {
			FileUtils.listFiles(new File(monitored), extensions, true).forEach((file) -> {
				if (hasChanged(file))
					onChangedCallback.accept(file);
			});
		}, 1, 1, TimeUnit.SECONDS);

	}

	/**
	 * Turn it off. May throw an execption.
	 */
	public void stop() {
		future.cancel(true);
	}

	public String getHash(File file) {
		try (FileInputStream fis = new FileInputStream(file)) {
			String sha1 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(fis);
			return sha1;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean hasChanged(File file) {
		boolean hasHash = this.hashMap.containsKey(file.getName());
		String newHash = getHash(file);
		String lastHash = this.hashMap.getOrDefault(file.getName(), getHash(file));
		boolean isEqual = newHash.equals(lastHash);
		hashMap.put(file.getName(), newHash);
		if (!hasHash)
			return true;
		if (isEqual)
			return false;
		return true;
	}

}
