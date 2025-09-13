package fr.siovision.voyages.web;

import fr.siovision.voyages.application.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/presign")
    public Map<String, String> presign(@RequestParam String filename,
                                       @RequestParam(defaultValue = "image/jpeg") String contentType) {
        String key = fileService.buildCoverKey(filename);
        return fileService.presignPut(key, contentType);
    }
}
