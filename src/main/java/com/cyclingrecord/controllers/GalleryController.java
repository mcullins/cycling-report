package com.cyclingrecord.controllers;

import com.cyclingrecord.models.DBFile;
import com.cyclingrecord.models.Entry;
import com.cyclingrecord.payload.UploadFileResponse;
import com.cyclingrecord.services.DBFileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Controller
public class GalleryController {
    private static final Logger logger = LoggerFactory.getLogger(GalleryController.class);

    @Autowired
    private DBFileStorageService dbFileStorageService;

    public ArrayList<String> encodeImages(){
        ArrayList<DBFile> imageList = new ArrayList<>();
        ArrayList<String> encodedList = new ArrayList<>();
        imageList = dbFileStorageService.getImage();
        for(int i = 0; i<imageList.size(); i++) {
            encodedList.add(Base64.getEncoder().encodeToString(imageList.get(i).getImage()));
        }
        return encodedList;
    }

    @PostMapping("/gallery")
    public UploadFileResponse uploadImage(Model model, @RequestParam("image")MultipartFile image){
        DBFile dbFile = dbFileStorageService.storeImage(image);

        String imageDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/gallery")
                .path(dbFile.getId())
                .toUriString();

        model.addAttribute("imageList", encodeImages());

        return new UploadFileResponse(dbFile.getName(), imageDownloadUri, image.getSize());
    }

    @GetMapping("/gallery")
    public String downloadImage(Model model) {
        model.addAttribute("imageList", encodeImages());
        return "gallery";
    }
}


