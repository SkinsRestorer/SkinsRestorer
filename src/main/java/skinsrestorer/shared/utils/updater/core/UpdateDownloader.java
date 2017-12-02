/*
 * Copyright 2016 inventivetalent. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and contributors and should not be interpreted as representing official policies,
 *  either expressed or implied, of anybody else.
 */

package skinsrestorer.shared.utils.updater.core;

import skinsrestorer.shared.utils.updater.core.ResourceInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class UpdateDownloader {

	public static final String RESOURCE_DOWNLOAD = "http://api.spiget.org/v2/resources/%s/download";

	public static Runnable downloadAsync(final ResourceInfo info, final File file, final String userAgent, final DownloadCallback callback) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					download(info, file, userAgent);
					callback.finished();
				} catch (Exception e) {
					callback.error(e);
				}
			}
		};
	}

	public static void download(ResourceInfo info, File file) {
		download(info, file);
	}

	public static void download(ResourceInfo info, File file, String userAgent) {
		if (info.external) { throw new IllegalArgumentException("Cannot download external resource #" + info.id); }
		ReadableByteChannel channel;
		try {
			//https://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
			HttpURLConnection connection = (HttpURLConnection) new URL(String.format(RESOURCE_DOWNLOAD, info.id)).openConnection();
			connection.setRequestProperty("User-Agent", userAgent);
			if (connection.getResponseCode() != 200) {
				throw new RuntimeException("Download returned status #" + connection.getResponseCode());
			}
			channel = Channels.newChannel(connection.getInputStream());
		} catch (IOException e) {
			throw new RuntimeException("Download failed", e);
		}
		try {
			FileOutputStream output = new FileOutputStream(file);
			output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
			output.flush();
			output.close();
		} catch (IOException e) {
			throw new RuntimeException("Could not save file", e);
		}
	}

}
