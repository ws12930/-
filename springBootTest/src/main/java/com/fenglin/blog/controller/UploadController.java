package com.fenglin.blog.controller;

import com.fenglin.blog.common.BizException;
import com.fenglin.blog.common.R;
import com.fenglin.blog.security.RequireAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传。
 *
 * <p>安全约定：
 * <ul>
 *   <li>扩展名白名单，显式排除 svg / html / js 等可执行或可承载脚本的类型 —— 否则
 *       {@code /uploads/**} 会变成存储型 XSS 的投放点。</li>
 *   <li>落盘文件名完全由服务端生成（UUID + 白名单扩展名），不使用用户提供的名字，
 *       从根本上杜绝路径穿越与文件覆盖。</li>
 *   <li>落盘前后都做路径规范化校验，确保最终路径仍在 uploads 根目录之内。</li>
 *   <li>必须登录（{@link RequireAuth}）才能上传。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class UploadController {

    /** 图片：允许内联展示 */
    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /** 附件：仅常见文档，均为浏览器不会当作脚本执行的类型 */
    private static final Set<String> DOC_EXTENSIONS =
            Set.of("pdf", "txt", "md", "csv", "zip", "doc", "docx", "xls", "xlsx", "ppt", "pptx");

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_DOC_BYTES = 20L * 1024 * 1024;

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path uploadRoot;

    public UploadController(@Value("${blog.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/upload")
    @RequireAuth
    public R<UploadedFile> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(originalName);

        boolean image = IMAGE_EXTENSIONS.contains(extension);
        if (!image && !DOC_EXTENSIONS.contains(extension)) {
            throw BizException.badRequest("不支持的文件类型，仅支持常见图片与文档");
        }

        long limit = image ? MAX_IMAGE_BYTES : MAX_DOC_BYTES;
        if (file.getSize() > limit) {
            throw BizException.badRequest(
                    (image ? "图片" : "文件") + "过大，上限 " + (limit / 1024 / 1024) + "MB");
        }

        String datePath = LocalDate.now().format(DATE_PATH);
        Path directory = resolveInsideRoot(datePath);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = resolveInsideRoot(datePath + "/" + storedName);

        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw BizException.badRequest("文件写入失败，请稍后重试");
        }

        String url = "/uploads/" + datePath + "/" + storedName;
        return R.ok("上传成功",
                new UploadedFile(url, sanitizeName(originalName), file.getSize(), image));
    }

    /** 解析相对路径并确保结果仍位于 uploads 根目录内，否则直接拒绝 */
    private Path resolveInsideRoot(String relative) {
        Path resolved = uploadRoot.resolve(relative).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw BizException.badRequest("非法的上传路径");
        }
        return resolved;
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 原始文件名仅用于回显给用户，剥掉目录部分与控制字符 */
    private static String sanitizeName(String filename) {
        String base = filename.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        String cleaned = base.replaceAll("\\p{Cntrl}", "").trim();
        return cleaned.isEmpty() ? "未命名文件" : cleaned;
    }

    /** @param image true 表示前端应以图片形式内联展示，false 表示作为附件链接 */
    public record UploadedFile(String url, String name, long size, boolean image) {
    }
}
