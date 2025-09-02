package com.newnormallist.userservice.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CategoryResponse {
    private final String id;
    private final String categoryName;
    private final String icon;
}
