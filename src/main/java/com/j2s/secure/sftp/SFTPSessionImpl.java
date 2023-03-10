package com.j2s.secure.sftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j(topic = "sftp")
class SFTPSessionImpl extends SFTPAbstractSession implements SFTPSession {

	public SFTPSessionImpl(String sessionKey) {
		super(sessionKey);
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	@Override
	public void cd(String path) throws SftpException {
		channel.cd(path);
	}

	@Override
	public String pwd() throws SftpException {
		return channel.pwd();
	}

	@Override
	public List<ChannelSftp.LsEntry> ls() throws SftpException {
		return ls(pwd());
	}

	@Override
	public List<ChannelSftp.LsEntry> ls(String path) throws SftpException {
		Vector<ChannelSftp.LsEntry> ls = channel.ls(path);
		return new ArrayList<>(ls);
	}

	@Override
	public InputStream get(String name) throws SftpException {
		return _get(name);
	}

	private InputStream _get(String name) throws SftpException {
		return channel.get(name);
	}

	@Override
	public File getFile(String name) throws SftpException, IOException {
		File file = Files.createTempFile(name, ".tmp").toFile();
		file.deleteOnExit();
		try(
				FileOutputStream fos = new FileOutputStream(file);
				InputStream is = _get(name);) {
			IOUtils.copy(is, fos);
		}
		return file;
	}

	@Override
	public void rm(String name) throws SftpException {
		SftpATTRS stat = channel.stat(name);
		_rm(stat, name);
	}

	private void _rm(SftpATTRS stat, String fPath) throws SftpException {
		if(stat.isDir()) {
			_rmRecv(fPath);
			channel.rmdir(fPath);
		} else {
			channel.rm(fPath);
		}
	}

	private void _rmRecv(String rPath) throws SftpException {
		List<ChannelSftp.LsEntry> lsEntries = ls(rPath);
		for (ChannelSftp.LsEntry lsEntry : lsEntries) {
			String fileName = lsEntry.getFilename();
			SftpATTRS stat = lsEntry.getAttrs();
			if(fileName.equals(".") || fileName.equals("..")) {
				continue;
			}
			String fPath = rPath + "/" + fileName;
			_rm(stat, fPath);
		}
	}

	@Override
	public void put(File file) throws SftpException {
		put(file, file.getName());
	}

	@Override
	public void put(File file, String name) throws SftpException {
		channel.put(file.getAbsolutePath(), name);
	}

	@Override
	public String cat(String name) throws SftpException, IOException {
		try(InputStream is = _get(name)) {
			return IOUtils.toString(is, Charset.defaultCharset());
		}
	}

	@Override
	public void rename(String srcName, String dstName) throws SftpException {
		channel.rename(srcName, dstName);
	}
}
