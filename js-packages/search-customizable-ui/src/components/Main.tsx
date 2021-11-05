import React from "react";
import {
  ALL_SUGGESTION_CATEGORY_ID,
  DOCUMENT_TYPES_SUGGESTION_CATEGORY_ID,
  GenericResultItem,
  getPlugins,
  SearchQuery,
  SearchToken,
  SuggestionResult,
} from "@openk9/http-api";
import { useQuery } from "react-query";
import useDebounce from "../hooks/useDebounce";
import { TokenComponent } from "./Token";
import {
  getPluginResultRenderers,
  mapSuggestionToSearchToken,
  useDocumentTypeSuggestions,
  useSuggestionCategories,
} from "@openk9/search-ui-components";
import { useClickAway } from "../hooks/useClickAway";
import { useInView } from "react-intersection-observer";
import { Detail } from "./Detail";
import { OpenK9UIInteractions, OpenK9UITemplates } from "../api";
import { EmbedElement } from "./EmbedElement";
import { useTabTokens } from "../data-hooks/useTabTokens";
import { useInfiniteResults } from "../data-hooks/useInfiniteResults";
import { ResultPageMemo } from "./ResultPage";
import { SuggestionPageMemo } from "./SuggestionPage";
import { useInfiniteSuggestions } from "../data-hooks/useInfiniteSuggestions";

type MainProps = {
  children: (widgets: {
    search: React.ReactNode;
    suggestions: React.ReactNode;
    tabs: React.ReactNode;
    results: React.ReactNode;
    details: React.ReactNode;
  }) => React.ReactNode;
  templates: OpenK9UITemplates;
  interactions: OpenK9UIInteractions;
};

type State = {
  tokens: SearchToken[];
  tabIndex: number;
  focusedToken: number | null;
  text: string;
  searchQuery: SearchQuery | null;
  showSuggestions: boolean;
  selectedSuggestion: SuggestionResult | null;
  activeSuggestionCategory: number;
  detail: GenericResultItem<any> | null;
};

const initialState: State = {
  tokens: [],
  tabIndex: 0,
  text: "",
  searchQuery: null,
  showSuggestions: false,
  focusedToken: null,
  selectedSuggestion: null,
  activeSuggestionCategory: ALL_SUGGESTION_CATEGORY_ID,
  detail: null,
};

