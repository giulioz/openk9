import React from "react";
import { Plugin } from "@openk9/rest-api";
import { WebResultItem } from "@openk9/search-frontend";

export const plugin: Plugin<WebResultItem> = {
  pluginId: "web-generic-datasource",
  displayName: "Web Generic DataSource",
  pluginServices: [
    {
      type: "DATASOURCE",
      displayName: "Web Generic DataSource",
      driverServiceName: "io.openk9.plugins.web.generic.driver.GenericWebPluginDriver",
      iconRenderer,
      initialSettings: `
        {
            "datasourceId": 1,
            "timestamp": 0,
            "startUrls": ["https://www.smc.it/"],
            "allowedDomains": ["smc.it"],
            "allowedPaths": [],
            "excludedPaths": ["/en"],
            "bodyTag": "body",
        	"maxLength": 50000
        }
      `,
    },
    {
      type: "ENRICH",
      displayName: "Web Async NER",
      serviceName:
        "io.openk9.plugins.web.generic.enrichprocessor.AsyncWebNerEnrichProcessor",
      iconRenderer,
      initialSettings: `{
                            "entityConfiguration": {
                                "person": 0.70,
                                "organization": 0.70,
                                "loc": 0.70,
                                "email": 0.90
                            },
                            "defaultConfidence": 0.80,
                            "relations": [
                                {
                                    "startType": "person",
                                    "endType": "organization",
                                    "name": "interacts_with"
                                },
                                {
                                    "startType": "person",
                                    "endType": "email",
                                    "name": "has_email"
                                }
                            ]
                        }`,
    },
  ],
};
