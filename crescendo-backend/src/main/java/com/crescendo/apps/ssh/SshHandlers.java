package com.crescendo.apps.ssh;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class SshHandlers {

    @ActionMapping(appKey = "ssh", actionKey = "run-command")
    public ActionResult runCommand(ActionContext c) {
        try {
            String cmd = String.valueOf(c.configuration().getOrDefault("command", ""));
            if (cmd.isBlank()) {
                return ActionResult.failure("SSH command is required");
            }
            String cwd = String.valueOf(c.configuration().getOrDefault("cwd", ""));
            if (!cwd.isBlank() && !cwd.equals("null") && !cwd.equals("/")) {
                cmd = "cd '" + cwd.replace("'", "'\\''") + "' && " + cmd;
            }
            
            Session session = getSession(c);
            session.connect(15000);
            
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            channel.setInputStream(null);
            
            InputStream in = channel.getInputStream();
            InputStream err = channel.getExtInputStream();
            
            channel.connect();
            
            String outStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String errStr = new String(err.readAllBytes(), StandardCharsets.UTF_8);
            
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            
            int exitStatus = channel.getExitStatus();
            channel.disconnect();
            session.disconnect();
            
            return ActionResult.success(Map.of(
                "exitCode", exitStatus, 
                "output", outStr,
                "error", errStr
            ));
        } catch (Exception e) {
            return ActionResult.failure("SSH command failed: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "ssh", actionKey = "download")
    public ActionResult download(ActionContext c) {
        try {
            String path = String.valueOf(c.configuration().getOrDefault("path", ""));
            if (path.isBlank()) {
                return ActionResult.failure("Remote path is required");
            }
            
            Session session = getSession(c);
            session.connect(15000);
            
            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            sftp.get(path, out);
            
            sftp.disconnect();
            session.disconnect();
            
            byte[] b = out.toByteArray();
            return ActionResult.success(Map.of(
                "base64", Base64.getEncoder().encodeToString(b),
                "bytes", b.length
            ));
        } catch (Exception e) {
            return ActionResult.failure("SSH download failed: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "ssh", actionKey = "upload")
    public ActionResult upload(ActionContext c) {
        try {
            String path = String.valueOf(c.configuration().getOrDefault("path", ""));
            if (path.isBlank()) {
                return ActionResult.failure("Remote path is required");
            }
            
            boolean isBinary = "true".equalsIgnoreCase(String.valueOf(c.configuration().getOrDefault("binaryData", "true")));
            byte[] b;
            if (isBinary) {
                b = Base64.getDecoder().decode(String.valueOf(c.configuration().getOrDefault("base64", "")));
            } else {
                b = String.valueOf(c.configuration().getOrDefault("fileContent", "")).getBytes(StandardCharsets.UTF_8);
            }
            
            Session session = getSession(c);
            session.connect(15000);
            
            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            
            sftp.put(new ByteArrayInputStream(b), path);
            
            sftp.disconnect();
            session.disconnect();
            
            return ActionResult.success(Map.of(
                "uploaded", true,
                "bytes", b.length,
                "path", path
            ));
        } catch (Exception e) {
            return ActionResult.failure("SSH upload failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private Session getSession(ActionContext c) throws Exception {
        JSch jsch = new JSch();
        String pk = cred(c, "privateKey", "");
        if (!pk.isBlank()) {
            String pass = cred(c, "password", "");
            jsch.addIdentity("crescendo", pk.getBytes(StandardCharsets.UTF_8), null, 
                pass.isBlank() ? null : pass.getBytes(StandardCharsets.UTF_8));
        }
        
        int port = Integer.parseInt(cred(c, "port", "22"));
        Session session = jsch.getSession(cred(c, "username", ""), cred(c, "host", ""), port);
        
        if (pk.isBlank()) {
            session.setPassword(cred(c, "password", ""));
        }
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }

    private String cred(ActionContext c, String k, String f) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null || String.valueOf(v).isBlank() ? f : String.valueOf(v);
    }
}
