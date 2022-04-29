import React from "react";
import ReactDOM from "react-dom";
import { css } from "styled-components/macro";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faLightbulb,
  faSearch,
  faSyncAlt,
} from "@fortawesome/free-solid-svg-icons";
import { useDebounce } from "../components/useDebounce";
import { useQueryAnalysis } from "../components/remote-data";
import { DetailMemo } from "../components/Detail";
import { ResultsMemo } from "../components/ResultList";
import { TokenSelect } from "../components/TokenSelect";
import { useClickAway } from "../components/useClickAway";
import { getAutoSelections, useSelections } from "../components/useSelections";
import { Tooltip } from "../components/Tooltip";
import { useLoginInfo } from "../components/useLogin";
import { LoginInfoComponentMemo } from "../components/LoginInfo";
import {
  AnalysisRequestEntry,
  AnalysisResponseEntry,
  AnalysisToken,
  GenericResultItem,
  SearchToken,
} from "@openk9/rest-api";
import { isEqual } from "lodash";
import "overlayscrollbars/css/OverlayScrollbars.css";
import { Configuration, ConfigurationUpdateFunction } from "./entry";
import { TabsMemo, useTabTokens } from "../components/Tabs";
import { FiltersMemo } from "../components/Filters";
import { SimpleErrorBoundary } from "../components/SimpleErrorBoundary";

