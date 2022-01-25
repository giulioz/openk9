package io.openk9.datasource.web;

import io.openk9.datasource.dto.SuggestionCategoryFieldDto;
import io.openk9.datasource.mapper.SuggestionCategoryFieldIgnoreNullMapper;
import io.openk9.datasource.mapper.SuggestionCategoryFieldNullAwareMapper;
import io.openk9.datasource.model.SuggestionCategoryField;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/v2/suggestion-category-field")
public class SuggestionCategoryFieldResource {

	@GET
	@Path("/{id}")
	@Produces()
	public Uni<SuggestionCategoryField> findById(@PathParam("id") long id){
		return SuggestionCategoryField.findById(id);
	}

	@POST
	@Path("/filter")
	@Produces()
	public Uni<List<SuggestionCategoryField>> filter(
		SuggestionCategoryFieldDto dto){

		Map<String, Object> map = JsonObject.mapFrom(dto).getMap();

		Tuple2<String, Map<String, Object>> query = ResourceUtil.getFilterQuery(map);

		return SuggestionCategoryField.list(query.getItem1(), query.getItem2());
	}

	@GET
	@Produces()
	public Uni<List<SuggestionCategoryField>> findAll(
		@QueryParam("sort") List<String> sortQuery,
		@QueryParam("page") @DefaultValue("0") int pageIndex,
		@QueryParam("size") @DefaultValue("20") int pageSize
	){
		Page page = Page.of(pageIndex, pageSize);
		Sort sort = Sort.by(sortQuery.toArray(String[]::new));

		return SuggestionCategoryField.findAll(sort).page(page).list();
	}

	@POST
	@Consumes("application/json")
	public Uni<SuggestionCategoryField> create(@Valid SuggestionCategoryFieldDto dto) {

		SuggestionCategoryField datasource = _suggestionCategoryFieldNullAwareMapper.toSuggestionCategoryField(dto);

		return Panache.withTransaction(datasource::persistAndFlush);

	}

	@POST
	@Path("/{id}")
	@Consumes("application/json")
	public Uni<SuggestionCategoryField> update(
		@PathParam("id") long id, @Valid SuggestionCategoryFieldDto dto) {

		return SuggestionCategoryField
			.findById(id)
			.onItem()
			.ifNull()
			.failWith(() -> new WebApplicationException(
				"SuggestionCategoryField with id of " + id + " does not exist.", 404))
			.flatMap(datasource -> {
				SuggestionCategoryField newSuggestionCategoryField =
					_suggestionCategoryFieldNullAwareMapper.update((SuggestionCategoryField)datasource, dto);
				return Panache.withTransaction(newSuggestionCategoryField::persist);
			});

	}

	@PATCH
	@Path("/{id}")
	@Consumes("application/json")
	public Uni<SuggestionCategoryField> patch(
		@PathParam("id") long id, @Valid SuggestionCategoryFieldDto dto) {

		return SuggestionCategoryField
			.findById(id)
			.onItem()
			.ifNull()
			.failWith(() -> new WebApplicationException(
				"SuggestionCategoryField with id of " + id + " does not exist.", 404))
			.flatMap(datasource -> {
				SuggestionCategoryField newSuggestionCategoryField =
					_suggestionCategoryFieldIgnoreNullMapper.update((SuggestionCategoryField)datasource, dto);
				return Panache.withTransaction(newSuggestionCategoryField::persist);
			});

	}

	@DELETE
	@Path("/{id}")
	public Uni<Response> deleteById(@PathParam("id") long id){

		return Panache.withTransaction(() ->
			SuggestionCategoryField
				.findById(id)
				.onItem()
				.ifNull()
				.failWith(() -> new WebApplicationException(
					"SuggestionCategory with id of " + id + " does not exist.", 404))
				.flatMap(PanacheEntityBase::delete)
				.map(unused -> Response.status(204).build())
		);

	}

	@Inject
	SuggestionCategoryFieldNullAwareMapper
		_suggestionCategoryFieldNullAwareMapper;

	@Inject
	SuggestionCategoryFieldIgnoreNullMapper
		_suggestionCategoryFieldIgnoreNullMapper;
}