import React from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faUser } from "@fortawesome/free-solid-svg-icons";
import { UserResultItem } from "./UserItem";
import { ResultContainer } from "../../../renderer-components/ResultContainer";
import { ResultTitle } from "../../../renderer-components/ResultTitle";
import { HighlightableText } from "../../../renderer-components/HighlightableText";
import { ResultAttribute } from "../../../renderer-components/ResultAttribute";

type UserResultProps = { result: UserResultItem };
export function UserResult({ result }: UserResultProps) {
  return (
    <ResultContainer icon={<FontAwesomeIcon icon={faUser} />}>
      <ResultTitle>
        <HighlightableText result={result} path="user.fullName" />
      </ResultTitle>
      {result.source.user.jobTitle && (
        <ResultAttribute label="Job">
          <HighlightableText result={result} path="user.jobTitle" />
        </ResultAttribute>
      )}
      {result.source.user.emailAddress && (
        <ResultAttribute label="Email">
          <HighlightableText result={result} path="user.emailAddress" />
        </ResultAttribute>
      )}
      {result.source.user.phoneNumber && (
        <ResultAttribute label="Phone">
          <HighlightableText result={result} path="user.phoneNumber" />
        </ResultAttribute>
      )}
    </ResultContainer>
  );
}
