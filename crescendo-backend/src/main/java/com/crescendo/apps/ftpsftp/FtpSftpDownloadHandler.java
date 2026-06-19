package com.crescendo.apps.ftpsftp;

import com.crescendo.execution.action.*;
import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.net.ftp.FTPClient;
import java.io.*;
import java.util.*;

@ActionMapping(appKey = "ftp-sftp", actionKey = "download")
public class FtpSftpDownloadHandler extends FtpSftpSupport {
    @Override
    public ActionResult execute(ActionContext c) {
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
                if (f != null) {
                    f.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