type MainProps = {
  configuration: Configuration;
  onConfigurationChange: ConfigurationUpdateFunction;
  onQueryStateChange(queryState: QueryState): void;
};
export function Main({
  configuration,
  onConfigurationChange,
  onQueryStateChange,
}: MainProps) {
  const login = useLoginInfo();
  const autoSelect = configuration.searchAutoselect;
  const setAutoSelect = React.useCallback(
    (searchAutoselect: boolean) => {
      onConfigurationChange({ searchAutoselect });
    },
    [onConfigurationChange],
  );
  const replaceText = configuration.searchReplaceText;
  const setReplaceText = React.useCallback(
    (searchReplaceText: boolean) => {
      onConfigurationChange({ searchReplaceText });
    },
    [onConfigurationChange],
  );
  const [state, dispatch] = useSelections();
  const [openedDropdown, setOpenedDropdown] = React.useState<{
    textPosition: number;
    optionPosition: number;
  } | null>(null);
  const [detail, setDetail] = React.useState<GenericResultItem<unknown> | null>(
    null,
  );
  const tabs = useTabTokens();
  const [selectedTabIndex, setSelectedTabIndex] = React.useState(0);
  const tabTokens = React.useMemo(
    () => tabs[selectedTabIndex]?.tokens ?? [],
    [selectedTabIndex, tabs],
  );
  const debounced = useDebounce(state, 600);
  const queryAnalysis = useQueryAnalysis({
    searchText: debounced.text,
    tokens: debounced.selection.flatMap(({ text, start, end, token }) =>
      token ? [{ text, start, end, token }] : [],
    ),
  });
  const spans = React.useMemo(
    () => calculateSpans(state.text, queryAnalysis.data?.analysis),
    [queryAnalysis.data?.analysis, state.text],
  );
  const showSyntax =
    state.text === debounced.text &&
    queryAnalysis.data !== undefined &&
    !queryAnalysis.isPreviousData;
  const clickAwayRef = React.useRef<HTMLDivElement | null>(null);
  useClickAway([clickAwayRef], () => setOpenedDropdown(null));
  const filterTokens = configuration.filterTokens;
  const addFilterToken = React.useCallback(
    (searchToken: SearchToken) => {
      onConfigurationChange((configuration) => ({
        filterTokens: [...configuration.filterTokens, searchToken],
      }));
    },
    [onConfigurationChange],
  );
  const removeFilterToken = React.useCallback(
    (searchToken: SearchToken) => {
      onConfigurationChange((configuration) => ({
        filterTokens: configuration.filterTokens.filter(
          (token) => !isEqual(token, searchToken),
        ),
      }));
    },
    [onConfigurationChange],
  );
  const defaultTokens = configuration.defaultTokens;
  const derivedSearchQuery = React.useMemo(
    () =>
      deriveSearchQuery(
        spans,
        state.selection.flatMap(({ text, start, end, token }) =>
          token ? [{ text, start, end, token }] : [],
        ),
      ),
    [spans, state.selection],
  );
  const searchQueryMemo = React.useMemo(
    () => [
      ...defaultTokens,
      ...tabTokens,
      ...filterTokens,
      ...derivedSearchQuery,
    ],
    [defaultTokens, tabTokens, filterTokens, derivedSearchQuery],
  );
  const searchQuery = useDebounce(searchQueryMemo, 600);
  React.useEffect(() => {
    onQueryStateChange({
      defaultTokens,
      tabTokens,
      filterTokens,
      searchTokens: derivedSearchQuery,
    });
  }, [
    onQueryStateChange,
    derivedSearchQuery,
    filterTokens,
    searchQuery,
    tabTokens,
    defaultTokens,
  ]);
  React.useEffect(() => {
    if (
      autoSelect &&
      queryAnalysis.data &&
      queryAnalysis.data.searchText === state.text
    ) {
      const autoSelections = getAutoSelections(
        state.selection,
        queryAnalysis.data.analysis,
      );
      for (const selection of autoSelections) {
        dispatch({
          type: "set-selection",
          replaceText: false,
          selection,
        });
      }
    }
  }, [
    autoSelect,
    dispatch,
    queryAnalysis.data,
    replaceText,
    state.selection,
    state.text,
  ]);
  const inputRef = React.useRef<HTMLInputElement | null>(null);
  const [adjustedSelection, setAdjustedSelection] = React.useState<{
    selectionStart: number;
    selectionEnd: number;
  }>({ selectionStart: 0, selectionEnd: 0 });
  React.useLayoutEffect(() => {
    if (inputRef.current) {
      inputRef.current.selectionStart = adjustedSelection.selectionStart;
      inputRef.current.selectionEnd = adjustedSelection.selectionEnd;
    }
  }, [adjustedSelection]);
  React.useEffect(() => {
    setDetail(null);
  }, [searchQuery]);
  return (
    <React.Fragment>
      {renderPortal(
        <div
          ref={clickAwayRef}
          className="openk9-embeddable-search--input-container"
          css={css`
            display: flex;
            align-items: center;
          `}
        >
          <FontAwesomeIcon
            icon={faSearch}
            style={{
              paddingLeft: "16px",
              color: "--openk9-embeddable-search--secondary-text-color",
            }}
          />
          <div
            css={css`
              flex-grow: 1;
              position: relative;
              display: flex;
            `}
          >
            <div
              css={css`
                top: 0px;
                left: 0px;
                padding: var(--openk9-embeddable-search--input-padding);
                display: flex;
                position: absolute;
              `}
            >
              {showSyntax &&
                spans.map((span, index) => {
                  const isOpen =
                    openedDropdown !== null &&
                    openedDropdown.textPosition > span.start &&
                    openedDropdown.textPosition <= span.end;
                  const optionIndex = openedDropdown?.optionPosition ?? null;
                  const selection = state.selection.find(
                    (selection) =>
                      selection.start === span.start &&
                      selection.end === span.end,
                  );
                  const selected = selection?.token ?? null;
                  const onSelect = (token: AnalysisToken | null): void => {
                    dispatch({
                      type: "set-selection",
                      replaceText,
                      selection: {
                        text: span.text,
                        start: span.start,
                        end: span.end,
                        token,
                        isAuto: false,
                      },
                    });
                    if (
                      inputRef.current?.selectionStart &&
                      inputRef.current?.selectionEnd
                    ) {
                      setAdjustedSelection({
                        selectionStart: inputRef.current.selectionStart,
                        selectionEnd: inputRef.current.selectionEnd,
                      });
                    }
                    setOpenedDropdown(null);
                  };
                  const isAutoSelected = selection?.isAuto ?? false;
                  const onOptionIndexChange = (optionIndex: number) => {
                    setOpenedDropdown((openedDropdown) =>
                      openedDropdown
                        ? { ...openedDropdown, optionPosition: optionIndex }
                        : openedDropdown,
                    );
                  };
                  return (
                    <TokenSelect
                      key={index}
                      span={span}
                      isOpen={isOpen}
                      onOptionIndexChange={onOptionIndexChange}
                      optionIndex={optionIndex}
                      selected={selected}
                      onSelect={onSelect}
                      isAutoSlected={isAutoSelected}
                    />
                  );
                })}
            </div>
            <input
              ref={inputRef}
              value={state.text}
              onChange={(event) => {
                dispatch({
                  type: "set-text",
                  text: event.currentTarget.value,
                });
                setDetail(null);
                setOpenedDropdown(null);
              }}
              css={css`
                position: relative;
                flex-grow: 1;
                border: none;
                outline: none;
                padding: var(--openk9-embeddable-search--input-padding);
                color: ${showSyntax ? "transparent" : "inherit"};
                caret-color: black;
                font-size: inherit;
                font-family: inherit;
                background-color: inherit;
              `}
              spellCheck="false"
              onSelect={(event) => {
                if (
                  (event.currentTarget.selectionDirection === "forward" ||
                    event.currentTarget.selectionDirection === "none") &&
                  event.currentTarget.selectionStart ===
                    event.currentTarget.selectionEnd
                ) {
                  setOpenedDropdown({
                    textPosition: event.currentTarget.selectionStart as number,
                    optionPosition: openedDropdown?.optionPosition ?? 0,
                  });
                }
              }}
              onKeyDown={(event) => {
                const span =
                  openedDropdown &&
                  spans.find(
                    (span) =>
                      openedDropdown.textPosition > span.start &&
                      openedDropdown.textPosition <= span.end,
                  );
                const option =
                  openedDropdown &&
                  span?.tokens[openedDropdown.optionPosition - 1];
                if (event.key === "ArrowDown") {
                  event.preventDefault();
                  if (openedDropdown && span) {
                    setOpenedDropdown({
                      textPosition: openedDropdown.textPosition,
                      optionPosition:
                        openedDropdown.optionPosition < span.tokens.length
                          ? openedDropdown.optionPosition + 1
                          : 0,
                    });
                  }
                } else if (event.key === "ArrowUp") {
                  event.preventDefault();
                  if (openedDropdown && openedDropdown.optionPosition > 0) {
                    setOpenedDropdown({
                      textPosition: openedDropdown.textPosition,
                      optionPosition: openedDropdown.optionPosition - 1,
                    });
                  }
                } else if (event.key === "Enter") {
                  event.preventDefault();
                  if (span) {
                    dispatch({
                      type: "set-selection",
                      replaceText,
                      selection: {
                        text: span.text,
                        start: span.start,
                        end: span.end,
                        token: option ?? null,
                        isAuto: false,
                      },
                    });
                    if (
                      event.currentTarget.selectionStart &&
                      event.currentTarget.selectionEnd
                    ) {
                      setAdjustedSelection({
                        selectionStart: event.currentTarget.selectionStart,
                        selectionEnd: event.currentTarget.selectionEnd,
                      });
                    }
                    setOpenedDropdown(null);
                  }
                } else if (event.key === "Escape") {
                  setOpenedDropdown(null);
                }
              }}
            ></input>
          </div>
          <Tooltip description="Sostituzione del testo quando si seleziona un suggerimento">
            <FontAwesomeIcon
              icon={faSyncAlt}
              style={{
                paddingRight: "16px",
                color: replaceText
                  ? "var(--openk9-embeddable-search--primary-color)"
                  : "var(--openk9-embeddable-search--secondary-text-color)",
                cursor: "pointer",
              }}
              onClick={() => {
                setReplaceText(!replaceText);
              }}
            />
          </Tooltip>
          <Tooltip description="Seleziona automaticamente il suggerimento più pertinente">
            <FontAwesomeIcon
              icon={faLightbulb}
              style={{
                paddingRight: "16px",
                color: autoSelect
                  ? "var(--openk9-embeddable-search--primary-color)"
                  : "var(--openk9-embeddable-search--secondary-text-color)",
                cursor: "pointer",
              }}
              onClick={() => {
                setAutoSelect(!autoSelect);
              }}
            />
          </Tooltip>
        </div>,
        configuration.search,
      )}
      {renderPortal(
        <TabsMemo
          tabs={tabs}
          selectedTabIndex={selectedTabIndex}
          onSelectedTabIndexChange={setSelectedTabIndex}
          onConfigurationChange={onConfigurationChange}
        />,
        configuration.tabs,
      )}
      {renderPortal(
        <FiltersMemo
          searchQuery={searchQuery}
          onAddFilterToken={addFilterToken}
          onRemoveFilterToken={removeFilterToken}
        />,
        configuration.filters,
      )}
      {renderPortal(
        <ResultsMemo
          displayMode={configuration.resultsDisplayMode}
          searchQuery={searchQuery}
          onDetail={setDetail}
        />,
        configuration.results,
      )}
      {renderPortal(<DetailMemo result={detail} />, configuration.details)}
      {renderPortal(
        <LoginInfoComponentMemo
          loginState={login.state}
          onLogin={login.login}
          onLogout={login.logout}
        />,
        configuration.login,
      )}
    </React.Fragment>
  );
}

