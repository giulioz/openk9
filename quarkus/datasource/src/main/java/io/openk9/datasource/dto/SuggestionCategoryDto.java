package io.openk9.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SuggestionCategoryDto{
	private Long tenantId;
	private Long categoryId;
	private Long parentCategoryId;
	private String name;
}
