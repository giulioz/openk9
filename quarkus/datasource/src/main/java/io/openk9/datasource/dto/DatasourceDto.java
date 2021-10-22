/*
 * Copyright (c) 2020-present SMC Treviso s.r.l. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.openk9.datasource.dto;

import com.cronutils.model.CronType;
import com.cronutils.validation.Cron;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class DatasourceDto {
    private Boolean active;
    private String description;
    private String jsonConfig;
    private Instant lastIngestionDate;
    private String name;
    private Long tenantId;
    @Cron(type = CronType.QUARTZ)
    private String scheduling;
    private String driverServiceName;
}