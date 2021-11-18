package io.openk9.search.query.internal.query.parser.annotator;

import io.openk9.search.api.query.parser.Annotator;
import io.openk9.search.api.query.parser.CategorySemantics;
import io.openk9.search.client.api.RestHighLevelClientProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Map;

@Component(
	immediate = true, service = Annotator.class
)
public class IstatTopicAggregatorAnnotator extends BaseAggregatorAnnotator {

	public IstatTopicAggregatorAnnotator() {
		super("istat.topic");
	}

	@Override
	protected CategorySemantics _createCategorySemantics(
		String aggregatorName, String aggregatorKey) {

		return CategorySemantics.of(
			"$AGGREGATE",
			Map.of(
				"tokenType", "TEXT",
				"keywordKey", aggregatorName,
				"value", aggregatorKey
			)
		);

	}

	@Override
	@Reference
	protected void setAnnotatorConfig(
		AnnotatorConfig annotatorConfig) {
		super.setAnnotatorConfig(annotatorConfig);
	}

	@Override
	@Reference
	public void setRestHighLevelClientProvider(
		RestHighLevelClientProvider restHighLevelClientProvider) {
		super.setRestHighLevelClientProvider(restHighLevelClientProvider);
	}

}