export function Main({ children, templates, interactions }: MainProps) {
  const [state, setState] = React.useState<State>(initialState);
  const { tokens, text, showSuggestions, searchQuery } = state;
  const tabTokens = useTabTokens(null);
  const selectedTabTokens = tabTokens[state.tabIndex].tokens;
  const inputRef = React.useRef<HTMLInputElement | null>(null);
  const suggestionsRef = React.useRef<HTMLInputElement | null>(null);
  const results = useInfiniteResults(searchQuery);
  const debouncedSearchQuery: SearchQuery = React.useMemo(
    () =>
      text
        ? [
            ...tokens,
            ...selectedTabTokens,
            { tokenType: "TEXT", values: [text] },
          ]
        : [...tokens, ...selectedTabTokens],
    [text, tokens, selectedTabTokens],
  );
  const suggestions = useInfiniteSuggestions(
    showSuggestions ? debouncedSearchQuery : null,
    state.activeSuggestionCategory,
  );
  const flattenedSuggestions = React.useMemo(
    () => suggestions.data?.pages.flatMap((page) => page.result) ?? [],
    [suggestions.data?.pages],
  );
  const selectedSuggestionIndex = React.useMemo(() => {
    if (!state.selectedSuggestion) return null;
    const indexOf = flattenedSuggestions.indexOf(state.selectedSuggestion);
    if (indexOf === -1) return null;
    return indexOf;
  }, [flattenedSuggestions, state.selectedSuggestion]);
  const { data: pluginInfos } = useQuery(["plugins"], () => {
    return getPlugins(null);
  });
  const documentTypeSuggestions = useDocumentTypeSuggestions(state.text);
  const renderers = React.useMemo(() => {
    return pluginInfos ? getPluginResultRenderers(pluginInfos) : null;
  }, [pluginInfos]);
  const updateSearch = React.useCallback(() => {
    setState((state): State => {
      // ATTENTION reread from state otherwise it will lag 1 interactions behind
      const { text, tokens } = state;
      const selectedTabTokens = tabTokens[state.tabIndex].tokens;
      const searchQuery: SearchQuery = text
        ? [
            ...tokens,
            ...selectedTabTokens,
            { tokenType: "TEXT", values: [text] },
          ]
        : [...tokens, ...selectedTabTokens];
      if (searchQuery.length > 0) {
        return {
          ...state,
          searchQuery,
        };
      } else {
        return { ...state, searchQuery: null };
      }
    });
  }, [tabTokens]);
  function addToken(token: SearchToken) {
    setState((state): State => {
      return { ...state, tokens: [...state.tokens, token] };
    });
  }
  function focusInput() {
    inputRef.current?.focus();
  }
  const addSuggestion = React.useCallback(
    (suggestion: SuggestionResult) => {
      const searchToken = mapSuggestionToSearchToken(suggestion);
      switch (suggestion.tokenType) {
        case "ENTITY": {
          addToken(searchToken);
          break;
        }
        case "TEXT": {
          addToken(searchToken);
          break;
        }
        case "DOCTYPE": {
          addToken(searchToken);
          break;
        }
        case "DATASOURCE": {
          addToken(searchToken);
          break;
        }
      }
      setState(
        (state): State => ({
          ...state,
          text: "",
          selectedSuggestion: null,
          showSuggestions: false,
          focusedToken: state.tokens.length - 1,
        }),
      );
      updateSearch();
      focusInput();
    },
    [updateSearch],
  );
  const selectSuggestion = React.useCallback((suggestion: SuggestionResult) => {
    setState(
      (state): State => ({
        ...state,
        selectedSuggestion: suggestion,
      }),
    );
  }, []);
  useClickAway(
    React.useMemo(() => [inputRef, suggestionsRef], []),
    React.useCallback(() => {
      setState((state): State => ({ ...state, showSuggestions: false }));
    }, []),
  );
  const loadMoreResultsRef = useLoadMore(results.fetchNextPage);
  const loadMoreSuggestionsRef = useLoadMore(suggestions.fetchNextPage);
  function suggestionUp() {
    setState((state): State => {
      if (selectedSuggestionIndex !== null && selectedSuggestionIndex > 0) {
        return {
          ...state,
          selectedSuggestion: flattenedSuggestions[selectedSuggestionIndex - 1],
        };
      }
      if (selectedSuggestionIndex === 0) {
        return { ...state, selectedSuggestion: null };
      } else return state;
    });
  }
  function suggestionDown() {
    setState((state): State => {
      if (selectedSuggestionIndex !== null) {
        return {
          ...state,
          selectedSuggestion: flattenedSuggestions[selectedSuggestionIndex + 1],
        };
      } else return { ...state, selectedSuggestion: flattenedSuggestions[0] };
    });
  }
  function setActiveTabIndex(index: number) {
    setState((state): State => ({ ...state, tabIndex: index }));
    updateSearch();
  }
  const setResultForDetail = React.useCallback(
    (result: GenericResultItem<unknown>) => {
      setState((state): State => ({ ...state, detail: result }));
    },
    [],
  );
  function setActiveSuggestionCategoryId(id: number) {
    setState(
      (state): State => ({
        ...state,
        activeSuggestionCategory: id,
      }),
    );
  }
  // update search debounced
  React.useEffect(() => {
    if (interactions.searchAsYouType) {
      const timeoutId = setTimeout(() => {
        updateSearch();
      }, 300);
      return () => {
        clearTimeout(timeoutId);
      };
    }
  }, [updateSearch, interactions.searchAsYouType, state.text]);
  const suggestionCategories = useSuggestionCategories(null);
  return children({
    search: (
      <div
        style={{
          display: "flex",
          height: "100%",
          alignItems: "center",
        }}
      >
        {tokens.map((token, index) => {
          if (templates.token) {
            return (
              <React.Fragment key={index}>
                <EmbedElement
                  element={templates.token({ token, entity: null })}
                />
              </React.Fragment>
            );
          }
          return (
            <TokenComponent
              key={index}
              token={token}
              onChange={(token) => {
                setState((state): State => {
                  const tokens = [...state.tokens];
                  tokens[index] = token;
                  return { ...state, tokens };
                });
              }}
              onRemove={() => {
                setState(
                  (state): State => ({
                    ...state,
                    tokens: state.tokens.filter((t, i) => i !== index),
                  }),
                );
              }}
              onSubmit={updateSearch}
              isFocused={state.focusedToken === index}
              onFocus={() =>
                setState(
                  (state): State => ({
                    ...state,
                    focusedToken: index,
                    showSuggestions: false,
                  }),
                )
              }
              interactions={interactions}
            />
          );
        })}
        <input
          ref={inputRef}
          value={text}
          onChange={(event) => {
            const text = event.currentTarget.value;
            setState(
              (state): State => ({
                ...state,
                text,
                selectedSuggestion: null,
              }),
            );
          }}
          onFocus={() => {
            setState(
              (state): State => ({
                ...state,
                focusedToken: null,
                showSuggestions: true,
              }),
            );
          }}
          onKeyDown={(event) => {
            switch (event.key) {
              case "Enter": {
                if (selectedSuggestionIndex !== null) {
                  if (state.selectedSuggestion)
                    addSuggestion(state.selectedSuggestion);
                } else {
                  setState(
                    (state): State => ({
                      ...state,
                      showSuggestions: false,
                    }),
                  );
                  updateSearch();
                }
                break;
              }
              case " ": {
                addToken({ tokenType: "TEXT", values: [text] });
                setState(
                  (state): State => ({
                    ...state,
                    text: "",
                    selectedSuggestion: null,
                    showSuggestions: false,
                  }),
                );
                updateSearch();
                break;
              }
              case "Escape": {
                setState(
                  (state): State => ({
                    ...state,
                    selectedSuggestion: null,
                    showSuggestions: false,
                  }),
                );
                break;
              }
              case "Backspace": {
                if (event.currentTarget.value === "") {
                  event.preventDefault();
                  setState(
                    (state): State => ({
                      ...state,
                      tokens: state.tokens.slice(0, -1),
                    }),
                  );
                }
                break;
              }
              case "ArrowUp": {
                event.preventDefault();
                suggestionUp();
                break;
              }
              case "ArrowDown": {
                event.preventDefault();
                suggestionDown();
                break;
              }
              default:
                setState(
                  (state): State => ({ ...state, showSuggestions: true }),
                );
            }
          }}
          style={{
            appearance: "none",
            outline: "none",
            border: "none",
            padding: "0px",
            font: "inherit",
            flexGrow: 1,
          }}
          placeholder={templates.inputPlaceholder ?? "Search"}
        />
      </div>
    ),
    suggestions: showSuggestions && (
      <div
        ref={suggestionsRef}
        style={{
          height: "200px",
          display: "flex",
        }}
      >
        <div
          style={{
            width: "200px",
            overflowX: "hidden",
            overflowY: "scroll",
          }}
        >
          {suggestionCategories.data?.map((suggestionCategory) => {
            const isActive =
              suggestionCategory.suggestionCategoryId ===
              state.activeSuggestionCategory;
            const customizedTokenKind = templates.suggestionCategory?.({
              label: suggestionCategory.name,
              active: isActive,
              select: () =>
                setActiveSuggestionCategoryId(
                  suggestionCategory.suggestionCategoryId,
                ),
            });
            if (customizedTokenKind) {
              return (
                <React.Fragment key={suggestionCategory.suggestionCategoryId}>
                  <EmbedElement element={customizedTokenKind} />
                </React.Fragment>
              );
            }
            return (
              <div
                key={suggestionCategory.suggestionCategoryId}
                onClick={() => {
                  setActiveSuggestionCategoryId(
                    suggestionCategory.suggestionCategoryId,
                  );
                }}
                style={{
                  padding: "8px 16px",
                  backgroundColor: isActive ? "lightgray" : "",
                  cursor: "pointer",
                }}
              >
                {suggestionCategory.name}
              </div>
            );
          })}
        </div>
        <div
          style={{
            flexGrow: 1,
            overflowX: "hidden",
            overflowY: "scroll",
          }}
        >
          {state.activeSuggestionCategory ===
            DOCUMENT_TYPES_SUGGESTION_CATEGORY_ID && (
            <SuggestionPageMemo
              suggestions={documentTypeSuggestions}
              templates={templates}
              onAdd={addSuggestion}
              onSelect={selectSuggestion}
              selected={state.selectedSuggestion}
            />
          )}
          {suggestions.data?.pages.map((suggestionPage, index) => {
            const selected =
              state.selectedSuggestion &&
              suggestionPage.result.includes(state.selectedSuggestion)
                ? state.selectedSuggestion
                : null;
            return (
              <SuggestionPageMemo
                key={index}
                suggestions={suggestionPage.result}
                templates={templates}
                onAdd={addSuggestion}
                onSelect={selectSuggestion}
                selected={selected}
              />
            );
          })}
          {suggestions.hasNextPage && !suggestions.isFetchingNextPage && (
            <div style={{ margin: "8px 16px" }} ref={loadMoreSuggestionsRef}>
              Loading more...
            </div>
          )}
        </div>
      </div>
    ),
    tabs: templates.tabs ? (
      <EmbedElement
        element={templates.tabs({
          tabs: tabTokens.map(({ label }) => label),
          activeIndex: state.tabIndex,
          setActiveIndex: setActiveTabIndex,
        })}
      />
    ) : (
      <div style={{ display: "flex" }}>
        {tabTokens.map((tabToken, index) => {
          return (
            <div
              key={index}
              style={{
                width: "100px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                padding: "8px 16px",
                backgroundColor: state.tabIndex === index ? "lightgray" : "",
                cursor: "pointer",
              }}
              onClick={() => setActiveTabIndex(index)}
            >
              {tabToken.label} {state.tabIndex === index}
            </div>
          );
        })}
      </div>
    ),
    results: (
      <div>
        <div style={{ margin: "8px 16px" }}>
          {results.data?.pages[0] && <>{results.data.pages[0].total} results</>}
        </div>
        {renderers &&
          results.data?.pages.map(({ result }, index) => {
            return (
              <ResultPageMemo
                key={index}
                results={result}
                templates={templates}
                renderers={renderers}
                onDetail={setResultForDetail}
              />
            );
          })}
        {results.hasNextPage && !results.isFetchingNextPage && (
          <div style={{ margin: "8px 16px" }} ref={loadMoreResultsRef}>
            Loading more...
          </div>
        )}
      </div>
    ),
    details:
      renderers &&
      state.detail &&
      (() => {
        const customizedDetail = templates.detail?.({
          result: state.detail,
        });
        if (customizedDetail)
          return <EmbedElement element={customizedDetail} />;
        return (
          <div style={{ padding: "8px 16px" }}>
            <Detail
              result={state.detail}
              sidebarRenderers={renderers.sidebarRenderers}
            />
          </div>
        );
      })(),
  }) as JSX.Element;
}

function useLoadMore(callback: () => void) {
  const { ref, inView } = useInView({ delay: 200, initialInView: false });
  React.useEffect(() => {
    if (inView) callback();
  }, [callback, inView]);
  return ref;
}
