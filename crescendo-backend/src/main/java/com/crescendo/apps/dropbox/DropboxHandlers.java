package com.crescendo.apps.dropbox;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

class DropboxBase {
    static RestClient api(ActionContext c) {
        return SimpleApiSupport.bearer("https://api.dropboxapi.com/2", SimpleApiSupport.cred(c, "accessToken"));
    }

    static RestClient content(ActionContext c, ObjectMapper mapper, Map<String, Object> arg) throws Exception {
        return RestClient.builder()
                .baseUrl("https://content.dropboxapi.com/2")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SimpleApiSupport.cred(c, "accessToken"))
                .defaultHeader("Dropbox-API-Arg", mapper.writeValueAsString(arg))
                .build();
    }

    static String path(ActionContext c) {
        String path = SimpleApiSupport.cfg(c, "path");
        return "/".equals(path) ? "" : path;
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "list-folder")
class DropboxListFolderHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxListFolderHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String res = DropboxBase.api(c).post().uri("/files/list_folder")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("path", DropboxBase.path(c)))
                    .retrieve().body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Dropbox list folder failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "search")
class DropboxSearchHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxSearchHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            Map<String, Object> options = new LinkedHashMap<>();
            String path = DropboxBase.path(c);
            if (!path.isBlank()) {
                options.put("path", path);
            }
            options.put("max_results", Math.max(1, SimpleApiSupport.intCfg(c, "maxResults", 25)));

            String res = DropboxBase.api(c).post().uri("/files/search_v2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", SimpleApiSupport.cfg(c, "query"), "options", options))
                    .retrieve().body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Dropbox search failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "upload-text")
class DropboxUploadTextHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxUploadTextHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String res = DropboxBase.content(c, mapper, Map.of(
                            "path", SimpleApiSupport.cfg(c, "path"),
                            "mode", "overwrite",
                            "autorename", false
                    )).post().uri("/files/upload")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(SimpleApiSupport.cfg(c, "content").getBytes(StandardCharsets.UTF_8))
                    .retrieve().body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Dropbox upload text failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "download")
class DropboxDownloadHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxDownloadHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            byte[] b = DropboxBase.content(c, mapper, Map.of("path", SimpleApiSupport.cfg(c, "path")))
                    .post().uri("/files/download")
                    .retrieve().body(byte[].class);
            return ActionResult.success(Map.of("base64", Base64.getEncoder().encodeToString(b), "bytes", b.length));
        } catch (Exception e) {
            return ActionResult.failure("Dropbox download failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "delete")
class DropboxDeleteHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxDeleteHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String res = DropboxBase.api(c).post().uri("/files/delete_v2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("path", SimpleApiSupport.cfg(c, "path")))
                    .retrieve().body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Dropbox delete failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "create-folder")
class DropboxCreateFolderHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxCreateFolderHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String res = DropboxBase.api(c).post().uri("/files/create_folder_v2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("path", SimpleApiSupport.cfg(c, "path"), "autorename", false))
                    .retrieve().body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Dropbox create folder failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "create-shared-link")
class DropboxCreateSharedLinkHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxCreateSharedLinkHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("path", SimpleApiSupport.cfg(c, "path"));
            Map<String, Object> settings = new LinkedHashMap<>();
            if (!SimpleApiSupport.cfg(c, "audience").isBlank()) {
                settings.put("audience", SimpleApiSupport.cfg(c, "audience"));
            }
            if (!SimpleApiSupport.cfg(c, "access").isBlank()) {
                settings.put("access", SimpleApiSupport.cfg(c, "access"));
            }
            if (!settings.isEmpty()) {
                body.put("settings", settings);
            }
            String res = DropboxBase.api(c).post().uri("/sharing/create_shared_link_with_settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Dropbox shared link failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "list-revisions")
class DropboxListRevisionsHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxListRevisionsHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String res = DropboxBase.api(c).post().uri("/files/list_revisions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "path", SimpleApiSupport.cfg(c, "path"),
                            "mode", ".path",
                            "limit", Math.max(1, SimpleApiSupport.intCfg(c, "limit", 10))
                    ))
                    .retrieve().body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Dropbox list revisions failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "dropbox", actionKey = "restore-revision")
class DropboxRestoreRevisionHandler implements ActionHandler {
    private final ObjectMapper mapper;
    DropboxRestoreRevisionHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String res = DropboxBase.api(c).post().uri("/files/restore")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("path", SimpleApiSupport.cfg(c, "path"), "rev", SimpleApiSupport.cfg(c, "rev")))
                    .retrieve().body(String.class);
            return SimpleApiSupport.parsed(mapper, res);
        } catch (Exception e) {
            return ActionResult.failure("Dropbox restore revision failed: " + e.getMessage());
        }
    }
}
