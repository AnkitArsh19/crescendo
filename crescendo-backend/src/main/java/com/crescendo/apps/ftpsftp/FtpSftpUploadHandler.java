package com.crescendo.apps.ftpsftp;

import com.crescendo.execution.action.*;
import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.net.ftp.FTPClient;
import java.io.*;
import java.util.*;

@ActionMapping(appKey = "ftp-sftp", actionKey = "upload")
public class FtpSftpUploadHandler extends FtpSftpSupport {
    @Override
    public ActionResult execute(ActionContext c) {
        ChannelSftp s = null;
        FTPClient f = null;
        try {
            byte[] b = Base64.getDecoder().decode(cfg(c, "base64", ""));
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
                if (f != null) {
                    f.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
