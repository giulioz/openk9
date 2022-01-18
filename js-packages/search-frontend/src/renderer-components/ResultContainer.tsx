import React from "react";
import { css } from "styled-components/macro";

type ResultContainerProps = {
  icon: React.ReactNode;
  children: React.ReactNode;
};
export function ResultContainer({ icon, children }: ResultContainerProps) {
  return (
    <div
      css={css`
        display: flex;
        padding: 8px 16px;
      `}
    >
      <div
        css={css`
          width: 30px;
          margin-right: 8px;
        `}
      >
        <div
          css={css`
            width: 30px;
            height: 30px;
            display: flex;
            align-items: center;
            justify-content: center;
          `}
        >
          {icon}
        </div>
      </div>
      <div
        css={css`
          margin-left: 8px;
          overflow: hidden;
        `}
      >
        {children}
      </div>
    </div>
  );
}
