package com.crescendo.apps.ftpsftp;

import com.crescendo.execution.action.*;
import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.net.ftp.FTPClient;
import java.util.*;

@ActionMapping(appKey = "ftp-sftp", actionKey = "list")
public class FtpSftpListHandler extends FtpSftpSupport {
    @Override
    public ActionResult execute(ActionContext c) {
        ChannelSftp s = null;
        FTPClient f = null;
        try {
            String path = cfg(c, "path", "/");
            List<Map<String, Object>> files = new ArrayList<>();
            if (sftp(c)) {
                s = sftpClient(c);
                for (Object o : s.ls(path)) {
                    ChannelSftp.LsEntry e = (ChannelSftp.LsEntry) o;
                    files.add(Map.of(
                            "name", e.getFilename(),
                            "size", e.getAttrs().getSize(),
                            "directory", e.getAttrs().isDir()
                    ));
                }
            } else {
                f = ftp(c);
                for (var e : f.listFiles(path)) {
                    files.add(Map.of(
                            "name", e.getName(),
                            "size", e.getSize(),
                            "directory", e.isDirectory()
                    ));
                }
            }
            return ActionResult.success(Map.of("files", files, "count", files.size()));
        } catch (Exception e) {
            return ActionResult.failure("FTP/SFTP list failed: " + e.getMessage());
        } finally {
            close(s);
            try {
                if (f != null) {
                    f.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