function renderPortal(
  node: React.ReactNode,
  container: Element | string | null,
): React.ReactNode {
  const element =
    typeof container === "string"
      ? document.querySelector(container)
      : container;
  return (
    <SimpleErrorBoundary>
      <React.Suspense>
        {element ? (ReactDOM.createPortal(node, element) as any) : null}
      </React.Suspense>
    </SimpleErrorBoundary>
  );
}

export type QueryState = {
  defaultTokens: Array<SearchToken>;
  tabTokens: Array<SearchToken>;
  filterTokens: Array<SearchToken>;
  searchTokens: Array<SearchToken>;
};

function deriveSearchQuery(
  spans: AnalysisResponseEntry[],
  selection: AnalysisRequestEntry[],
) {
  return spans
    .map((span) => ({ ...span, text: span.text.trim() }))
    .filter((span) => span.text)
    .map((span): SearchToken => {
      const token =
        selection.find(
          (selection) =>
            selection.start === span.start && selection.end === span.end,
        )?.token ?? null;
      if (token) {
        return analysisTokenToSearchToken(token);
      }
      return { tokenType: "TEXT", values: [span.text], filter: false };
    });
}

function analysisTokenToSearchToken(token: AnalysisToken): SearchToken {
  switch (token.tokenType) {
    case "DATASOURCE":
      return {
        tokenType: "DATASOURCE",
        values: [token.value],
        filter: false,
      };
    case "DOCTYPE":
      return {
        tokenType: "DOCTYPE",
        keywordKey: "type",
        values: [token.value],
        filter: true,
      };
    case "ENTITY":
      return {
        tokenType: "ENTITY",
        keywordKey: token.keywordKey,
        entityType: token.entityType,
        entityName: token.entityName,
        values: [token.value],
        filter: false,
      };
    case "TEXT":
      return {
        tokenType: "TEXT",
        keywordKey: token.keywordKey,
        values: [token.value],
        filter: false,
      };
  }
}

function calculateSpans(
  text: string,
  analysis: AnalysisResponseEntry[] | undefined,
): AnalysisResponseEntry[] {
  const spans: Array<AnalysisResponseEntry> = [
    { text: "", start: 0, end: 0, tokens: [] },
  ];
  for (let i = 0; i < text.length; ) {
    const found = analysis?.find(
      ({ start, text }) => i === start && text !== "",
    );
    if (found) {
      spans.push(found);
      i += found.text.length;
      spans.push({ text: "", start: i, end: i, tokens: [] });
    } else {
      const last = spans[spans.length - 1];
      last.text += text[i];
      last.end += 1;
      i += 1;
    }
  }
  return spans.filter((span) => span.text);
}
