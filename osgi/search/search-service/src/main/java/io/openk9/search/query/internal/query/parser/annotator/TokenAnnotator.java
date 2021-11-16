package io.openk9.search.query.internal.query.parser.annotator;

import io.openk9.search.api.query.parser.Annotator;
import io.openk9.search.api.query.parser.CategorySemantics;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Map;

@Component(
	immediate = true, service = Annotator.class
)
public class TokenAnnotator extends BaseAnnotator {

	@Override
	public List<CategorySemantics> annotate_(long tenantId, String...tokens) {

		if (tokens.length == 1) {
			String token = tokens[0];
			return List.of(
				CategorySemantics.of(
					"$TOKEN",
					Map.of(
						"tokenType", "TEXT",
						"value", token
					)
				)
			);
		}

		return List.of();

	}

	@Override
	@Reference
	protected void setAnnotatorConfig(
		AnnotatorConfig annotatorConfig) {
		super.setAnnotatorConfig(annotatorConfig);
	}

}
