package com.crescendo.apps.ftpsftp;

import com.crescendo.execution.action.*;
import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

abstract class FtpSftpSupport {
    String cfg(ActionContext c, String k, String f) {
        Object v = c.configuration().get(k);
        return v == null || String.valueOf(v).isBlank() ? f : String.valueOf(v);
    }

    String cred(ActionContext c, String k, String f) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null || String.valueOf(v).isBlank() ? f : String.valueOf(v);
    }

    boolean sftp(ActionContext c) {
        return "sftp".equalsIgnoreCase(cred(c, "protocol", "ftp"));
    }

    FTPClient ftp(ActionContext c) throws Exception {
        FTPClient f = new FTPClient();
        int timeout = Integer.parseInt(cfg(c, "timeout", "10000"));
        f.setConnectTimeout(timeout);
        f.setDefaultTimeout(timeout);
        f.connect(cred(c, "host", ""), Integer.parseInt(cred(c, "port", "21")));
        f.setSoTimeout(timeout);
        if (!f.login(cred(c, "username", ""), cred(c, "password", ""))) {
            throw new IOException("FTP login failed");
        }
        f.enterLocalPassiveMode();
        f.setFileType(FTP.BINARY_FILE_TYPE);
        return f;
    }

    @SuppressWarnings("deprecation")
    ChannelSftp sftpClient(ActionContext c) throws Exception {
        JSch j = new JSch();
        String pk = cred(c, "privateKey", "");
        if (!pk.isBlank()) {
            j.addIdentity("crescendo", pk.getBytes(StandardCharsets.UTF_8), null,
                    cred(c, "password", "").isBlank() ? null : cred(c, "password", "").getBytes(StandardCharsets.UTF_8));
        }
        Session s = j.getSession(cred(c, "username", ""), cred(c, "host", ""), Integer.parseInt(cred(c, "port", "22")));
        if (pk.isBlank()) {
            s.setPassword(cred(c, "password", ""));
        }
        s.setConfig("StrictHostKeyChecking", "no");
        int timeout = Integer.parseInt(cfg(c, "timeout", "10000"));
        s.connect(timeout);
        ChannelSftp ch = (ChannelSftp) s.openChannel("sftp");
        ch.connect(timeout);
        return ch;
    }

    void close(ChannelSftp ch) {
        if (ch != null) {
            Session s = null;
            try {
                s = ch.getSession();
            } catch (Exception ignored) {
            }
            ch.disconnect();
            if (s != null) {
                s.disconnect();
            }
        }
    }
}
