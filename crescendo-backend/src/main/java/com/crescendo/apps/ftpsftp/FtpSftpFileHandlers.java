package com.crescendo.apps.ftpsftp;

import com.crescendo.execution.action.*;
import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.*;

@Component
public class FtpSftpFileHandlers extends FtpSftpSupport {
    private static final Logger logger = LoggerFactory.getLogger(FtpSftpFileHandlers.class);

    @ActionMapping(appKey = "ftp-sftp", actionKey = "delete")
    public ActionResult delete(ActionContext context) {
        String path = cfg(context, "path", "");
        if (path.isBlank()) {
            return ActionResult.failure("Missing remote path");
        }
        if (sftp(context)) {
            ChannelSftp ch = null;
            try {
                ch = sftpClient(context);
                boolean isFolder = "true".equalsIgnoreCase(cfg(context, "folder", "false"));
                boolean isRecursive = "true".equalsIgnoreCase(cfg(context, "recursive", "false"));
                if (isFolder) {
                    if (isRecursive) {
                        recursiveSftpDelete(ch, path);
                    } else {
                        ch.rmdir(path);
                    }
                } else {
                    ch.rm(path);
                }
                return ActionResult.success(Map.of("deleted", true, "path", path));
            } catch (Exception e) {
                logger.error("SFTP delete failed", e);
                return ActionResult.failure("SFTP delete failed: " + e.getMessage());
            } finally {
                close(ch);
            }
        } else {
            FTPClient f = null;
            try {
                f = ftp(context);
                boolean isFolder = "true".equalsIgnoreCase(cfg(context, "folder", "false"));
                boolean isRecursive = "true".equalsIgnoreCase(cfg(context, "recursive", "false"));
                boolean ok;
                if (isFolder) {
                    if (isRecursive) {
                        ok = recursiveFtpDelete(f, path);
                    } else {
                        ok = f.removeDirectory(path);
                    }
                } else {
                    ok = f.deleteFile(path);
                }
                if (!ok) {
                    return ActionResult.failure("FTP delete failed: " + f.getReplyString());
                }
                return ActionResult.success(Map.of("deleted", true, "path", path));
            } catch (Exception e) {
                logger.error("FTP delete failed", e);
                return ActionResult.failure("FTP delete failed: " + e.getMessage());
            } finally {
                if (f != null) {
                    try { f.disconnect(); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void recursiveSftpDelete(ChannelSftp ch, String path) throws Exception {
        java.util.Vector<ChannelSftp.LsEntry> list = ch.ls(path);
        for (ChannelSftp.LsEntry entry : list) {
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) continue;
            String p = path + (path.endsWith("/") ? "" : "/") + entry.getFilename();
            if (entry.getAttrs().isDir()) {
                recursiveSftpDelete(ch, p);
            } else {
                ch.rm(p);
            }
        }
        ch.rmdir(path);
    }

    private boolean recursiveFtpDelete(FTPClient f, String path) throws Exception {
        org.apache.commons.net.ftp.FTPFile[] files = f.listFiles(path);
        if (files != null) {
            for (org.apache.commons.net.ftp.FTPFile file : files) {
                if (file.getName().equals(".") || file.getName().equals("..")) continue;
                String p = path + (path.endsWith("/") ? "" : "/") + file.getName();
                if (file.isDirectory()) {
                    if (!recursiveFtpDelete(f, p)) return false;
                } else {
                    if (!f.deleteFile(p)) return false;
                }
            }
        }
        return f.removeDirectory(path);
    }

    @ActionMapping(appKey = "ftp-sftp", actionKey = "download")
    public ActionResult download(ActionContext c) {
        ChannelSftp s = null;
        FTPClient f = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String path = cfg(c, "path", "");
            if (sftp(c)) {
                s = sftpClient(c);
                s.get(path, out);
            } else {
                f = ftp(c);
                if (!f.retrieveFile(path, out)) {
                    throw new IOException("FTP retrieve failed");
                }
            }
            byte[] b = out.toByteArray();
            return ActionResult.success(Map.of("base64", Base64.getEncoder().encodeToString(b), "bytes", b.length));
        } catch (Exception e) {
            return ActionResult.failure("FTP/SFTP download failed: " + e.getMessage());
        } finally {
            close(s);
            try {
                if (f != null) f.disconnect();
            } catch (Exception ignored) {}
        }
    }

    @ActionMapping(appKey = "ftp-sftp", actionKey = "upload")
    public ActionResult upload(ActionContext c) {
        ChannelSftp s = null;
        FTPClient f = null;
        try {
            boolean isBinary = "true".equalsIgnoreCase(cfg(c, "binaryData", "true"));
            byte[] b;
            if (isBinary) {
                b = Base64.getDecoder().decode(cfg(c, "base64", ""));
            } else {
                b = cfg(c, "fileContent", "").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            String path = cfg(c, "path", "");
            if (sftp(c)) {
                s = sftpClient(c);
                s.put(new ByteArrayInputStream(b), path);
            } else {
                f = ftp(c);
                if (!f.storeFile(path, new ByteArrayInputStream(b))) {
                    throw new IOException("FTP upload failed");
                }
            }
            return ActionResult.success(Map.of("uploaded", true, "bytes", b.length, "path", path));
        } catch (Exception e) {
            return ActionResult.failure("FTP/SFTP upload failed: " + e.getMessage());
        } finally {
            close(s);
            try {
                if (f != null) f.disconnect();
            } catch (Exception ignored) {}
        }
    }

    @ActionMapping(appKey = "ftp-sftp", actionKey = "list")
    public ActionResult list(ActionContext c) {
        ChannelSftp s = null;
        FTPClient f = null;
        try {
            String path = cfg(c, "path", "/");
            List<Map<String, Object>> files = new ArrayList<>();
            boolean recursive = "true".equalsIgnoreCase(cfg(c, "recursive", "false"));
            if (sftp(c)) {
                s = sftpClient(c);
                if (recursive) {
                    listSftpRecursive(s, path, files);
                } else {
                    for (Object o : s.ls(path)) {
                        ChannelSftp.LsEntry e = (ChannelSftp.LsEntry) o;
                        files.add(Map.of(
                                "name", e.getFilename(),
                                "path", path + (path.endsWith("/") ? "" : "/") + e.getFilename(),
                                "size", e.getAttrs().getSize(),
                                "directory", e.getAttrs().isDir()
                        ));
                    }
                }
            } else {
                f = ftp(c);
                if (recursive) {
                    listFtpRecursive(f, path, files);
                } else {
                    for (var e : f.listFiles(path)) {
                        files.add(Map.of(
                                "name", e.getName(),
                                "path", path + (path.endsWith("/") ? "" : "/") + e.getName(),
                                "size", e.getSize(),
                                "directory", e.isDirectory()
                        ));
                    }
                }
            }
            return ActionResult.success(Map.of("files", files, "count", files.size()));
        } catch (Exception e) {
            return ActionResult.failure("FTP/SFTP list failed: " + e.getMessage());
        } finally {
            close(s);
            try {
                if (f != null) f.disconnect();
            } catch (Exception ignored) {}
        }
    }

    private void listSftpRecursive(ChannelSftp ch, String path, List<Map<String, Object>> files) throws Exception {
        java.util.Vector<ChannelSftp.LsEntry> list = ch.ls(path);
        for (ChannelSftp.LsEntry e : list) {
            if (e.getFilename().equals(".") || e.getFilename().equals("..")) continue;
            String p = path + (path.endsWith("/") ? "" : "/") + e.getFilename();
            files.add(Map.of(
                    "name", e.getFilename(),
                    "path", p,
                    "size", e.getAttrs().getSize(),
                    "directory", e.getAttrs().isDir()
            ));
            if (e.getAttrs().isDir()) {
                listSftpRecursive(ch, p, files);
            }
        }
    }

    private void listFtpRecursive(FTPClient f, String path, List<Map<String, Object>> files) throws Exception {
        org.apache.commons.net.ftp.FTPFile[] list = f.listFiles(path);
        if (list != null) {
            for (org.apache.commons.net.ftp.FTPFile e : list) {
                if (e.getName().equals(".") || e.getName().equals("..")) continue;
                String p = path + (path.endsWith("/") ? "" : "/") + e.getName();
                files.add(Map.of(
                        "name", e.getName(),
                        "path", p,
                        "size", e.getSize(),
                        "directory", e.isDirectory()
                ));
                if (e.isDirectory()) {
                    listFtpRecursive(f, p, files);
                }
            }
        }
    }

    @ActionMapping(appKey = "ftp-sftp", actionKey = "rename")
    public ActionResult rename(ActionContext context) {
        String path = cfg(context, "path", "");
        String newPath = cfg(context, "newPath", "");
        if (path.isBlank() || newPath.isBlank()) {
            return ActionResult.failure("Missing remote path or new remote path");
        }
        if (sftp(context)) {
            ChannelSftp ch = null;
            try {
                ch = sftpClient(context);
                boolean createDirs = "true".equalsIgnoreCase(cfg(context, "createDirectories", "false"));
                if (createDirs) {
                    createDirectoriesSftp(ch, newPath);
                }
                ch.rename(path, newPath);
                return ActionResult.success(Map.of("renamed", true, "oldPath", path, "newPath", newPath));
            } catch (Exception e) {
                logger.error("SFTP rename failed", e);
                return ActionResult.failure("SFTP rename failed: " + e.getMessage());
            } finally {
                close(ch);
            }
        } else {
            FTPClient f = null;
            try {
                f = ftp(context);
                boolean createDirs = "true".equalsIgnoreCase(cfg(context, "createDirectories", "false"));
                if (createDirs) {
                    createDirectoriesFtp(f, newPath);
                }
                boolean ok = f.rename(path, newPath);
                if (!ok) {
                    return ActionResult.failure("FTP rename failed: " + f.getReplyString());
                }
                return ActionResult.success(Map.of("renamed", true, "oldPath", path, "newPath", newPath));
            } catch (Exception e) {
                logger.error("FTP rename failed", e);
                return ActionResult.failure("FTP rename failed: " + e.getMessage());
            } finally {
                if (f != null) {
                    try { f.disconnect(); } catch (Exception ignored) {}
                }
            }
        }
    }

    private void createDirectoriesSftp(ChannelSftp ch, String path) {
        String parent = path.substring(0, path.lastIndexOf('/'));
        if (parent.isEmpty() || parent.equals("/")) return;
        try {
            ch.ls(parent);
        } catch (Exception e) {
            createDirectoriesSftp(ch, parent);
            try {
                ch.mkdir(parent);
            } catch (Exception ignored) {}
        }
    }

    private void createDirectoriesFtp(FTPClient f, String path) {
        String parent = path.substring(0, path.lastIndexOf('/'));
        if (parent.isEmpty() || parent.equals("/")) return;
        try {
            if (!f.changeWorkingDirectory(parent)) {
                createDirectoriesFtp(f, parent);
                f.makeDirectory(parent);
            }
            f.changeWorkingDirectory("/");
        } catch (Exception ignored) {}
    }
}
