package com.wardk.meeteam_backend.web.project.dto;


import jakarta.validation.constraints.NotBlank;

public class SubCategoryDto {

    @NotBlank
    private String subCategory;

    private int count;
}
