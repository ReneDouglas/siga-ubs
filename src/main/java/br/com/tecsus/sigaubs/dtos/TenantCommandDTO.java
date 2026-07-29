package br.com.tecsus.sigaubs.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class TenantCommandDTO {

    @Positive
    private Long id;
    @Pattern(regexp = "[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?")
    private String slug;
    @NotBlank
    @Size(max = 255)
    private String name;
    @Size(max = 255)
    private String domain;

    @AssertTrue(message = "Slug obrigatório.")
    public boolean isSlugPresentForCreation() {
        return id != null || (slug != null && !slug.isBlank());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
