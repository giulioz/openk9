import React from "react";
import { css } from "styled-components/macro";
import { WebResult } from "../renderers/openk9/web/WebResult";
import { EmailResult } from "../renderers/openk9/email/EmailResult";
import { UserResult } from "../renderers/openk9/user/UserResult";
import { GenericResultItem } from "@openk9/rest-api";
import { DocumentResult } from "../renderers/openk9/document/DocumentResult";
import { NotizieResult } from "../renderers/bdi/notizie/NotizieResult";
import { PubblicazioniResult } from "../renderers/bdi/pubblicazioni/PubblicazioniResult";
import { PdfResult } from "../renderers/openk9/pdf/PdfResult";
import { MostreResult } from "../renderers/cm/mostre/MostreResult";
import { EventiResult } from "../renderers/cm/eventi/EventiResult";
import { PetizioniResult } from "../renderers/cm/petizioni/PetizioniResult";
import { ProcessiResult } from "../renderers/cm/processi/ProcessiResult";
import { OpendataResult } from "../renderers/cm/opendata/OpendataResult";
import { CalendarResult } from "../renderers/openk9/calendar/CalendarResult";

type ResultProps<E> = {
  result: GenericResultItem<E>;
  onDetail(result: GenericResultItem<E> | null): void;
};
function Result<E>(props: ResultProps<E>) {
  const result = props.result as any;
  const { onDetail } = props;
  return (
    <div
      className="openk9-embeddable-search--result-container"
      onMouseEnter={() => onDetail(result)}
    >
      {(() => {
        if (result.source.documentTypes.includes("opendata")) {
          return <OpendataResult result={result} />;
        }
        if (result.source.documentTypes.includes("notizie")) {
          return <NotizieResult result={result} />;
        }
        if (result.source.documentTypes.includes("pubblicazioni")) {
          return <PubblicazioniResult result={result} />;
        }
        if (result.source.documentTypes.includes("mostre")) {
          return <MostreResult result={result} />;
        }
        if (result.source.documentTypes.includes("eventi")) {
          return <EventiResult result={result} />;
        }
        if (result.source.documentTypes.includes("petizioni")) {
          return <PetizioniResult result={result} />;
        }
        if (result.source.documentTypes.includes("processi")) {
          return <ProcessiResult result={result} />;
        }
        if (result.source.documentTypes.includes("pdf")) {
          return <PdfResult result={result} />;
        }
        if (result.source.documentTypes.includes("document")) {
          return <DocumentResult result={result} />;
        }
        if (result.source.documentTypes.includes("web")) {
          return <WebResult result={result} />;
        }
        if (result.source.documentTypes.includes("email")) {
          return <EmailResult result={result} />;
        }
        if (result.source.documentTypes.includes("user")) {
          return <UserResult result={result} />;
        }
        if (result.source.documentTypes.includes("calendar")) {
          return <CalendarResult result={result} />;
        }
        return (
          <pre
            css={css`
              height: 100px;
              overflow: hidden;
            `}
          >
            {JSON.stringify(result, null, 2)}
          </pre>
        );
      })()}
    </div>
  );
}
export const ResultMemo = React.memo(Result) as typeof Result;
